package com.DGSD.Teexter.Activity;

import java.net.URLDecoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SearchView;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.DGSD.Teexter.BroadcastType;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.TxtMessage;
import com.DGSD.Teexter.Fragment.BaseListFragment;
import com.DGSD.Teexter.Fragment.BaseListFragment.OnMessageItemLongClickListener;
import com.DGSD.Teexter.Fragment.DraftsFragment;
import com.DGSD.Teexter.Fragment.FavouritesFragment;
import com.DGSD.Teexter.Fragment.InboxFragment;
import com.DGSD.Teexter.Fragment.SentFragment;
import com.DGSD.Teexter.Service.DatabaseService;
import com.DGSD.Teexter.Utils.CopyUtils;
import com.DGSD.Teexter.Utils.IntentUtils;
import com.DGSD.Teexter.Utils.ToastUtils;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

//TODO: Add 'Forward' option
public class MainActivity extends BaseReceiverActivity implements DialogInterface.OnClickListener,
		OnMessageItemLongClickListener, SearchView.OnQueryTextListener {
	private static final String CURRENT_TITLE = "_current_title";
	
	private static final int CONFIRM_DELETE_DIALOG = 0;

	private static final int NUMBER_OF_PAGES = 4;

	public static final int FAVOURITES_PAGE = 0;
	public static final int INBOX_PAGE = 1;
	public static final int SENT_PAGE = 2;
	public static final int DRAFTS_PAGE = 3;

	private ViewPager mPager;
	private TitlePageIndicator mIndicator;
	private PageAdapter mAdapter;
	private ActionMode mLongPressActionMode;

	private MenuItem mSearchMenuItem;
	private SearchView mSearchView;

	private int mLastMessageLongClicked = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);

		if(savedInstanceState != null) {
			getActionBar().setTitle(savedInstanceState.getCharSequence(CURRENT_TITLE, getString(R.string.app_name)));
		}
		
		initView();
	}

	@Override
	public void onStart() {
		super.onStart();
		setProgressBarIndeterminateVisibility(false);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mLongPressActionMode != null) {
			mLongPressActionMode.finish();
		}

		// Remove any showing notifications
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancelAll();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString(CURRENT_TITLE, getActionBar().getTitle().toString());
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		mSearchMenuItem = menu.findItem(R.id.search);
		mSearchView = (SearchView) mSearchMenuItem.getActionView();
		mSearchView.setIconifiedByDefault(true);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setSubmitButtonEnabled(false);
		mSearchView.setQueryHint("Search Messages");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.compose:
				final Intent intent = new Intent(this, ComposeActivity.class);
				startActivity(intent);
				return true;
			case R.id.search:

				return true;
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

			if (query != null) {
				query = URLDecoder.decode(query);
			}

			// Our query will return the display name of the message sender
			String id = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);

			System.err.println("DO SEARCH FOR: " + query + " OR " + id);
		}
	}

	private void initView() {
		mPager = (ViewPager) findViewById(R.id.pager);
		mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);

		mAdapter = new PageAdapter(getSupportFragmentManager());

		mPager.setOffscreenPageLimit(2);
		mPager.setAdapter(mAdapter);

		mIndicator.setViewPager(mPager, INBOX_PAGE);
	}

	@Override
	public Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {
			case CONFIRM_DELETE_DIALOG:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.are_you_sure);
				builder.setMessage(R.string.message_delete_are_you_sure);
				builder.setPositiveButton(R.string.yes, this);
				builder.setNegativeButton(R.string.cancel, this);

				dialog = builder.create();
				break;
		}

		return dialog;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(DialogInterface dialog, int id) {
		switch (id) {
			case DialogInterface.BUTTON_POSITIVE:
				DatabaseService.requestDeleteInboxMessage(this, mLastMessageLongClicked);
				showProgressBar();
				mLongPressActionMode.finish();
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				dismissDialog(CONFIRM_DELETE_DIALOG);
				break;
		}
	}

	@Override
	public void onMessageItemLongClick(int fromFragment, int id, TxtMessage msg) {
		mLongPressActionMode = startActionMode(new LongPressActionMode(fromFragment, id, msg));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		int type = intent.getIntExtra(Extra.DATA_TYPE, -1);
		String action = intent.getAction();

		switch (type) {
			case DatabaseService.RequestType.TOGGLE_FAVOURITE:
				if (action.equals(BroadcastType.ERROR)) {
					ToastUtils.show(this, "Error whilst updating. Please try again", Toast.LENGTH_SHORT);
				}
				break;
			case DatabaseService.RequestType.DELETE_INBOX:
				if (action.equals(BroadcastType.ERROR)) {
					ToastUtils.show(this, "Error whilst deleting. Please try again", Toast.LENGTH_SHORT);
				} else if (action.equals(BroadcastType.SUCCESS)) {
					ToastUtils.show(this, "Message deleted", Toast.LENGTH_SHORT);
				}
				break;
		}

		hideProgressBar();
	}

	public boolean onQueryTextChange(String newText) {
		BaseListFragment frag = (BaseListFragment) mAdapter.getFragmentAt(mPager.getCurrentItem());

		if (frag != null) {
			if (TextUtils.isEmpty(newText)) {
				getActionBar().setTitle(R.string.app_name);
				frag.clearTextFilter();
			} else {
				getActionBar().setTitle("Search: '" + newText.toString() + "'");
				frag.setFilterText(newText.toString());
			}
		}
		return true;
	}

	public boolean onQueryTextSubmit(String query) {
		if(mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
			mSearchMenuItem.collapseActionView();
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		BaseListFragment frag = (BaseListFragment) mAdapter.getFragmentAt(mPager.getCurrentItem());
		if(frag.hasFilterApplied()) {
			onQueryTextChange(null);
		} else {
			super.onBackPressed();
		}
	}
	
	private final class LongPressActionMode implements ActionMode.Callback {
		private TxtMessage mMsg;
		private int mFromType;

		public LongPressActionMode(int fromFragmentType, int msgId, TxtMessage msg) {
			mMsg = msg;
			mLastMessageLongClicked = msgId;
			mFromType = fromFragmentType;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			getMenuInflater().inflate(R.menu.message_list_contextual_menu, menu);

			MenuItem item = menu.findItem(R.id.favourite);
			if (mMsg.isFavourite()) {
				item.setTitle(R.string.unfavourite);
			} else {
				item.setTitle(R.string.favourite);
			}

			MenuItem menuItem = menu.findItem(R.id.share);
			ShareActionProvider mShareActionProvider = (ShareActionProvider) menuItem.getActionProvider();
			mShareActionProvider.setShareIntent(IntentUtils.newShareTextIntent(mMsg.getSender(), mMsg.getMessage(),
					"Share Message"));

			mode.setTitle(mMsg.getMessage());

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			int id = item.getItemId();

			switch (id) {
				case R.id.delete: {
					showDialog(CONFIRM_DELETE_DIALOG);
					return true;
				}
				case R.id.favourite: {
					showProgressBar();
					DatabaseService.requestToggleFavourite(MainActivity.this, mLastMessageLongClicked,
							!mMsg.isFavourite());
					mLongPressActionMode.finish();
					return true;
				}
				case R.id.reply: {
					Intent intent = new Intent(MainActivity.this, ComposeActivity.class);
					intent.putExtra(Extra.ID, mLastMessageLongClicked);
					intent.putExtra(Extra.IS_REPLY, true);
					startActivity(intent);
					return true;
				}
				case R.id.forward: {
					Intent intent = new Intent(MainActivity.this, ComposeActivity.class);
					intent.putExtra(Extra.ID, mLastMessageLongClicked);
					intent.putExtra(Extra.IS_FORWARD, true);
					startActivity(intent);
					return true;
				}
				case R.id.copy: {
					String msg = mMsg.getMessage();
					String bestName = (mMsg.getContact() == null || mMsg.getContact().name == null) ? mMsg.getSender()
							: mMsg.getContact().name;

					CopyUtils.copyText(MainActivity.this, "Text Message from " + bestName, msg);
					ToastUtils.show(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT);
					mLongPressActionMode.finish();
					return true;
				}
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {

		}
	}

	/**
	 * Largely based on the support library FragmentPagerAdapter, but we can pull specific
	 * fragments out given an index.  
	 * @author daniel
	 */
	private class PageAdapter extends PagerAdapter implements TitleProvider {
		private final FragmentManager mFragmentManager;
		private FragmentTransaction mCurTransaction = null;

		public PageAdapter(FragmentManager fm) {
			mFragmentManager = fm;
		}

		@Override
		public int getCount() {
			return NUMBER_OF_PAGES;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			if (mCurTransaction == null) {
				mCurTransaction = mFragmentManager.beginTransaction();
			}

			// Do we already have this fragment?
			String name = makeFragmentTag(position);
			Fragment fragment = mFragmentManager.findFragmentByTag(name);
			if (fragment != null) {
				mCurTransaction.attach(fragment);
			} else {
				fragment = getItem(position);
				mCurTransaction.add(container.getId(), fragment, makeFragmentTag(position));
			}

			if (fragment instanceof BaseListFragment) {
				((BaseListFragment) fragment).setOnMessageItemLongClickListener(MainActivity.this);
			}

			return fragment;
		}

		public Fragment getFragmentAt(int pos) {
			return mFragmentManager.findFragmentByTag(makeFragmentTag(pos));
		}

		public Fragment getItem(int pos) {
			switch (pos) {
				case INBOX_PAGE:
					return InboxFragment.newInstance();
				case SENT_PAGE:
					return SentFragment.newInstance();
				case DRAFTS_PAGE:
					return DraftsFragment.newInstance();
				case FAVOURITES_PAGE:
					return FavouritesFragment.newInstance();
			}

			return null;
		}

		@Override
		public String getTitle(int pos) {
			switch (pos) {
				case INBOX_PAGE:
					return "Inbox";
				case SENT_PAGE:
					return "Sent";
				case DRAFTS_PAGE:
					return "Drafts";
				case FAVOURITES_PAGE:
					return "Favourites";
			}

			return null;
		}

		public void startUpdate(View container) {
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			if (mCurTransaction == null) {
				mCurTransaction = mFragmentManager.beginTransaction();
			}

			mCurTransaction.detach((Fragment) object);
		}

		@Override
		public void finishUpdate(View container) {
			if (mCurTransaction != null) {
				mCurTransaction.commit();
				mCurTransaction = null;
				mFragmentManager.executePendingTransactions();
			}
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return ((Fragment) object).getView() == view;
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}
	}

	private static String makeFragmentTag(int index) {
		return "android:switcher:" + index;
	}
}

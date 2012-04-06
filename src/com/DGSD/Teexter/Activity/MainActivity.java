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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;
import android.widget.Toast;

import com.DGSD.Teexter.BroadcastType;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.TxtMessage;
import com.DGSD.Teexter.Fragment.BaseListFragment;
import com.DGSD.Teexter.Fragment.BaseListFragment.OnMessageItemLongClickListener;
import com.DGSD.Teexter.Fragment.FavouritesFragment;
import com.DGSD.Teexter.Fragment.InboxFragment;
import com.DGSD.Teexter.Fragment.SentFragment;
import com.DGSD.Teexter.Service.DatabaseService;
import com.DGSD.Teexter.Utils.IntentUtils;
import com.DGSD.Teexter.Utils.ToastUtils;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MainActivity extends BaseReceiverActivity implements DialogInterface.OnClickListener,
		OnMessageItemLongClickListener {
	private static final int CONFIRM_DELETE_DIALOG = 0;

	private static final int NUMBER_OF_PAGES = 3;

	public static final int FAVOURITES_PAGE = 0;
	public static final int INBOX_PAGE = 1;
	public static final int SENT_PAGE = 2;

	private ViewPager mPager;
	private TitlePageIndicator mIndicator;
	private PageAdapter mAdapter;
	private ActionMode mLongPressActionMode;

	private int mLastMessageLongClicked = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);

		initView();
	}

	@Override
	public void onStart() {
		super.onStart();
		setSupportProgressBarIndeterminateVisibility(false);
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
	public void onPause() {
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.compose:

				return true;
			case R.id.search:
				onSearchRequested();
				return true;
		}

		return false;
	}

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

	@Override
	public void onClick(DialogInterface dialog, int id) {
		switch (id) {
			case DialogInterface.BUTTON_POSITIVE:
				// TODO: Delete calendar with clientId 'mLastCalendarId'
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

		if (type == DatabaseService.RequestType.TOGGLE_FAVOURITE) {
			if (action.equals(BroadcastType.ERROR)) {
				ToastUtils.show(this, "Error whilst updating. Please try again", Toast.LENGTH_SHORT);
			} else if(action.equals(BroadcastType.SUCCESS)) {
				if(mAdapter != null) {
					mAdapter.notifyDataSetChanged();
				}
			}
		}

		hideProgressBar();
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
			getSupportMenuInflater().inflate(R.menu.message_list_contextual_menu, menu);

			mode.setTitle(mMsg.getMessage());

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			int id = item.getItemId();

			switch (id) {
				case R.id.delete:
					showDialog(CONFIRM_DELETE_DIALOG);
					return true;
				case R.id.favourite:
					showProgressBar();
					DatabaseService.requestToggleFavourite(MainActivity.this, mLastMessageLongClicked,
							mFromType != FAVOURITES_PAGE);
					mLongPressActionMode.finish();
					return true;
				case R.id.reply:

					return true;
				case R.id.share:
					startActivity(IntentUtils.newShareTextIntent(mMsg.getSender(), mMsg.getMessage(), "Share Message"));
					return true;
			}
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			
		}
	}

	private class PageAdapter extends FragmentPagerAdapter implements TitleProvider {
		public PageAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return NUMBER_OF_PAGES;
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			Fragment f = (Fragment) super.instantiateItem(container, position);

			if (f instanceof BaseListFragment) {
				((BaseListFragment) f).setOnMessageItemLongClickListener(MainActivity.this);
			}

			return f;
		}

		@Override
		public Fragment getItem(int pos) {
			switch (pos) {
				case INBOX_PAGE:
					return InboxFragment.newInstance();
				case SENT_PAGE:
					return SentFragment.newInstance();
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
				case FAVOURITES_PAGE:
					return "Favourites";
			}

			return null;
		}
	}
}

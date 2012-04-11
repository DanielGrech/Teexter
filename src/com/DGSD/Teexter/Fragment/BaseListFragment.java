package com.DGSD.Teexter.Fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.TxtMessage;
import com.DGSD.Teexter.Activity.ComposeActivity;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.UI.QuickContactBadge;
import com.DGSD.Teexter.Utils.AnimUtils;
import com.DGSD.Teexter.Utils.ContactPhotoManager;
import com.DGSD.Teexter.Utils.ContactUtils.Contact;
import com.DGSD.Teexter.Utils.UriUtils;

public abstract class BaseListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, ViewBinder,
		OnItemLongClickListener, OnItemClickListener {
	private static final String TAG = BaseListFragment.class.getSimpleName();

	protected ListView mList;
	protected SimpleCursorAdapter mAdapter;
	protected OnMessageItemLongClickListener mOnMessageItemLongClickListener;
	protected Drawable mDefaultContactBitmap;

	protected abstract FilterableMessageAdapter onCreateAdapter();

	protected abstract int getType();

	protected ContactPhotoManager mPhotoLoader;

	protected CursorCols cursorCols = new CursorCols();
	
	protected String mCurrentFilterText;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_list, container, false);

		mAdapter = onCreateAdapter();

		mList = (ListView) v.findViewById(android.R.id.list);
		mList.setAdapter(mAdapter);
		mList.setLayoutAnimation(AnimUtils.getListViewSlideInFromLeftAnimator());

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter.setViewBinder(this);
		mList.setOnItemLongClickListener(this);
		mList.setOnItemClickListener(this);

		mPhotoLoader = ContactPhotoManager.getInstance(getActivity());

		getLoaderManager().initLoader(0, null, this);
		mDefaultContactBitmap = getResources().getDrawable(R.drawable.ic_contact_picture);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor);
		mPhotoLoader.refreshCache();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}

	@Override
	public boolean setViewValue(final View view, Cursor cursor, int col) {
		if (cursorCols.id < 0) {
			cursorCols.id = cursor.getColumnIndex(DbField.ID.getName());
			cursorCols.time = cursor.getColumnIndex(DbField.TIME.getName());
			cursorCols.contact_lookup = cursor.getColumnIndex(DbField.CONTACT_LOOKUP_ID.getName());
			cursorCols.photo_uri = cursor.getColumnIndex(DbField.PHOTO_URI.getName());
			cursorCols.display_name = cursor.getColumnIndex(DbField.DISPLAY_NAME.getName());
			cursorCols.phone_number = cursor.getColumnIndex(DbField.NUMBER.getName());
			cursorCols.message = cursor.getColumnIndex(DbField.MESSAGE.getName());
			cursorCols.favourite = cursor.getColumnIndex(DbField.FAVOURITE.getName());
		}

		ViewHolder holder = (ViewHolder) ((View) view.getParent()).getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.nameTv = (TextView) ((View) view.getParent()).findViewById(R.id.name);
			holder.msgTv = (TextView) ((View) view.getParent()).findViewById(R.id.message);
			holder.timeTv = (TextView) ((View) view.getParent()).findViewById(R.id.date);
			holder.qcb = (QuickContactBadge) ((View) view.getParent()).findViewById(R.id.quick_contact_badge);
			// holder.favouriteIcon = (ImageView)
			// parent.findViewById(R.id.favourite_icon);
		}

		holder.id = cursor.getInt(cursorCols.id);
		holder.nameTv.setText(cursor.getString(cursorCols.display_name));
		holder.msgTv.setText(cursor.getString(cursorCols.message));
		holder.timeTv.setText(DateUtils.getRelativeTimeSpanString(getActivity(), cursor.getLong(cursorCols.time)));
		// if(cursor.getInt(CursorCols.favourite) == 1) {
		// holder.favouriteIcon.setVisibility(View.VISIBLE);
		// } else {
		// holder.favouriteIcon.setVisibility(View.GONE);
		// }

		Uri lookupUri = Contacts.CONTENT_LOOKUP_URI.buildUpon()
				.appendPath(Uri.encode(cursor.getString(cursorCols.contact_lookup))).build();

		if (cursor.getString(cursorCols.contact_lookup) != null) {
			holder.qcb.assignContactUri(lookupUri);
		} else {
			holder.qcb.assignContactFromPhone(cursor.getString(cursorCols.display_name), true);
		}

		mPhotoLoader.loadPhoto(holder.qcb, UriUtils.parseUriOrNull(cursor.getString(cursorCols.photo_uri)));

		// Get our TxtMessage instance
		holder.msg = new TxtMessage(cursor.getString(cursorCols.display_name), holder.msgTv.getText().toString(),
				cursor.getLong(cursorCols.time), new Contact(cursor.getString(cursorCols.contact_lookup),
						cursor.getString(cursorCols.display_name), cursor.getString(cursorCols.phone_number),
						cursor.getString(cursorCols.photo_uri)), (cursorCols.favourite == -1) ? false
						: (cursor.getInt(cursorCols.favourite) == 1));

		((View) view.getParent()).setTag(holder);
		return true;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> list, View view, int pos, long id) {
		ViewHolder holder = (ViewHolder) view.getTag();

		if (mOnMessageItemLongClickListener != null) {
			mOnMessageItemLongClickListener.onMessageItemLongClick(getType(), holder.id, holder.msg);
		} else {
			if (BuildConfig.DEBUG) {
				Log.w(TAG, "Long clicked with no listener set");
			}
		}

		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		ViewHolder holder = (ViewHolder) view.getTag();

		if (holder != null) {
			Intent intent = new Intent(getActivity(), ComposeActivity.class);
			intent.putExtra(Extra.ID, holder.id);
			startActivity(intent);
		} else {
			if (BuildConfig.DEBUG) {
				Log.w(TAG, "Clicked with no tag set");
			}
		}
	}

	public void clearTextFilter() {
		if(mList != null) {
			mCurrentFilterText = null;
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	public void setFilterText(String text) {
		if(mList != null && mAdapter != null) {
			mCurrentFilterText = text;
			getLoaderManager().restartLoader(0, null, this);
		}
	}
	
	public boolean hasFilterApplied() {
		return mCurrentFilterText != null;
	}

	public void setPhotoLoader(ContactPhotoManager photoLoader) {
		mPhotoLoader = photoLoader;
	}

	public void setOnMessageItemLongClickListener(OnMessageItemLongClickListener listener) {
		mOnMessageItemLongClickListener = listener;
	}

	public static interface OnMessageItemLongClickListener {
		public void onMessageItemLongClick(int fragmentType, int id, TxtMessage msg);
	}

	protected class CursorCols {
		public int id = -1;
		public int time = -1;
		public int contact_lookup = -1;
		public int display_name = -1;
		public int phone_number = -1;
		public int photo_uri = -1;
		public int message = -1;
		public int favourite = -1;
	}

	protected static class ViewHolder {
		public int id;
		// public ImageView favouriteIcon;
		public TxtMessage msg;
		public TextView nameTv;
		public TextView msgTv;
		public TextView timeTv;
		public QuickContactBadge qcb;
	}

	public class FilterableMessageAdapter extends SimpleCursorAdapter {
		public FilterableMessageAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to, 0);
		}
	}
}

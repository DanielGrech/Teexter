package com.DGSD.Teexter.Fragment;


import java.io.InputStream;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.TxtMessage;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.UI.QuickContactBadge;
import com.DGSD.Teexter.Utils.AnimUtils;
import com.DGSD.Teexter.Utils.ContactUtils.Contact;
import com.actionbarsherlock.app.SherlockFragment;

public abstract class BaseListFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		ViewBinder, OnItemLongClickListener {
	private static final String TAG = BaseListFragment.class.getSimpleName();
	
	protected ListView mList;
	protected SimpleCursorAdapter mAdapter;
	protected OnMessageItemLongClickListener mOnMessageItemLongClickListener;
	protected Drawable mDefaultContactBitmap;
	
	protected abstract int getType();
	protected abstract SimpleCursorAdapter onCreateAdapter();
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_list, container, false);
		
		mAdapter = onCreateAdapter();
		
		mList = (ListView) v.findViewById(android.R.id.list);
		mList.setAdapter(mAdapter);
		mList.setLayoutAnimation(AnimUtils.getListViewCascadeAnimator());
		
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter.setViewBinder(this);
		mList.setOnItemLongClickListener(this);
		
		getLoaderManager().initLoader(0, null, this);
		mDefaultContactBitmap = getResources().getDrawable(R.drawable.ic_contact_picture);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}

	@Override
	public boolean setViewValue(View view, Cursor cursor, int col) {
		if (CursorCols.id < 0) {
			CursorCols.id = cursor.getColumnIndex(DbField.ID.getName());
			CursorCols.time = cursor.getColumnIndex(DbField.TIME.getName());
			CursorCols.contact_lookup = cursor.getColumnIndex(DbField.CONTACT_LOOKUP_ID.getName());
			CursorCols.display_name = cursor.getColumnIndex(DbField.DISPLAY_NAME.getName());
			CursorCols.message = cursor.getColumnIndex(DbField.MESSAGE.getName());
			CursorCols.favourite = cursor.getColumnIndex(DbField.FAVOURITE.getName());
		}
		View parent = (View) view.getParent();

		ViewHolder holder = (ViewHolder) parent.getTag();
		if (holder == null) {
			holder = new ViewHolder();
			holder.nameTv = (TextView) parent.findViewById(R.id.name);
			holder.msgTv = (TextView) parent.findViewById(R.id.message);
			holder.timeTv = (TextView) parent.findViewById(R.id.date);
			holder.qcb = (QuickContactBadge) parent.findViewById(R.id.quick_contact_badge);
		}

		holder.id = cursor.getInt(CursorCols.id);
		holder.nameTv.setText(cursor.getString(CursorCols.display_name));
		holder.msgTv.setText(cursor.getString(CursorCols.message));
		holder.timeTv.setText(DateUtils.getRelativeTimeSpanString(getActivity(), cursor.getLong(CursorCols.time)));

		Uri lookupUri = Contacts.CONTENT_LOOKUP_URI.buildUpon()
				.appendPath(Uri.encode(cursor.getString(CursorCols.contact_lookup))).build();
		
		if(cursor.getString(CursorCols.contact_lookup) != null) {
			holder.qcb.assignContactUri(lookupUri);
		} else {
			holder.qcb.assignContactFromPhone(cursor.getString(CursorCols.display_name), true);
		}

		ContentResolver cr = getActivity().getContentResolver();
		Uri contactUri = Contacts.lookupContact(cr, lookupUri);
		if (contactUri != null) {
			InputStream is = Contacts.openContactPhotoInputStream(cr, contactUri);
			if (is != null) {
				holder.qcb.setImageBitmap(BitmapFactory.decodeStream(is));
			} else {
				holder.qcb.setImageDrawable(mDefaultContactBitmap);
			}
		} else {
			holder.qcb.setImageDrawable(mDefaultContactBitmap);
		} 

		//Get our TxtMessage instance
		Contact contact = new Contact(cursor.getString(CursorCols.contact_lookup), cursor.getString(CursorCols.display_name));
		holder.msg = new TxtMessage(contact.name, holder.msgTv.getText().toString(), cursor.getLong(CursorCols.time), contact);
		
		parent.setTag(holder);
		return true;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> list, View view, int pos, long id) {
		ViewHolder holder = (ViewHolder) view.getTag();
		
		if(mOnMessageItemLongClickListener != null) {
			mOnMessageItemLongClickListener.onMessageItemLongClick(getType(), holder.id, holder.msg);
		} else {
			if(BuildConfig.DEBUG) {
				Log.w(TAG, "Long clicked with no listener set");
			}
		}
		
		return true;
	}

	public void setOnMessageItemLongClickListener(OnMessageItemLongClickListener listener) {
		mOnMessageItemLongClickListener = listener;
	}
	
	public static interface OnMessageItemLongClickListener {
		public void onMessageItemLongClick(int fragmentType, int id, TxtMessage msg);
	}

	protected static class CursorCols {
		public static int id = -1;
		public static int time = -1;
		public static int contact_lookup = -1;
		public static int display_name = -1;
		public static int message = -1;
		public static int favourite = -1;
	}

	protected static class ViewHolder {
		public int id;
		public TxtMessage msg;
		public TextView nameTv;
		public TextView msgTv;
		public TextView timeTv;
		public QuickContactBadge qcb;
	}

}

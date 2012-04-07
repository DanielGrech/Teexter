package com.DGSD.Teexter.Fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

import com.DGSD.Teexter.R;
import com.DGSD.Teexter.Activity.MainActivity;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;

public class InboxFragment extends BaseListFragment {
	public static InboxFragment newInstance() {
		InboxFragment frag = new InboxFragment();

		return frag;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity(), MessagesProvider.INBOX_URI, null, null, null, DbField.TIME.getName()
				+ " DESC");
	}

	@Override
	protected SimpleCursorAdapter onCreateAdapter() {
		return new SimpleCursorAdapter(getActivity(), R.layout.inbox_list_item, null, new String[] {
			DbField.ID.getName()
		}, new int[] {
			R.id.message
		}, 0);
	}

	@Override
	protected int getType() {
		return MainActivity.INBOX_PAGE;
	}
}

package com.DGSD.Teexter.Fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.DGSD.Teexter.R;
import com.DGSD.Teexter.Activity.MainActivity;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;

public class DraftsFragment extends BaseListFragment {
	public static DraftsFragment newInstance() {
		DraftsFragment frag = new DraftsFragment();

		return frag;
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		return new CursorLoader(getActivity(), MessagesProvider.SENT_URI, null, DbField.IS_DRAFT + "=1", null, DbField.TIME.getName()
				+ " DESC");
	}

	@Override
	protected FilterableMessageAdapter onCreateAdapter() {
		return new FilterableMessageAdapter(getActivity(), R.layout.inbox_list_item, null, new String[] {
			DbField.ID.getName()
		}, new int[] {
			R.id.message
		});
	}
	
	@Override
	protected int getType() {
		return MainActivity.DRAFTS_PAGE;
	}
}

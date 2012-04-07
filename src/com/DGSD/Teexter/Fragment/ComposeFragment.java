package com.DGSD.Teexter.Fragment;

import java.io.IOException;
import java.io.InputStream;

import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;
import com.DGSD.Teexter.Fragment.BaseListFragment.CursorCols;
import com.DGSD.Teexter.UI.ExpandableTextView;
import com.DGSD.Teexter.UI.QuickContactBadge;
import com.DGSD.Teexter.UI.RecipientsAdapter;
import com.DGSD.Teexter.UI.RecipientsEditor;
import com.DGSD.Teexter.UI.StatefulEditText;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

public class ComposeFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = ComposeFragment.class.getSimpleName();

	private int mResponseToMessageId = -1;

	private RecipientsEditor mRecipientsInput;
	private StatefulEditText mMessageInput;

	// Top message view
	private TextView mTimeView;
	private TextView mDisplayNameView;
	private ExpandableTextView mReplyToMessageView;
	private QuickContactBadge mQcb;

	public static ComposeFragment newInstance(int msgId) {
		ComposeFragment frag = new ComposeFragment();

		if (msgId != -1) {
			Bundle args = new Bundle();
			args.putInt(Extra.ID, msgId);
			frag.setArguments(args);
		}

		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		if (getArguments() != null) {
			mResponseToMessageId = getArguments().getInt(Extra.ID, -1);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_compose, container, false);

		if (mResponseToMessageId != -1) {
			// We will need to fill in our message view
			View wrapper = v.findViewById(R.id.in_reply_to_message);
			mTimeView = (TextView) wrapper.findViewById(R.id.date);
			mDisplayNameView = (TextView) wrapper.findViewById(R.id.name);
			mReplyToMessageView = (ExpandableTextView) wrapper.findViewById(R.id.expandable_message);
			mQcb = (QuickContactBadge) wrapper.findViewById(R.id.quick_contact_badge);
		} else {
			v.findViewById(R.id.in_reply_to_message).setVisibility(View.GONE);
		}

		mRecipientsInput = (RecipientsEditor) v.findViewById(R.id.recipients_editor);
		mRecipientsInput.setAdapter(new RecipientsAdapter(getActivity()));
		mMessageInput = (StatefulEditText) v.findViewById(R.id.new_message);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mResponseToMessageId != -1) {
			getLoaderManager().initLoader(0, null, this);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.compose_menu, menu);
		
		if(getArguments() == null || getArguments().getInt(Extra.ID, -1) == -1) {
			menu.findItem(R.id.favourite).setVisible(false);
			menu.findItem(R.id.share).setVisible(false);
			menu.findItem(R.id.delete).setVisible(false);
			menu.findItem(R.id.copy).setVisible(false);
		}
		
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (mResponseToMessageId == -1) {
			return null;
		} else {
			return new CursorLoader(getActivity(), MessagesProvider.INBOX_URI, null, DbField.ID + "=?", new String[] {
				String.valueOf(mResponseToMessageId)
			}, null);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor != null && cursor.moveToFirst()) {
			mTimeView.setText(DateUtils.getRelativeTimeSpanString(getActivity(),
					cursor.getLong(cursor.getColumnIndex(DbField.TIME.getName()))));
			mDisplayNameView.setText(cursor.getString(cursor.getColumnIndex(DbField.DISPLAY_NAME.getName())));
			mReplyToMessageView.setText(cursor.getString(cursor.getColumnIndex(DbField.MESSAGE.getName())));

			String lookupKey = cursor.getString(cursor.getColumnIndex(DbField.CONTACT_LOOKUP_ID.getName()));
			Uri lookupUri = Contacts.CONTENT_LOOKUP_URI.buildUpon()
					.appendPath(Uri.encode(cursor.getString(CursorCols.contact_lookup))).build();

			if (lookupKey != null) {
				mQcb.assignContactUri(lookupUri);
			} else {
				mQcb.assignContactFromPhone(mDisplayNameView.getText().toString(), true);
			}

			if (lookupUri != null) {
				InputStream is = Contacts.openContactPhotoInputStream(getActivity().getContentResolver(), lookupUri);
				if (is != null) {
					mQcb.setImageBitmap(BitmapFactory.decodeStream(is));
					try {
						is.close();
					} catch (IOException e) {
						if (BuildConfig.DEBUG) {
							Log.w(TAG, "Error closing input stream");
						}
					}

				} else {
					mQcb.setImageDrawable(getResources().getDrawable(R.drawable.ic_contact_picture));
				}
			} else {
				mQcb.setImageDrawable(getResources().getDrawable(R.drawable.ic_contact_picture));
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {

	}

}

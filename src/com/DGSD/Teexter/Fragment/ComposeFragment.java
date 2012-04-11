package com.DGSD.Teexter.Fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.Activity.ComposeActivity;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;
import com.DGSD.Teexter.UI.ExpandableTextView;
import com.DGSD.Teexter.UI.QuickContactBadge;
import com.DGSD.Teexter.UI.StatefulEditText;
import com.DGSD.Teexter.UI.Recipient.RecipientAdapter;
import com.DGSD.Teexter.UI.Recipient.RecipientEditTextView;
import com.DGSD.Teexter.UI.Recipient.RecipientEditorTokenizer;
import com.DGSD.Teexter.Utils.ContactPhotoManager;
import com.DGSD.Teexter.Utils.CopyUtils;
import com.DGSD.Teexter.Utils.IntentUtils;
import com.DGSD.Teexter.Utils.SmsUtils;
import com.DGSD.Teexter.Utils.ToastUtils;
import com.DGSD.Teexter.Utils.UriUtils;

public class ComposeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = ComposeFragment.class.getSimpleName();

	private int mResponseToMessageId = -1;
	private boolean mIsReply = false;
	private boolean mIsForward = false;

	private RecipientEditTextView mRecipientsInput;
	private StatefulEditText mMessageInput;

	// Top message view
	private TextView mTimeView;
	private TextView mDisplayNameView;
	private ExpandableTextView mReplyToMessageView;
	private QuickContactBadge mQcb;
	protected ContactPhotoManager mPhotoLoader;

	private OnFavouriteListener mOnFavouriteListener;
	private OnDeleteListener mOnDeleteListener;

	private boolean mIsFavourite = false;

	public static ComposeFragment newInstance(int msgId, boolean isReply, boolean isForward) {
		ComposeFragment frag = new ComposeFragment();

		Bundle args = new Bundle();
		if (msgId != -1) {
			args.putInt(Extra.ID, msgId);
		}

		args.putBoolean(Extra.IS_REPLY, isReply);
		args.putBoolean(Extra.IS_FORWARD, isForward);

		frag.setArguments(args);

		return frag;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		if (getArguments() != null) {
			mResponseToMessageId = getArguments().getInt(Extra.ID, -1);
			mIsReply = getArguments().getBoolean(Extra.IS_REPLY, false);
			mIsForward = getArguments().getBoolean(Extra.IS_FORWARD, false);
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
			v.findViewById(R.id.reply_subtitle).setVisibility(View.GONE);
		}

		mRecipientsInput = (RecipientEditTextView) v.findViewById(R.id.recipients_editor);
		mRecipientsInput.setAdapter(new RecipientAdapter(getActivity(), (RecipientEditTextView) mRecipientsInput));
		mRecipientsInput.setTokenizer(new RecipientEditorTokenizer());
		mRecipientsInput.setValidator(new AutoCompleteTextView.Validator() {
			@Override
			public boolean isValid(CharSequence text) {
				return text == null ? false : PhoneNumberUtils.isWellFormedSmsAddress(text.toString());

			}

			@Override
			public CharSequence fixText(CharSequence invalidText) {
				return invalidText;
			}
		});

		mMessageInput = (StatefulEditText) v.findViewById(R.id.new_message);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (mResponseToMessageId != -1) {
			mPhotoLoader = ContactPhotoManager.getInstance(getActivity());
			getLoaderManager().initLoader(0, null, this);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.compose_menu, menu);

		if (getArguments() == null || getArguments().getInt(Extra.ID, -1) == -1) {
			menu.findItem(R.id.favourite).setVisible(false);
			menu.findItem(R.id.share).setVisible(false);
			menu.findItem(R.id.delete).setVisible(false);
			menu.findItem(R.id.copy).setVisible(false);
			menu.findItem(R.id.forward).setVisible(false);
		} else {
			MenuItem menuItem = menu.findItem(R.id.share);
			ShareActionProvider mShareActionProvider = (ShareActionProvider) menuItem.getActionProvider();
			String displayName = mDisplayNameView.getText().toString();
			mShareActionProvider.setShareIntent(IntentUtils.newShareTextIntent("Text from " + displayName,
					mReplyToMessageView.getText().toString(), "Share text from " + displayName));
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mOnDeleteListener = null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.favourite: {
				if (mOnFavouriteListener != null) {
					mOnFavouriteListener.onToggleFavourite(mResponseToMessageId);
				}
				return true;
			}
			case R.id.delete: {
				if (mOnDeleteListener != null) {
					mOnDeleteListener.onDelete(mResponseToMessageId);
				}
				return true;
			}
			case R.id.copy: {
				String displayName = mDisplayNameView.getText().toString();
				CopyUtils.copyText(getActivity(), "Text from " + displayName, mReplyToMessageView.getText().toString());
				ToastUtils.show(getActivity(), "Copied text from " + displayName, Toast.LENGTH_SHORT);
				return true;
			}
			case R.id.forward: {
				Intent intent = new Intent(getActivity(), ComposeActivity.class);
				intent.putExtra(Extra.ID, mResponseToMessageId);
				intent.putExtra(Extra.IS_FORWARD, true);
				startActivity(intent);
				return true;
			}
			case R.id.send: {
				if (mRecipientsInput.hasFocus()) {
					mRecipientsInput.clearFocus();
				}

				if (!mRecipientsInput.isValid()) {
					ToastUtils.show(getActivity(), "Some numbers are invalid", Toast.LENGTH_SHORT);
					return true;
				}

				String[] addresses = mRecipientsInput.getAddresses();
				if (addresses == null || addresses.length == 0) {
					mRecipientsInput.setError("Please enter a recipient");
					return true;
				}

				if (TextUtils.isEmpty(mMessageInput.getText())) {
					mMessageInput.setError("Please enter a message");
					return true;
				}

				SmsUtils.sendMessage(getActivity(), addresses, mMessageInput.getText().toString(),
						mResponseToMessageId, false);
				getActivity().finish();
				return true;
			}
		}

		return false;
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

			mIsFavourite = cursor.getInt(cursor.getColumnIndex(DbField.FAVOURITE.getName())) == 1;

			if (mIsReply) {
				String num = cursor.getString(cursor.getColumnIndex(DbField.NUMBER.getName()));
				if (num == null) {
					if (BuildConfig.DEBUG) {
						Log.w(TAG, "No number found");
					}
				} else {
					mRecipientsInput.append(num + ", ");
				}
			} else if (mIsForward) {
				mMessageInput.setText(cursor.getString(cursor.getColumnIndex(DbField.MESSAGE.getName())));
			}

			String lookupKey = cursor.getString(cursor.getColumnIndex(DbField.CONTACT_LOOKUP_ID.getName()));
			Uri lookupUri = Contacts.CONTENT_LOOKUP_URI
					.buildUpon()
					.appendPath(
							Uri.encode(cursor.getString(cursor.getColumnIndex(DbField.CONTACT_LOOKUP_ID.getName()))))
					.build();

			if (lookupKey != null) {
				mQcb.assignContactUri(lookupUri);
			} else {
				mQcb.assignContactFromPhone(mDisplayNameView.getText().toString(), true);
			}

			mPhotoLoader.loadPhoto(mQcb,
					UriUtils.parseUriOrNull(cursor.getString(cursor.getColumnIndex(DbField.PHOTO_URI.getName()))));
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {

	}

	public void setOnDeleteListener(OnDeleteListener listener) {
		mOnDeleteListener = listener;
	}

	public void setOnFavouriteListener(OnFavouriteListener listener) {
		mOnFavouriteListener = listener;
	}

	public boolean isFavourite() {
		return mIsFavourite;
	}

	public void setIsFavourite(boolean isFav) {
		mIsFavourite = isFav;
	}

	public static interface OnDeleteListener {
		public void onDelete(int id);
	}

	public static interface OnFavouriteListener {
		public void onToggleFavourite(int id);
	}
}

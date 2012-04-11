package com.DGSD.Teexter.Receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Service.DatabaseService;
import com.DGSD.Teexter.Utils.ContactUtils;
import com.DGSD.Teexter.Utils.ContactUtils.Contact;

public class SmsSentReceiver extends BroadcastReceiver {
	private static final String TAG = SmsSentReceiver.class.getSimpleName();

	public static final String ACTION = "com.DGSD.Teexter.SENT_SMS_RECEIVER_ACTION";

	@Override
	public void onReceive(Context context, Intent intent) {
		// get the Bundle map from the Intent parameter to onReceive()
		Bundle bundle = intent.getExtras();

		boolean isFromDraft = bundle.getBoolean(Extra.IS_DRAFT, false);
		int replyToId = bundle.getInt(Extra.ID, -1);
		String address = bundle.getString(Extra.ADDRESS);
		String message = bundle.getString(Extra.TEXT);
		Contact contact = ContactUtils.getContactFromPhone(context, address);

		ContentValues values = new ContentValues();
		values.put(DbField.TIME.getName(), System.currentTimeMillis());
		values.put(DbField.MESSAGE.getName(), message);
		values.put(DbField.NUMBER.getName(), address);

		if (contact != null) {
			values.put(DbField.CONTACT_LOOKUP_ID.getName(), contact.lookupId);
			values.put(DbField.PHOTO_URI.getName(), contact.photoUri);
		}

		values.put(DbField.DISPLAY_NAME.getName(), (contact == null || contact.name == null) ? address : contact.name);

		if (replyToId != -1) {
			values.put(DbField.IN_REPLY_TO_ID.getName(), replyToId);
		}

		if (BuildConfig.DEBUG) {
			Log.i(TAG, "Inserting: " + values);
		}

		switch (getResultCode()) {
			case Activity.RESULT_OK:
				// Sms was sent successfully! Put it into our 'sent messages'
				// table
				if (BuildConfig.DEBUG) {
					Log.i(TAG, "Message was sent successfully!");
				}
				values.put(DbField.IS_DRAFT.getName(), "0");

				DatabaseService.requestInsertSent(context, values);

				if (isFromDraft) {
					DatabaseService.requestRemoveDraft(context, address, message);
				}

				Toast.makeText(context, "Message Sent!", Toast.LENGTH_SHORT).show();
				break;
			default:
				// Some sort of error occured! Notify the user and save in
				// 'drafts' table
				if (BuildConfig.DEBUG) {
					Log.w(TAG, "Error sending message");
				}

				values.put(DbField.IS_DRAFT.getName(), "1");

				if (!isFromDraft) {
					Toast.makeText(context, "Error sending message. Saved in drafts", Toast.LENGTH_LONG).show();
					// Dont want to insert mulitple records if the draft fails
					// more than once..
					DatabaseService.requestInsertDraft(context, values);
				}

				break;
		}
	}
}

package com.DGSD.Teexter.Service;

import java.sql.SQLDataException;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;
import com.DGSD.Teexter.Utils.ContactUtils;
import com.DGSD.Teexter.Utils.ContactUtils.Contact;

public class ImportSmsService extends IntentService {
	private static final String TAG = ImportSmsService.class.getSimpleName();

	public static final String PERMISSION = "com.DGSD.Teexter.ACCESS_DATA";

	private static final Uri SYSTEM_SMS_INBOX_URI = Uri.parse("content://sms/inbox");

	private static final String[] INBOX_PROJ = new String[] {
			"address", "date", "read", "body"
	};

	public ImportSmsService() {
		super(TAG);
	}

	private void insertInboxRecords() throws Exception {
		// Try and insert inbox messages
		Cursor cursor = getContentResolver().query(SYSTEM_SMS_INBOX_URI, INBOX_PROJ, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			do {
				ContentValues values = new ContentValues();

				Contact contact = ContactUtils.getContactFromPhone(this, cursor.getString(0));

				if (contact == null || contact.name == null) {
					values.put(DbField.DISPLAY_NAME.getName(), cursor.getString(0));
				} else {
					values.put(DbField.DISPLAY_NAME.getName(), contact.name);
				}

				values.put(DbField.TIME.getName(), cursor.getLong(1));
				values.put(DbField.READ.getName(), cursor.getInt(2));
				values.put(DbField.MESSAGE.getName(), cursor.getString(3));

				if (contact != null && contact.lookupId != null) {
					values.put(DbField.CONTACT_LOOKUP_ID.getName(), contact.lookupId);
				}
				
				if (contact != null && contact.photoUri != null) {
					values.put(DbField.PHOTO_URI.getName(), contact.photoUri);
				}
				

				if (DatabaseService.doInsert(this, MessagesProvider.INBOX_URI, values) == null) {
					throw new SQLDataException("No record was inserted for sms: " + cursor.getString(4));
				}
			} while (cursor.moveToNext());
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			insertInboxRecords();
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error importing sms", e);
			}
		}
	}
}

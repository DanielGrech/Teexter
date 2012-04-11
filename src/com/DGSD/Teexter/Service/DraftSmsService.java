package com.DGSD.Teexter.Service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;
import com.DGSD.Teexter.Utils.SmsUtils;

public class DraftSmsService extends IntentService {
	private static final String TAG = DraftSmsService.class.getSimpleName();

	public DraftSmsService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Cursor cursor = null;
		try {
			cursor = getContentResolver().query(MessagesProvider.SENT_URI, null, DbField.IS_DRAFT + "=1", null, null);

			if (cursor != null && cursor.moveToFirst()) {
				int messageInReplyToCol = cursor.getColumnIndex(DbField.IN_REPLY_TO_ID.getName());
				int messageCol = cursor.getColumnIndex(DbField.MESSAGE.getName());
				int phoneCol = cursor.getColumnIndex(DbField.NUMBER.getName());
				do {
					String messageInReplyToId = cursor.getString(messageInReplyToCol);
					int inReplyToId = -1;
					try {
						inReplyToId = Integer.valueOf(messageInReplyToId);
					} catch (Exception e) {
						inReplyToId = -1;
					}

					SmsUtils.sendMessage(this, new String[] {
						cursor.getString(phoneCol)
					}, cursor.getString(messageCol), inReplyToId, true);
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error importing sms", e);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	public static synchronized void requestCheckDrafts(Context c) {
		Intent intent = new Intent(c, DraftSmsService.class);

		c.startService(intent);
	}
}

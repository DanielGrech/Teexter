package com.DGSD.Teexter.Service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.DGSD.Teexter.BroadcastType;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;

public class DatabaseService extends IntentService {
	private static final String TAG = DatabaseService.class.getSimpleName();

	public static final String PERMISSION = "com.DGSD.Teexter.ACCESS_DATA";
	
	public DatabaseService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		switch (intent.getIntExtra(Extra.DATA_TYPE, -1)) {
			case RequestType.INSERT_MSG_IN_INBOX:
				if (doInsert(this, MessagesProvider.INBOX_URI,
						(ContentValues) intent.getParcelableExtra(Extra.CONTENT_VALUES)) != null) {
					broadcastResult(RequestType.INSERT_MSG_IN_INBOX);
				} else {
					broadcastError(RequestType.INSERT_MSG_IN_INBOX);
				}
				break;
			case RequestType.TOGGLE_FAVOURITE:
				ContentValues values = new ContentValues();
				values.put(DbField.FAVOURITE.getName(), intent.getBooleanExtra(Extra.FAVOURITE, false) ? 1 : 0);
				
				String selection = DbField.ID + "=?";
				String[] selArgs = new String[]{ String.valueOf(intent.getIntExtra(Extra.ID, -1)) };
				
				if(doUpdate(this, MessagesProvider.FAVOURITES_URI, values, selection, selArgs) != 1) {
					broadcastError(RequestType.TOGGLE_FAVOURITE);
				} else {
					broadcastResult(RequestType.TOGGLE_FAVOURITE);
				}
				break;
		}
	}

	public static Uri doInsert(Context c, Uri uri, ContentValues values) {
		return c.getContentResolver().insert(uri, values);
	}
	
	public static int doUpdate(Context c, Uri uri, ContentValues values, String sel, String[] selArgs) {
		return c.getContentResolver().update(uri, values, sel, selArgs);
	}

	private void broadcastResult(int type) {
		Intent intent = new Intent(BroadcastType.SUCCESS);
		intent.putExtra(Extra.DATA_TYPE, type);
		sendBroadcast(intent, PERMISSION);
	}

	private void broadcastError(int type) {
		Intent intent = new Intent(BroadcastType.ERROR);
		intent.putExtra(Extra.DATA_TYPE, type);
		sendBroadcast(intent, PERMISSION);
	}
	
	public static synchronized void requestToggleFavourite(Context c, int msgId, boolean isFavourite) {
		Intent intent = new Intent(c, DatabaseService.class);
		intent.putExtra(Extra.DATA_TYPE, RequestType.TOGGLE_FAVOURITE);
		intent.putExtra(Extra.ID, msgId);
		intent.putExtra(Extra.FAVOURITE, isFavourite);
		
		c.startService(intent);
	}
	
	public static class RequestType {
		public static final int INSERT_MSG_IN_INBOX = 0x1;
		public static final int TOGGLE_FAVOURITE = 0x2;
	}

}
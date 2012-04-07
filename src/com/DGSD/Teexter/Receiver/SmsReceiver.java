package com.DGSD.Teexter.Receiver;

import java.sql.SQLDataException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.R;
import com.DGSD.Teexter.TxtMessage;
import com.DGSD.Teexter.Activity.MainActivity;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.Provider.MessagesProvider;
import com.DGSD.Teexter.Service.DatabaseService;
import com.DGSD.Teexter.Utils.ContactUtils;
import com.DGSD.Teexter.Utils.DiagnosticUtils;

public class SmsReceiver extends BroadcastReceiver {
	private static final String TAG = SmsReceiver.class.getSimpleName();

	public static final int NEW_MESSAGE_NOTIFICATION = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		// get the Bundle map from the Intent parameter to onReceive()
		Bundle bundle = intent.getExtras();

		// get the SMS received
		Object[] pdus = (Object[]) bundle.get("pdus");
		SmsMessage[] msgs = new SmsMessage[pdus.length];

		/** sms sender phone */
		String smsSender = "";

		/** body of received sms */
		String smsBody = "";

		/** timerstamp */
		long timestamp = 0L;

		for (int i = 0, len = msgs.length; i < len; i++) {
			msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
			smsSender += msgs[i].getOriginatingAddress();
			smsBody += msgs[i].getMessageBody().toString();
			timestamp += msgs[i].getTimestampMillis();
		}

		TxtMessage txt = new TxtMessage(smsSender, smsBody, timestamp, ContactUtils.getContactFromPhone(context,
				smsSender), false);
		if (BuildConfig.DEBUG) {
			Log.v(TAG, "Got Message: " + txt);
		}

		try {
			// Insert message into db
			ContentValues values = new ContentValues();
			if(txt.getContact() == null || txt.getContact().name == null) {
				values.put(DbField.DISPLAY_NAME.getName(), txt.getSender());
			} else {
				values.put(DbField.DISPLAY_NAME.getName(), txt.getContact().name);
			}
			
			values.put(DbField.MESSAGE.getName(), txt.getMessage());
			values.put(DbField.TIME.getName(), txt.getTimestamp());

			if (txt.getContact() != null && txt.getContact().lookupId != null) {
				values.put(DbField.CONTACT_LOOKUP_ID.getName(), txt.getContact().lookupId);
			}

			if (DatabaseService.doInsert(context, MessagesProvider.INBOX_URI, values) == null) {
				throw new SQLDataException("No record was inserted for sms: " + txt);
			}

			showNotification(context, txt);

			// We intercepted the message, we dont want other SMS apps to
			// intercept the message too!
			this.abortBroadcast();
		} catch (Exception e) {
			// Its disastrous if we fail to insert the message and we cancel the
			// broadcast, as the users message will be lost!
			// Let the broadcast through if nothing goes wrong..
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error saving message: " + txt, e);
			}
		}
	}

	private void showNotification(Context c, TxtMessage msg) {
		NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = null;

		// Common to notifications for all API levels.
		Intent intent = new Intent(c, MainActivity.class);
		intent.putExtra(Extra.TEXT, msg);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pi = PendingIntent.getActivity(c, NEW_MESSAGE_NOTIFICATION, intent, 0);

		String bestName = (msg.getContact() == null || msg.getContact().name == null) ? msg.getSender() == null ? "Unknown Sender"
				: msg.getSender()
				: msg.getContact().name;

		if (DiagnosticUtils.ANDROID_API_LEVEL >= 11) {
			// We can show a rich notification!
			Notification.Builder builder = new Notification.Builder(c);
			builder.setSmallIcon(R.drawable.ic_launcher);
			builder.setContentText(msg.getMessage());
			builder.setDefaults(Notification.DEFAULT_ALL); // Default sound,
															// light and
															// vibration;
			builder.setContentIntent(pi);
			builder.setAutoCancel(true);

			builder.setContentTitle(bestName);
			builder.setTicker("New Message from " + bestName);

			Resources r = c.getResources();
			if (msg.getContact() != null && msg.getContact().lookupId != null) {
				Bitmap photo = ContactUtils.getContactPhoto(c.getContentResolver(), msg.getContact().lookupId);
				if (photo == null) {
					// Load the default image
					
					builder.setLargeIcon(getLargeIconBitmap(r, BitmapFactory.decodeResource(r, R.drawable.ic_contact_picture)));
				} else {
					builder.setLargeIcon(getLargeIconBitmap(r, photo));
				}
			} else {
				// Load the default image
				builder.setLargeIcon(getLargeIconBitmap(r, BitmapFactory.decodeResource(r, R.drawable.ic_contact_picture)));
			}

			notification = builder.getNotification();
		} else {
			// Show a regular notification..
			notification = new Notification(R.drawable.ic_launcher, "New Message from " + bestName,
					System.currentTimeMillis());
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
			notification.setLatestEventInfo(c, bestName, msg.getMessage(), pi);
		}

		nm.notify(NEW_MESSAGE_NOTIFICATION, notification);
	}

	private static final Bitmap getLargeIconBitmap(Resources r, Bitmap source) {
		Bitmap large = Bitmap.createScaledBitmap(source,
				r.getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
				r.getDimensionPixelSize(android.R.dimen.notification_large_icon_height), false);
		source.recycle();

		return large;
	}
}

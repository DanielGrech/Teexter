/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.DGSD.Teexter.Utils;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Extra;
import com.DGSD.Teexter.Receiver.SmsSentReceiver;

public class SmsUtils {
	private static final String TAG = SmsUtils.class.getSimpleName();

	private static int requestCodeCounter = 0;

	public static void resendDraft(Context c, String addr, String msg, int replyToMsgId) {
		sendMessage(c, new String[]{addr}, msg, replyToMsgId, true);
	}
	
	public static void sendMessage(Context context, String[] addresses, String message, int reponseToMessageId, boolean isFromDraft) {
		if (TextUtils.isEmpty(message) || addresses == null || addresses.length == 0) {
			// Dont send an empty message!
			return;
		}

		SmsManager smsManager = SmsManager.getDefault();

		ArrayList<String> messages = smsManager.divideMessage(message);
		if (messages.size() == 1) {
			// Small enough to fit into a single message!
			for (String addr : addresses) {
				Intent sendIntent = new Intent(SmsSentReceiver.ACTION);
				sendIntent.putExtra(Extra.ADDRESS, addr);
				sendIntent.putExtra(Extra.TEXT, message);
				sendIntent.putExtra(Extra.ID, reponseToMessageId);
				sendIntent.putExtra(Extra.IS_DRAFT, isFromDraft);

				if (BuildConfig.DEBUG) {
					Log.i(TAG, "Sending single message to: " + addr + " ; Message = " + message);
				}

				smsManager.sendTextMessage(addr, null, message, PendingIntent.getBroadcast(context,
						requestCodeCounter++, sendIntent, PendingIntent.FLAG_UPDATE_CURRENT), null);
			}
		} else {
			if (BuildConfig.DEBUG) {
				Log.i(TAG, "Going to send '" + message + "' in " + messages.size() + " parts");
			}
			for (String addr : addresses) {
				ArrayList<PendingIntent> sendIntents = new ArrayList<PendingIntent>(messages.size());
				Intent sendIntent = new Intent(SmsSentReceiver.ACTION);
				sendIntent.putExtra(Extra.ADDRESS, addr);
				sendIntent.putExtra(Extra.TEXT, message);
				sendIntent.putExtra(Extra.ID, reponseToMessageId);
				sendIntent.putExtra(Extra.IS_DRAFT, isFromDraft);
				
				sendIntents.add(PendingIntent.getBroadcast(context, requestCodeCounter++, sendIntent,
						PendingIntent.FLAG_UPDATE_CURRENT));

				if (BuildConfig.DEBUG) {
					Log.i(TAG, "Sending mulitpart message to: " + addr);
				}
				// Send a multipart message
				smsManager.sendMultipartTextMessage(addr, null, messages, sendIntents, null);
			}
		}
	}
}
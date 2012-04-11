package com.DGSD.Teexter.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Service.DraftSmsService;

public class NetworkStateReceiver extends BroadcastReceiver {
	public static final String TAG = NetworkStateReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {

		boolean isNetworkDown = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

		if (!isNetworkDown) {
			if(BuildConfig.DEBUG) {
				Log.i(TAG, "Network now connected. Checking drafts");
				DraftSmsService.requestCheckDrafts(context);
			}
		}
	}
}

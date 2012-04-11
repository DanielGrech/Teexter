package com.DGSD.Teexter;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.DGSD.Teexter.Service.ImportSmsService;
import com.DGSD.Teexter.Utils.SharedPrefsUtils;
import com.DGSD.Teexter.Utils.SharedPrefsUtils.PrefKeys;

public class TeexterApp extends Application {
	private static final String TAG = TeexterApp.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();

		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!SharedPrefsUtils.getInstance(TeexterApp.this).get(PrefKeys.HAS_OPENED_BEFORE, false)) {
					// First time opening the app!
					if (BuildConfig.DEBUG) {
						Log.i(TAG, "First time opening app");
					}

					// Flag that we have started the app before!
					SharedPrefsUtils.getInstance(TeexterApp.this).put(PrefKeys.HAS_OPENED_BEFORE, true);

					// Import old sms from system provider into the app
					startService(new Intent(TeexterApp.this, ImportSmsService.class));
				}
			}
		}).start();

	}
}

package com.DGSD.Teexter.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;

/**
 * Hack needed to dynamically allocate a receiver
 */
public class PortableReceiver extends BroadcastReceiver {
	private static final String TAG = PortableReceiver.class.getSimpleName();

	private Receiver mReceiver;

	public void clearReceiver() {
		mReceiver = null;
	}

	public void setReceiver(Receiver receiver) {
		mReceiver = receiver;
	}
	
	public Receiver getReceiver() {
		return mReceiver;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (mReceiver != null) {
			mReceiver.onReceive(context, intent);
		} else {
			if(BuildConfig.DEBUG) {
				Log.w(TAG, "Dropping received Result");
			}
		}
	}
	
	
}
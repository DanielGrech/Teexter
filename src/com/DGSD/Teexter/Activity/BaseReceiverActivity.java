package com.DGSD.Teexter.Activity;

import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.DGSD.Teexter.BroadcastType;
import com.DGSD.Teexter.Receiver.PortableReceiver;
import com.DGSD.Teexter.Receiver.Receiver;
import com.DGSD.Teexter.Service.DatabaseService;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

/**
 * 
 * @author Daniel Grech
 */
public abstract class BaseReceiverActivity extends SherlockFragmentActivity implements
		Receiver {
	public static final String TAG = BaseReceiverActivity.class.getSimpleName();

	private static final String KEY_LOADING_COUNTER = "_loading_counter";

	/**
	 * Portable receiver to dynamically register and unregister other broadcast
	 * receivers
	 */
	protected PortableReceiver mReceiver;

	protected int mLoadingCounter = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		if (savedInstanceState != null) {
			mLoadingCounter = savedInstanceState.getInt(KEY_LOADING_COUNTER, 0);
		}

		mReceiver = new PortableReceiver();
		mReceiver.setReceiver(this);
	}

	@Override
	public void onStart() {
		super.onStart();
		
		if (mLoadingCounter > 0) {
			setSupportProgressBarIndeterminateVisibility(true);
		} else {
			setSupportProgressBarIndeterminateVisibility(false);
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();

		// Register the receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(BroadcastType.SUCCESS);
		filter.addAction(BroadcastType.NO_DATA);
		filter.addAction(BroadcastType.ERROR);

		registerReceiver(mReceiver, filter, DatabaseService.PERMISSION, null);
	}

	@Override
	public void onPause() {
		super.onPause();

		try {
			unregisterReceiver(mReceiver);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Error unregistering receiver");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(KEY_LOADING_COUNTER, mLoadingCounter);
	}

	public void showProgressBar() {
		mLoadingCounter++;
		setSupportProgressBarIndeterminateVisibility(true);
	}

	public void hideProgressBar() {
		mLoadingCounter--;

		if (mLoadingCounter < 0) {
			mLoadingCounter = 0;
		}

		// Check if we are waiting for any other progressable items.
		if (mLoadingCounter == 0) {
			setSupportProgressBarIndeterminateVisibility(false);
		}
	}

}

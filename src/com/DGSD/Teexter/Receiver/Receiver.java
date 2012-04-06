/**
 * 
 */
package com.DGSD.Teexter.Receiver;

import android.content.Context;
import android.content.Intent;

/** 
 * Callback for dynamically receive broadcast events
 *
 * @author Daniel Grech
 */
public interface Receiver {
	public void onReceive(Context context, Intent intent);
}

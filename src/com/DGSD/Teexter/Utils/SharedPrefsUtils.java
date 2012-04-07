package com.DGSD.Teexter.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsUtils {
	public static final String PREFS_NAME = "BHPrefs";
	private static SharedPrefsUtils mInstance;
	
	private SharedPreferences mPrefs;
	
	public static final SharedPrefsUtils getInstance(Context c) {
		if(mInstance == null) {
			mInstance = new SharedPrefsUtils(c);
		}
		
		return mInstance;
	}
	
	private SharedPrefsUtils(Context c) {
		mPrefs = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}
	
	public String get(String key, String defVal) {
		return mPrefs.getString(key, defVal);
	}
	
	public void put(String key, String val) {
		mPrefs.edit().putString(key, val).commit();
	}
	
	public boolean get(String key, boolean defVal) {
		return mPrefs.getBoolean(key, defVal);
	}
	
	public void put(String key, boolean val) {
		mPrefs.edit().putBoolean(key, val).commit();
	}
	
	public static class PrefKeys {
		public static final String HAS_OPENED_BEFORE = "_has_opened_before";
	}
}

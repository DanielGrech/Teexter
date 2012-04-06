package com.DGSD.Teexter.Utils;

/* Copyright (c) 2009 Matthias Käppler
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StrictMode;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;

/**
 * Useful android diagnostic helper classes
 *
 */
public class DiagnosticUtils {

    public static final int ANDROID_API_LEVEL;

    static {
        int apiLevel = -1;
        try {
            apiLevel = Build.VERSION.class.getField("SDK_INT").getInt(null);
        } catch (Exception e) {
            apiLevel = Integer.parseInt(Build.VERSION.SDK);
        }
        ANDROID_API_LEVEL = apiLevel;
    }

    /**
     * Returns the ANDROID_ID unique device ID for the current device. Reading that ID has changed
     * between platform versions, so this method takes care of attempting to read it in different
     * ways, if one failed.
     * 
     * @param context
     *            the context
     * @return the device's ANDROID_ID, or null if it could not be determined
     * @see android.provider.Settings.Secure#ANDROID_ID
     */
    public static String getAndroidId(Context context) {
        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        if (androidId == null) {
            // this happens on 1.6 and older
            androidId = Settings.System.getString(context.getContentResolver(),
                    Settings.System.ANDROID_ID);
        }
        return androidId;
    }

    /**
     * Same as {@link #getAndroidId(android.content.Context)}, but never returns null.
     * 
     * @param context
     *            the context
     * @param fallbackValue
     *            the fallback value
     * @return the device's ANDROID_ID, or the fallback value if it could not be determined
     * @see android.provider.Settings.Secure#ANDROID_ID
     */
    public static String getAndroidId(Context context, String fallbackValue) {
        String androidId = getAndroidId(context);
        if (androidId == null) {
            androidId = fallbackValue;
        }
        return androidId;
    }

    public static String getApplicationVersionString(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return "v" + info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String createDiagnosis(Activity context, Exception error) {
        StringBuilder sb = new StringBuilder();

        sb.append("Application version: " + getApplicationVersionString(context) + "\n");
        sb.append("Device locale: " + Locale.getDefault().toString() + "\n\n");
        sb.append("Android ID: " + getAndroidId(context, "n/a"));

        // phone information
        sb.append("PHONE SPECS\n");
        sb.append("model: " + Build.MODEL + "\n");
        sb.append("brand: " + Build.BRAND + "\n");
        sb.append("product: " + Build.PRODUCT + "\n");
        sb.append("device: " + Build.DEVICE + "\n\n");

        // android information
        sb.append("PLATFORM INFO\n");
        sb.append("Android " + Build.VERSION.RELEASE + " " + Build.ID + " (build "
                + Build.VERSION.INCREMENTAL + ")\n");
        sb.append("build tags: " + Build.TAGS + "\n");
        sb.append("build type: " + Build.TYPE + "\n\n");

        // settings
        sb.append("SYSTEM SETTINGS\n");
        String networkMode = null;
        ContentResolver resolver = context.getContentResolver();
        try {
            if (Secure.getInt(resolver, Secure.WIFI_ON) == 0) {
                networkMode = "DATA";
            } else {
                networkMode = "WIFI";
            }
            sb.append("network mode: " + networkMode + "\n");
            sb.append("HTTP proxy: "
                    + Secure.getString(resolver, Secure.HTTP_PROXY) + "\n\n");
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }

        if(error != null) {
	        sb.append("STACK TRACE FOLLOWS\n\n");
	
	        StringWriter stackTrace = new StringWriter();
	        error.printStackTrace(new PrintWriter(stackTrace));
	
	        sb.append(stackTrace.toString());
        }
        
        return sb.toString();
    }
    
    public static String getApplicationName(Context context) {
    	final PackageManager pm = context.getPackageManager();
    	ApplicationInfo ai;
    	try {
    	    ai = pm.getApplicationInfo(getApplicationPackage(context), 0);
    	} catch (final NameNotFoundException e) {
    	    ai = null;
    	}
    	return  (String) (ai != null ? pm.getApplicationLabel(ai) : "this app");
    }

    public static String getApplicationPackage(Context context) {
    	return context.getPackageName();
    }
    
    public static boolean isDebugOn(Context c) {
    	ApplicationInfo appInfo = c.getApplicationInfo();
    	if((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }
    
    public static void strictModeOn(Boolean isOn) {
    	//Strict mode is only available of Android 2.3+ (I.e gingerbread)
    	if(isOn && (ANDROID_API_LEVEL > Build.VERSION_CODES.HONEYCOMB)) {
	    	//Set strict mode
	        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	                //.detectDiskReads()
	                .detectDiskWrites()
	                .detectNetwork()
	                .penaltyLog()
	                .build());
	
	        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	                .detectActivityLeaks()
	                .detectLeakedClosableObjects()
	                .detectLeakedSqlLiteObjects()
	                .penaltyLog()
	                .build());
    	}
    }
}

package com.DGSD.Teexter.Utils;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;

public class CopyUtils {

	public static void copyText(Context c, String label, String text) {
		ClipboardManager cm = (ClipboardManager) c
				.getSystemService(Context.CLIPBOARD_SERVICE);
		cm.setPrimaryClip(ClipData.newPlainText(label, text));
	}

	public static String getCurrentClipboardString(Context c) {
		String result = null;
		
		ClipboardManager cm = (ClipboardManager) c
				.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData data = cm.getPrimaryClip();
		if (data != null && data.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
			if (data != null && data.getItemCount() > 0 & data.getItemAt(0) != null) {
				result = data.getItemAt(0).getText().toString();
			}
		}

		return result;
	}
}

package com.DGSD.Teexter.Utils;

import java.io.InputStream;

import com.DGSD.Teexter.BuildConfig;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class ContactUtils {
	private static final String TAG = ContactUtils.class.getSimpleName();

	public static Contact getContactFromPhone(Context context, String phone) {
		Cursor cursor = null;
		try {
			ContentResolver cr = context.getContentResolver();
			cursor = cr.query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)), new String[] {
					PhoneLookup.LOOKUP_KEY, PhoneLookup.DISPLAY_NAME
			}, null, null, null);

			if (cursor != null && cursor.moveToFirst()) {
				return new Contact(cursor.getString(0), cursor.getString(1));
			} else {
				return null;
			}

		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error getting contact from phone: " + phone, e);
			}

			return null;
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	public static Bitmap getContactPhoto(ContentResolver cr, String lookup) {
		try {
			Uri.Builder builder = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon();
			builder.appendPath(lookup);

			InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, builder.build());
			if (input == null) {
				return null;
			}
			return BitmapFactory.decodeStream(input);
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error getting contact photo: " + lookup, e);
			}
			return null;
		}
	}

	public static class Contact implements Parcelable {
		public String lookupId;
		public String name;

		public Contact(String lookupId, String name) {
			this.lookupId = lookupId;
			this.name = name;
		}
		
		private Contact(Parcel in) {
			lookupId = in.readString();
			name = in.readString();
		}
		
		@Override
		public int describeContents() {
			return this.hashCode();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(lookupId);
			dest.writeString(name);
		}
		
		public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
			public Contact createFromParcel(Parcel in) {
				return new Contact(in);
			}

			public Contact[] newArray(int size) {
				return new Contact[size];
			}
		};
	}
}

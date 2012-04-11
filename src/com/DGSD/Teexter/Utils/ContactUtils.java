package com.DGSD.Teexter.Utils;

import java.io.InputStream;

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
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;

public class ContactUtils {
	private static final String TAG = ContactUtils.class.getSimpleName();

	public static Contact getContactFromPhone(Context context, String phone) {
		Cursor cursor = null;
		try {
			ContentResolver cr = context.getContentResolver();
			cursor = cr.query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)),
					new String[] {
							PhoneLookup.LOOKUP_KEY, PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_URI
					}, null, null, null);

			if (cursor != null && cursor.moveToFirst()) {
				String lkpKey = cursor.getString(0);

				Uri photoUri = UriUtils.parseUriOrNull(cursor.getString(2));

				return new Contact(lkpKey, cursor.getString(1), phone, photoUri == null ? null : photoUri.toString());
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
		public String phone;
		public String photoUri;

		public Contact(String lookupId, String name, String phone, String photoUri) {
			this.lookupId = lookupId;
			this.name = name;
			this.phone = phone;
			this.photoUri = photoUri;
		}

		private Contact(Parcel in) {
			lookupId = in.readString();
			name = in.readString();
			phone = in.readString();
			photoUri = in.readString();
		}

		@Override
		public int describeContents() {
			return this.hashCode();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(lookupId);
			dest.writeString(name);
			dest.writeString(phone);
			dest.writeString(photoUri);
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

	/**
	 * Fomat the name and number.
	 * 
	 * @param name
	 * @param number
	 * @param numberE164
	 *            the number's E.164 representation, is used to get the country
	 *            the number belongs to.
	 * @return the formatted name and number
	 */
	public static String formatNameAndNumber(String name, String number) {
		// Format like this: Mike Cleron <(650) 555-1234>
		// Erick Tseng <(650) 555-1212>
		// Tutankhamun <tutank1341@gmail.com>
		// (408) 555-1289
		String formattedNumber = number;

		if (number.matches(android.util.Patterns.EMAIL_ADDRESS.pattern())) {
			formattedNumber = PhoneNumberUtils.formatNumber(number);
		}

		if (!TextUtils.isEmpty(name) && !name.equals(number)) {
			return name + " <" + formattedNumber + ">";
		} else {
			return formattedNumber;
		}
	}
}

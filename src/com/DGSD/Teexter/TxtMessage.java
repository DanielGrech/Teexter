package com.DGSD.Teexter;

import android.os.Parcel;
import android.os.Parcelable;

import com.DGSD.Teexter.Utils.ContactUtils.Contact;

public class TxtMessage implements Parcelable {
	private String mSender;
	private String mMessage;
	private long mTimestamp;
	private Contact mContact;
	private boolean mIsFavourite;

	public TxtMessage(String sender, String msg, long timestamp, Contact contact, boolean isFavourite) {
		mSender = sender;
		mMessage = msg;
		mTimestamp = timestamp;
		mContact = contact;
		mIsFavourite = isFavourite;
	}
	
	private TxtMessage(Parcel in) {
		mSender = in.readString();
		mMessage = in.readString();
		mTimestamp = in.readLong();
		mIsFavourite = in.readInt() == 1;
		mContact = in.readParcelable(null);
	}

	public String getSender() {
		return mSender;
	}

	public void setSender(String mSender) {
		this.mSender = mSender;
	}

	public String getMessage() {
		return mMessage;
	}

	public void setMessage(String mMessage) {
		this.mMessage = mMessage;
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public void setTimestamp(long mTimestamp) {
		this.mTimestamp = mTimestamp;
	}
	
	public Contact getContact() {
		return mContact;
	}

	public void setContact(Contact contact) {
		this.mContact = contact;
	}
	
	public boolean isFavourite() {
		return mIsFavourite;
	}

	public void setIsFavourite(boolean isFavourite) {
		this.mIsFavourite = isFavourite;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("[").append(mTimestamp).append(" ").append(mSender).append(": ")
				.append(mMessage).append("]").toString();
	}
	
	@Override
	public int describeContents() {
		return this.hashCode();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mSender);
		dest.writeString(mMessage);
		dest.writeLong(mTimestamp);
		dest.writeInt(mIsFavourite ? 1 : 0);
		if(mContact != null) {
			dest.writeParcelable(mContact, 0);
		}
	}
	
	public static final Parcelable.Creator<TxtMessage> CREATOR = new Parcelable.Creator<TxtMessage>() {
		public TxtMessage createFromParcel(Parcel in) {
			return new TxtMessage(in);
		}

		public TxtMessage[] newArray(int size) {
			return new TxtMessage[size];
		}
	};
}

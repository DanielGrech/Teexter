package com.DGSD.Teexter.Data;

public class DbField {

	public static final DbField ID = new DbField("_id", "integer", "primary key");
	public static final DbField TIME = new DbField("_time", "timestamp");
	public static final DbField CONTACT_LOOKUP_ID = new DbField("_contact_lookup", "text");
	public static final DbField PHOTO_URI = new DbField("_photo_uri", "text");
	public static final DbField DISPLAY_NAME = new DbField("_display_name", "text");
	public static final DbField NUMBER = new DbField("_number", "text");
	public static final DbField MESSAGE = new DbField("_message", "text");
	public static final DbField FAVOURITE = new DbField("_favourite", "integer", "DEFAULT 0");
	public static final DbField SENT_MESSAGE_ID = new DbField("_sent_message_id", "integer");
	public static final DbField READ = new DbField("_read", "integer");
	
	private String name;
	private String type;
	private String constraint;

	public DbField(String n, String t, String c) {
		name = n;
		type = t;
		constraint = c;
	}

	public DbField(String n, String t) {
		this(n, t, null);
	}

	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getConstraint() {
		return constraint;
	}

	public void setConstraint(String constraint) {
		this.constraint = constraint;
	}
}

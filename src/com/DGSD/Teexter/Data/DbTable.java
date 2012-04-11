package com.DGSD.Teexter.Data;

public class DbTable {

	public static final DbTable INBOX = new DbTable("_inbox", new DbField[] {
		DbField.ID, 
		DbField.TIME, // Timestamp in millis
		DbField.CONTACT_LOOKUP_ID, // Contact lookup
		DbField.PHOTO_URI,
		DbField.DISPLAY_NAME, // The contact display name (or number) associated with this msg
		DbField.NUMBER,
		DbField.MESSAGE, // Message contents
		DbField.FAVOURITE, // This message is marked as a favourite
		DbField.READ,
		DbField.THREAD_COUNT
	});

	public static final DbTable SENT = new DbTable("_sent", new DbField[] {
		DbField.ID, 
		DbField.TIME, // Timestamp in millis
		DbField.MESSAGE, // Message contents
		DbField.IN_REPLY_TO_ID,
		DbField.NUMBER,
		DbField.CONTACT_LOOKUP_ID, 
		DbField.DISPLAY_NAME,
		DbField.PHOTO_URI,
		DbField.IS_DRAFT,
	});
	
	private String name;
	private DbField[] fields;

	public DbTable(String n, DbField[] f) {
		name = n;
		fields = f;
	}

	public String getName() {
		return name;
	}

	public DbField[] getFields() {
		return fields;
	}

	@Override
	public String toString() {
		return name;
	}

	public String[] getFieldNames() {
		String[] results = new String[fields.length];

		for (int i = 0, size = fields.length; i < size; i++) {
			results[i] = fields[i].getName();
		}

		return results;
	}

	public String dropSql() {
		return new StringBuilder().append("DROP TABLE ").append(name).toString();
	}

	public String createSql() {
		StringBuilder builder = new StringBuilder().append("CREATE TABLE ").append(name).append(" ").append("(");

		// Ensure that a comma does not appear on the last iteration
		String comma = "";
		DbField[] fields = getFields();
		for (DbField field : fields) {
			builder.append(comma);
			comma = ",";

			builder.append(field.getName());
			builder.append(" ");
			builder.append(field.getType());
			builder.append(" ");

			if (field.getConstraint() != null) {
				builder.append(field.getConstraint());
			}
		}

		builder.append(")");

		return builder.toString();

	}
}

package com.DGSD.Teexter.Data.Provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;
import com.DGSD.Teexter.Data.DbField;
import com.DGSD.Teexter.Data.DbTable;
import com.DGSD.Teexter.Data.TeexterDb;

public class MessagesProvider extends ContentProvider {
	private static final String TAG = MessagesProvider.class.getSimpleName();

	private static final String AUTHORITY = "com.DGSD.Teexter.Data.Provider.MessagesProvider";
	private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

	protected static final UriMatcher mURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int INBOX = 0x1;
	private static final int SENT = 0x2;
	private static final int FAVOURITES = 0x3;
	private static final int RECIPIENTS = 0x4;
	private static final int SEARCH_MANAGER = 0x5;

	public static final Uri INBOX_URI = Uri.withAppendedPath(BASE_URI, "inbox");
	public static final Uri SENT_URI = Uri.withAppendedPath(BASE_URI, "sent");
	public static final Uri FAVOURITES_URI = Uri.withAppendedPath(BASE_URI, "favourites");
	public static final Uri RECIPIENTS_URI = Uri.withAppendedPath(BASE_URI, "recipients");
	

	private TeexterDb mDb;

	static {
		mURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_MANAGER);
		mURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_MANAGER);
		mURIMatcher.addURI(AUTHORITY, "inbox", INBOX);
		mURIMatcher.addURI(AUTHORITY, "sent", SENT);
		mURIMatcher.addURI(AUTHORITY, "favourites", FAVOURITES);
		mURIMatcher.addURI(AUTHORITY, "recipients", RECIPIENTS);
	}

	@Override
	public boolean onCreate() {
		mDb = new TeexterDb(getContext());
		return false;
	}

	@Override
	public String getType(Uri uri) {
		if (mURIMatcher.match(uri) != UriMatcher.NO_MATCH) {
			return uri.toString();
		} else {
			return null;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] proj, String sel, String[] selArgs, String sort) {
		try {
			int type = mURIMatcher.match(uri);
			if (type == UriMatcher.NO_MATCH) {
				if (BuildConfig.DEBUG) {
					Log.w(TAG, "No match for URI: " + uri);
				}

				return null;
			}

			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			switch (type) {
				case SEARCH_MANAGER:
					String searchTerm = uri.getLastPathSegment();
					
					StringBuilder rawQuery = new StringBuilder();
					rawQuery.append("SELECT ")
							.append(DbField.ID).append(", ")
							.append(DbField.MESSAGE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1).append(",")
							.append(DbField.DISPLAY_NAME + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2).append(",")
							.append(DbField.MESSAGE + " AS " + SearchManager.SUGGEST_COLUMN_QUERY).append(",")
							.append(DbField.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA)
							.append(" FROM ").append(DbTable.INBOX)
							.append(" WHERE ").append(DbField.DISPLAY_NAME).append(" LIKE '%" + searchTerm + "%'")
							.append(" OR ").append(DbField.MESSAGE).append(" LIKE '%" + searchTerm + "%'");
					
					rawQuery.append(" UNION SELECT ")
							.append(DbField.ID).append(", ")
							.append(DbField.MESSAGE + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1).append(",")
							.append("'' AS " + SearchManager.SUGGEST_COLUMN_TEXT_2).append(",")
							.append(DbField.MESSAGE + " AS " + SearchManager.SUGGEST_COLUMN_QUERY).append(",")
							.append(DbField.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA)
							.append(" FROM ").append(DbTable.SENT)
							.append(" WHERE ").append(DbField.MESSAGE).append(" LIKE '%" + searchTerm + "%'");
					
					Cursor cursor = mDb.getReadableDatabase().rawQuery(rawQuery.toString(), null);
					
					cursor.setNotificationUri(getContext().getContentResolver(), uri);

					return cursor;
				case INBOX:
					qb.setTables(DbTable.INBOX.getName());
					break;
				case SENT:
					qb.setTables(DbTable.SENT.getName());
					break;
				case FAVOURITES:
					qb.setTables(DbTable.INBOX.getName());
					qb.appendWhere(DbField.FAVOURITE + "=1");
					break;
				case RECIPIENTS:
					qb.setTables(DbTable.RECIPIENTS.getName());
					break;
			}
			
			Cursor cursor = qb.query(mDb.getReadableDatabase(), proj, sel, selArgs, null, null, sort);
			cursor.setNotificationUri(getContext().getContentResolver(), uri);

			return cursor;
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error querying data", e);
			}

			return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		try {
			int type = mURIMatcher.match(uri);
			if (type == UriMatcher.NO_MATCH || type == FAVOURITES) {
				if (BuildConfig.DEBUG) {
					Log.w(TAG, "No match for URI: " + uri);
				}

				return null;
			}

			SQLiteDatabase db = mDb.getWritableDatabase();
			long id = -1;
			switch (type) {
				case INBOX:
					id = db.insertOrThrow(DbTable.INBOX.getName(), null, values);
					break;
				case SENT:
					id = db.insertOrThrow(DbTable.SENT.getName(), null, values);
					break;
				case RECIPIENTS:
					id = db.insertOrThrow(DbTable.RECIPIENTS.getName(), null, values);
					break;
			}

			if (id > 0) {
				Uri newUri = ContentUris.withAppendedId(uri, id);
				getContext().getContentResolver().notifyChange(uri, null);
				return newUri;
			} else {
				throw new SQLException("Failed to insert row into " + uri);
			}

		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error inserting data", e);
			}

			return null;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		try {
			int type = mURIMatcher.match(uri);
			if (type == UriMatcher.NO_MATCH) {
				if (BuildConfig.DEBUG) {
					Log.w(TAG, "No match for URI: " + uri);
				}

				return 0;
			}

			SQLiteDatabase db = mDb.getWritableDatabase();

			String table = null;
			switch (type) {
				case INBOX:
					table = DbTable.INBOX.getName();
					break;
				case SENT:
					table = DbTable.SENT.getName();
					break;
				case FAVOURITES:
					table = DbTable.INBOX.getName();
					break;
				case RECIPIENTS:
					table = DbTable.RECIPIENTS.getName();
					break;
			}

			int rowsAffected = db.update(table, values, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return rowsAffected;
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error updating data", e);
			}

			return 0;
		}
	}

	@Override
	public int delete(Uri uri, String sel, String[] selArgs) {
		try {
			int type = mURIMatcher.match(uri);
			if (type == UriMatcher.NO_MATCH || type == FAVOURITES) {
				if (BuildConfig.DEBUG) {
					Log.w(TAG, "No match for URI: " + uri);
				}

				return 0;
			}

			SQLiteDatabase db = mDb.getWritableDatabase();

			String table = null;
			switch (type) {
				case INBOX:
					table = DbTable.INBOX.getName();
					break;
				case SENT:
					table = DbTable.SENT.getName();
					break;
				case FAVOURITES:
					table = DbTable.INBOX.getName();
					break;
				case RECIPIENTS:
					table = DbTable.RECIPIENTS.getName();
					break;
			}

			String id = uri.getLastPathSegment();
			int rowsAffected = 0;
			if (TextUtils.isEmpty(id)) {
				rowsAffected = db.delete(table, new StringBuilder().append(DbField.ID).append("=").append(id)
						.toString(), null);
			} else {
				rowsAffected = db.delete(table, new StringBuilder().append(sel)
						.append(" and ").append(DbField.ID).append("=").append(id).toString(), selArgs);
			}
			
			getContext().getContentResolver().notifyChange(uri, null);
			return rowsAffected;
		} catch (Exception e) {
			if (BuildConfig.DEBUG) {
				Log.e(TAG, "Error deleting data", e);
			}

			return 0;
		}
	}
}

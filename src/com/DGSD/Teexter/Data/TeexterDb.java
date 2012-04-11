package com.DGSD.Teexter.Data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.DGSD.Teexter.BuildConfig;

public class TeexterDb extends SQLiteOpenHelper {
	private static final String TAG = TeexterDb.class.getSimpleName();
	private static final int VERSION = 1;
	public static final String DATABASE_NAME = "teexter.db";
	
	public TeexterDb(Context context) {
		super(context, DATABASE_NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		if(BuildConfig.DEBUG) {
			Log.v(TAG, "Creating database");
		}
		
		db.execSQL(DbTable.INBOX.createSql());
		db.execSQL(DbTable.SENT.createSql());
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(DbTable.INBOX.dropSql());
		db.execSQL(DbTable.SENT.dropSql());
		
		this.onCreate(db);
	}
}

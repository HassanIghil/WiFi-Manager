package com.example.wifimanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashSet;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "wifi_manager.db";
    private static final int DATABASE_VERSION = 3;

    // Table Names
    public static final String TABLE_WIFI_SETTINGS = "wifi_settings";
    public static final String TABLE_NETWORK_SETTINGS = "network_settings";
    public static final String TABLE_SECURITY = "security";
    public static final String TABLE_GUEST_WIFI = "guest_wifi";
    public static final String TABLE_QOS = "qos";
    public static final String TABLE_SCHEDULES = "schedules";
    public static final String TABLE_PREFERENCES = "preferences";
    public static final String TABLE_DEVICE_SETTINGS = "device_settings";
    public static final String TABLE_NOTIFICATIONS = "notifications";

    // Common Column Names
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";

    // Create table statements
    private static final String CREATE_TABLE_TEMPLATE =
            "CREATE TABLE %s (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_KEY + " TEXT UNIQUE, " +
            COLUMN_VALUE + " TEXT)";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private boolean isTableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName}
        );
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Create all tables
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_WIFI_SETTINGS));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_NETWORK_SETTINGS));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_SECURITY));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_GUEST_WIFI));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_QOS));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_SCHEDULES));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_PREFERENCES));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_DEVICE_SETTINGS));
            db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_NOTIFICATIONS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            // Create any missing tables instead of dropping everything
            if (!isTableExists(db, TABLE_NOTIFICATIONS)) {
                db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_NOTIFICATIONS));
            }
            if (!isTableExists(db, TABLE_DEVICE_SETTINGS)) {
                db.execSQL(String.format(CREATE_TABLE_TEMPLATE, TABLE_DEVICE_SETTINGS));
            }
            // Add more table checks as needed
            
            // You can add specific version upgrade logic here if needed
            if (oldVersion < 2) {
                // Version 1 to 2 specific upgrades
            }
            if (oldVersion < 3) {
                // Version 2 to 3 specific upgrades
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putString(String table, String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY, key);
        values.put(COLUMN_VALUE, value);
        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void putBoolean(String table, String key, boolean value) {
        putString(table, key, String.valueOf(value));
    }

    public void putLong(String table, String key, long value) {
        putString(table, key, String.valueOf(value));
    }

    public void putInt(String table, String key, int value) {
        putString(table, key, String.valueOf(value));
    }

    public String getString(String table, String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_VALUE};
        String selection = COLUMN_KEY + " = ?";
        String[] selectionArgs = {key};
        
        try (Cursor cursor = db.query(table, columns, selection, selectionArgs, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VALUE));
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String table, String key, boolean defaultValue) {
        String value = getString(table, key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    public long getLong(String table, String key, long defaultValue) {
        String value = getString(table, key, String.valueOf(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt(String table, String key, int defaultValue) {
        String value = getString(table, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean containsKey(String table, String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_KEY};
        String selection = COLUMN_KEY + " = ?";
        String[] selectionArgs = {key};
        
        try (Cursor cursor = db.query(table, columns, selection, selectionArgs, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    public void clearTable(String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(table, null, null);
    }

    public Set<String> getAllKeys(String table) {
        Set<String> keys = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_KEY};
        
        try (Cursor cursor = db.query(table, columns, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    keys.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_KEY)));
                } while (cursor.moveToNext());
            }
        }
        return keys;
    }
}
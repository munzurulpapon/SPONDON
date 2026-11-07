
package com.example.spondon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Spondon.db";
    private static final int DATABASE_VERSION = 5;

    public static final String TABLE_REPORTS = "reports";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String query = "CREATE TABLE " + TABLE_REPORTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "category TEXT, "
                + "title TEXT, "
                + "description TEXT, "
                + "location TEXT, "
                + "status TEXT, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + "image_uri TEXT, "
                + "firebase_id TEXT, "
                + "latitude REAL, "
                + "longitude REAL"
                + ")";

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) {
            db.execSQL(
                    "ALTER TABLE reports ADD COLUMN created_at DATETIME"
            );

            db.execSQL(
                    "UPDATE reports SET created_at = CURRENT_TIMESTAMP "
                            + "WHERE created_at IS NULL"
            );
        }

        if (oldVersion < 3) {
            db.execSQL(
                    "ALTER TABLE reports ADD COLUMN image_uri TEXT"
            );
        }

        if (oldVersion < 4) {
            db.execSQL(
                    "ALTER TABLE reports ADD COLUMN firebase_id TEXT"
            );
        }

        if (oldVersion < 5) {
            db.execSQL(
                    "ALTER TABLE reports ADD COLUMN latitude REAL"
            );

            db.execSQL(
                    "ALTER TABLE reports ADD COLUMN longitude REAL"
            );
        }
    }
}

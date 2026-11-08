package com.example.spondon;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Spondon.db";
    private static final int DATABASE_VERSION = 8;

    public static final String TABLE_REPORTS = "reports";
    public static final String TABLE_FAVOURITES = "tourism_favourites";
    public static final String TABLE_JOB_APPLICATIONS = "job_applications";
    public static final String TABLE_SERVICE_REQUESTS = "service_requests";
    public static final String TABLE_BLOOD_DONORS = "blood_donors";
    public static final String TABLE_EMERGENCY_REQUESTS = "emergency_requests";
    public static final String TABLE_EMERGENCY_CONTACTS = "emergency_contacts";
    public static final String TABLE_COMMUNITY_POSTS = "community_posts";
    public static final String TABLE_COMMUNITY_COMMENTS = "community_comments";
    public static final String TABLE_NOTIFICATIONS = "notifications";

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
                + "longitude REAL, "
                + "uid TEXT"
                + ")";

        db.execSQL(query);

        createAdditionalTables(db);
    }

    private void createAdditionalTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVOURITES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "item_id TEXT UNIQUE, "
                + "title TEXT, "
                + "category TEXT, "
                + "location TEXT, "
                + "description TEXT, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_JOB_APPLICATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "job_id TEXT, "
                + "job_title TEXT, "
                + "company TEXT, "
                + "applicant_name TEXT, "
                + "applicant_phone TEXT, "
                + "applicant_email TEXT, "
                + "qualifications TEXT, "
                + "cover_note TEXT, "
                + "status TEXT, "
                + "applied_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SERVICE_REQUESTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "firebase_id TEXT, "
                + "service_name TEXT, "
                + "category TEXT, "
                + "address TEXT, "
                + "phone TEXT, "
                + "preferred_date TEXT, "
                + "preferred_time TEXT, "
                + "notes TEXT, "
                + "status TEXT, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BLOOD_DONORS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "blood_group TEXT, "
                + "phone TEXT, "
                + "area TEXT, "
                + "is_available INTEGER DEFAULT 1, "
                + "registered_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EMERGENCY_REQUESTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "firebase_id TEXT, "
                + "uid TEXT, "
                + "name TEXT, "
                + "phone TEXT, "
                + "type TEXT, "
                + "location TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "description TEXT, "
                + "status TEXT, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EMERGENCY_CONTACTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "phone TEXT, "
                + "relationship TEXT, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COMMUNITY_POSTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "content TEXT, "
                + "author TEXT, "
                + "category TEXT, "
                + "likes INTEGER DEFAULT 0, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COMMUNITY_COMMENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "post_id INTEGER, "
                + "author TEXT, "
                + "comment TEXT, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "message TEXT, "
                + "category TEXT, "
                + "is_read INTEGER DEFAULT 0, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
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

        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE reports ADD COLUMN uid TEXT");
            } catch (Exception ignored) {
            }
            createAdditionalTables(db);
        }

        if (oldVersion < 7) {
            createAdditionalTables(db);
        }

        if (oldVersion < 8) {
            createAdditionalTables(db);
        }
    }
}

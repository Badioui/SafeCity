package com.example.safecity.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "safecity.db";
    public static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Active les clés étrangères !
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // TABLE ROLES
        db.execSQL("CREATE TABLE roles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE" +
                ");");

        // TABLE USERS
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "role_id INTEGER NOT NULL, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(role_id) REFERENCES roles(id) ON DELETE CASCADE" +
                ");");

        // TABLE CATEGORIES
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE" +
                ");");

        // TABLE INCIDENTS
        db.execSQL("CREATE TABLE incidents (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "category_id INTEGER , " +
                "description TEXT, " +
                "photo_url TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "status TEXT DEFAULT 'Nouveau', " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL" +
                ");");

        // TABLE NOTIFICATIONS
        db.execSQL("CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "incident_id INTEGER NOT NULL, " +
                "message TEXT NOT NULL, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(incident_id) REFERENCES incidents(id) ON DELETE CASCADE" +
                ");");

        // Insérer roles par défaut
        db.execSQL("INSERT INTO roles (name) VALUES ('Admin'), ('Citoyen'), ('Autorité');");

        // Insérer catégories par défaut
        db.execSQL("INSERT INTO categories (name) VALUES " +
                "('Vol'), ('Accident'), ('Incendie'), ('Panne'), ('Autre');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Suppression + recréation
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS incidents");
        db.execSQL("DROP TABLE IF EXISTS categories");
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS roles");
        onCreate(db);
    }
}

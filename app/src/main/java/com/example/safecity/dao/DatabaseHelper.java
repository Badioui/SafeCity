package com.example.safecity.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * DatabaseHelper
 *
 * Gère la création, la mise à jour et l’intégrité de la base de données locale SafeCity.
 * Respecte le Cahier des Charges : intégrité référentielle, rôles, incidents, notifications.
 *
 * Tables :
 *  - roles
 *  - users
 *  - categories
 *  - incidents
 *  - notifications
 *
 * Auteur : Asmaa
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // --- Constantes de base ---
    public static final String DB_NAME = "safecity.db";
    public static final int DB_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Active les contraintes de clés étrangères.
     * Assure la cohérence (onDelete CASCADE, SET NULL).
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Création complète du schéma SafeCity.
     * Respect des dépendances logiques et fonctionnelles.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // === TABLE ROLES ===
            db.execSQL("CREATE TABLE roles (" +
                    "id_role INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nom_role TEXT NOT NULL UNIQUE" +
                    ");");

            // === TABLE USERS ===
            db.execSQL("CREATE TABLE users (" +
                    "id_utilisateur INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nom TEXT NOT NULL, " +
                    "email TEXT NOT NULL UNIQUE, " +
                    "mot_de_passe_hash TEXT NOT NULL, " +
                    "id_role INTEGER NOT NULL, " +
                    "date_creation TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(id_role) REFERENCES roles(id_role) ON DELETE CASCADE" +
                    ");");

            // === TABLE CATEGORIES ===
            db.execSQL("CREATE TABLE categories (" +
                    "id_categorie INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nom_categorie TEXT NOT NULL UNIQUE" +
                    ");");

            // === TABLE INCIDENTS ===
            db.execSQL("CREATE TABLE incidents (" +
                    "id_incident INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_utilisateur INTEGER NOT NULL, " +
                    "id_categorie INTEGER, " +
                    "description TEXT, " +
                    "photo_url TEXT, " +
                    "latitude REAL, " +
                    "longitude REAL, " +
                    "statut TEXT DEFAULT 'Nouveau' CHECK(statut IN ('Nouveau','En cours','Traité')), " +
                    "date_signalement TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(id_utilisateur) REFERENCES users(id_utilisateur) ON DELETE CASCADE, " +
                    "FOREIGN KEY(id_categorie) REFERENCES categories(id_categorie) ON DELETE SET NULL" +
                    ");");

            // === TABLE NOTIFICATIONS ===
            db.execSQL("CREATE TABLE notifications (" +
                    "id_notification INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "id_utilisateur INTEGER, " +
                    "id_incident INTEGER, " +
                    "titre TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "zone_ciblee TEXT, " +
                    "is_read INTEGER DEFAULT 0 CHECK(is_read IN (0,1)), " +
                    "date_envoi TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(id_utilisateur) REFERENCES users(id_utilisateur) ON DELETE CASCADE, " +
                    "FOREIGN KEY(id_incident) REFERENCES incidents(id_incident) ON DELETE SET NULL" +
                    ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_incidents_geo ON incidents(latitude, longitude);");


            // === INSERTIONS INITIALES ===
            db.execSQL("INSERT INTO roles (nom_role) VALUES " +
                    "('Admin'), ('Autorité'), ('Citoyen');");

            db.execSQL("INSERT INTO categories (nom_categorie) VALUES " +
                    "('Vol'), ('Accident'), ('Incendie'), ('Panne'), ('Autre');");

            Log.i("DatabaseHelper", "Base de données créée et initialisée avec succès.");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Erreur lors de la création de la base : " + e.getMessage());
        }
    }

    /**
     * Gestion de la mise à jour (migration) du schéma.
     * Supprime proprement les tables dans l’ordre inverse des dépendances.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseHelper", "Mise à niveau de la base de données : version " + oldVersion + " → " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS incidents");
        db.execSQL("DROP TABLE IF EXISTS categories");
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS roles");
        onCreate(db);
    }

    /**
     * Supprime toutes les données utilisateur (réinitialisation totale, usage admin uniquement).
     */
    public void clearAllData(SQLiteDatabase db) {
        db.execSQL("DELETE FROM notifications");
        db.execSQL("DELETE FROM incidents");
        db.execSQL("DELETE FROM users");
        db.execSQL("DELETE FROM categories");
    }
}

package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.safecity.model.Categorie;
import com.example.safecity.model.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * ReferenceDAO
 *
 * Gère le CRUD (principalement READ) pour les tables de référence :
 * - Rôles (roles)
 * - Catégories (categories)
 *
 * Ces tables sont généralement statiques et initialisées à la création de la BDD.
 * Auteur : Asmaa
 */
public class ReferenceDAO {

    private static final String TAG = "ReferenceDAO";

    private final DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public ReferenceDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // --- Utility : DB Management ---
    public void open() {
        // Lecture seule (ou écriture si nécessaire)
        if (db == null || !db.isOpen()) {
            db = dbHelper.getReadableDatabase();
        }
    }

    public void close() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        dbHelper.close();
    }

    private void ensureDb() {
        if (db == null || !db.isOpen()) open();
    }

    // =========================================================
    // CRUD : RÔLES (ROLES)
    // =========================================================

    /**
     * READ: Récupère tous les rôles.
     */
    public List<Role> getAllRoles() {
        ensureDb();
        List<Role> roles = new ArrayList<>();
        // Tri par ID pour une liste stable
        try (Cursor cursor = db.query("roles", null, null, null, null, null, "id_role ASC")) {
            while (cursor.moveToNext()) {
                roles.add(cursorToRole(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllRoles : " + e.getMessage());
        }
        return roles;
    }

    /**
     * READ: Récupère un rôle par son ID.
     */
    public Role getRoleById(long id) {
        ensureDb();
        Role role = null;
        try (Cursor cursor = db.query("roles", null, "id_role = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                role = cursorToRole(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getRoleById : " + e.getMessage());
        }
        return role;
    }

    /**
     * CREATE: Insère un rôle (Utilisé pour l'initialisation de la BDD).
     */
    public long insertRole(Role role) {
        ensureDb();
        if (role == null) return -1;

        ContentValues values = new ContentValues();
        values.put("nom_role", role.getNomRole());

        long result = -1;
        db.beginTransaction();
        try {
            // L'ID est généré automatiquement par la BDD si on ne le fournit pas.
            result = db.insert("roles", null, values);
            if (result != -1) db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "insertRole : " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return result;
    }


    // =========================================================
    // CRUD : CATÉGORIES (CATEGORIES)
    // =========================================================

    /**
     * READ: Récupère toutes les catégories.
     */
    public List<Categorie> getAllCategories() {
        ensureDb();
        List<Categorie> categories = new ArrayList<>();
        // Tri par nom pour affichage
        try (Cursor cursor = db.query("categories", null, null, null, null, null, "nom_categorie ASC")) {
            while (cursor.moveToNext()) {
                categories.add(cursorToCategorie(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllCategories : " + e.getMessage());
        }
        return categories;
    }

    /**
     * READ: Récupère une catégorie par son ID.
     */
    public Categorie getCategoryById(long id) {
        ensureDb();
        Categorie categorie = null;
        try (Cursor cursor = db.query("categories", null, "id_categorie = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                categorie = cursorToCategorie(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getCategoryById : " + e.getMessage());
        }
        return categorie;
    }

    /**
     * CREATE: Insère une catégorie (Utilisé pour l'initialisation de la BDD).
     */
    public long insertCategorie(Categorie categorie) {
        ensureDb();
        if (categorie == null) return -1;

        ContentValues values = new ContentValues();
        values.put("nom_categorie", categorie.getNomCategorie());

        long result = -1;
        db.beginTransaction();
        try {
            result = db.insert("categories", null, values);
            if (result != -1) db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "insertCategorie : " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return result;
    }


    // =========================================================
    // Conversion Cursor
    // =========================================================

    private Role cursorToRole(Cursor cursor) {
        Role role = new Role();
        role.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id_role")));
        role.setNomRole(cursor.getString(cursor.getColumnIndexOrThrow("nom_role")));
        return role;
    }

    private Categorie cursorToCategorie(Cursor cursor) {
        Categorie categorie = new Categorie();
        categorie.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id_categorie")));
        categorie.setNomCategorie(cursor.getString(cursor.getColumnIndexOrThrow("nom_categorie")));
        return categorie;
    }
}
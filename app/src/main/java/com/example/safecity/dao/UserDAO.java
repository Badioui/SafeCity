package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.safecity.model.Utilisateur;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO
 *
 * Gère les opérations CRUD et l’authentification des utilisateurs.
 * Conforme au Cahier des Charges SafeCity :
 *  - Authentification sécurisée (hash SHA-256)
 *  - Gestion des rôles
 *  - Intégrité référentielle (id_role FK)
 *
 * Auteur : Asmaa
 */
public class UserDAO {

    private static final String TAG = "UserDAO";

    private final DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public UserDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // --- Ouvrir / Fermer la base ---
    public void open() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
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
    // 🔐 Hashage du mot de passe (SHA-256)
    // =========================================================
    private String hashPassword(String password) {
        if (password == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Erreur de hashage : " + e.getMessage());
            return null;
        }
    }

    // =========================================================
    // ✅ CREATE : Ajouter un utilisateur
    // =========================================================
    public long insertUser(Utilisateur user, String plainPassword) {
        ensureDb();
        if (user == null || plainPassword == null) return -1;

        if (emailExists(user.getEmail())) {
            Log.w(TAG, "insertUser : email déjà utilisé -> " + user.getEmail());
            return -2;
        }

        String hashed = hashPassword(plainPassword);
        if (hashed == null) return -1;

        long result = -1;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("nom", user.getNom());
            values.put("email", user.getEmail().trim().toLowerCase());
            values.put("mot_de_passe_hash", hashed);
            values.put("id_role", user.getIdRole());

            result = db.insert("users", null, values);
            if (result != -1) {
                db.setTransactionSuccessful();
                Log.i(TAG, "Utilisateur ajouté (id=" + result + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "insertUser : " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return result;
    }

    // =========================================================
    // ✅ READ : Récupérer un utilisateur
    // =========================================================
    public Utilisateur getUserById(long id) {
        ensureDb();
        Utilisateur user = null;
        try (Cursor cursor = db.query("users", null, "id_utilisateur = ?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) user = cursorToUser(cursor);
        } catch (Exception e) {
            Log.e(TAG, "getUserById : " + e.getMessage());
        }
        return user;
    }

    public List<Utilisateur> getAllUsers() {
        ensureDb();
        List<Utilisateur> users = new ArrayList<>();
        try (Cursor cursor = db.query("users", null, null, null, null, null, "date_creation DESC")) {
            while (cursor.moveToNext()) users.add(cursorToUser(cursor));
        } catch (Exception e) {
            Log.e(TAG, "getAllUsers : " + e.getMessage());
        }
        return users;
    }

    // =========================================================
    // ✅ UPDATE
    // =========================================================
    public int updateUser(Utilisateur user) {
        ensureDb();
        if (user == null) return 0;
        int rows = 0;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("nom", user.getNom());
            values.put("email", user.getEmail().trim().toLowerCase());
            values.put("id_role", user.getIdRole());
            rows = db.update("users", values, "id_utilisateur = ?",
                    new String[]{String.valueOf(user.getId())});
            if (rows > 0) db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "updateUser : " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    // =========================================================
    // ✅ DELETE
    // =========================================================
    public int deleteUser(long idUser) {
        ensureDb();
        int rows = 0;
        db.beginTransaction();
        try {
            rows = db.delete("users", "id_utilisateur = ?", new String[]{String.valueOf(idUser)});
            if (rows > 0) db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "deleteUser : " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    // =========================================================
    // ✅ AUTHENTIFICATION
    // =========================================================
    public Utilisateur authenticate(String email, String plainPassword) {
        ensureDb();
        if (email == null || plainPassword == null) return null;
        Utilisateur user = null;
        try (Cursor cursor = db.query("users", null,
                "LOWER(email) = ? AND mot_de_passe_hash = ?",
                new String[]{email.trim().toLowerCase(), hashPassword(plainPassword)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                user = cursorToUser(cursor);
                Log.i(TAG, "Authentification réussie pour : " + email);
            }
        } catch (Exception e) {
            Log.e(TAG, "authenticate : " + e.getMessage());
        }
        return user;
    }

    // =========================================================
    // ✅ Vérifie si un email existe
    // =========================================================
    public boolean emailExists(String email) {
        ensureDb();
        boolean exists = false;
        try (Cursor cursor = db.rawQuery(
                "SELECT id_utilisateur FROM users WHERE LOWER(email) = ?",
                new String[]{email.trim().toLowerCase()})) {
            exists = cursor.moveToFirst();
        } catch (Exception e) {
            Log.e(TAG, "emailExists : " + e.getMessage());
        }
        return exists;
    }

    // =========================================================
    // ✅ Changer mot de passe
    // =========================================================
    public boolean changePassword(long idUser, String newPlainPassword) {
        ensureDb();
        if (newPlainPassword == null) return false;
        boolean success = false;
        String newHash = hashPassword(newPlainPassword);
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("mot_de_passe_hash", newHash);
            int rows = db.update("users", values, "id_utilisateur = ?", new String[]{String.valueOf(idUser)});
            if (rows > 0) {
                db.setTransactionSuccessful();
                success = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "changePassword : " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return success;
    }

    // =========================================================
    // Conversion Cursor → Utilisateur
    // =========================================================
    private Utilisateur cursorToUser(Cursor cursor) {
        Utilisateur u = new Utilisateur();
        u.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id_utilisateur")));
        u.setNom(cursor.getString(cursor.getColumnIndexOrThrow("nom")));
        u.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
        u.setMotDePasseHash(cursor.getString(cursor.getColumnIndexOrThrow("mot_de_passe_hash")));
        u.setIdRole(cursor.getLong(cursor.getColumnIndexOrThrow("id_role")));
        u.setDateCreation(cursor.getString(cursor.getColumnIndexOrThrow("date_creation")));
        return u;
    }

    // =========================================================
    // Compte des utilisateurs
    // =========================================================
    public long countUsers() {
        ensureDb();
        long count = 0;
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null)) {
            if (cursor.moveToFirst()) count = cursor.getLong(0);
        } catch (Exception e) {
            Log.e(TAG, "countUsers : " + e.getMessage());
        }
        return count;
    }
}



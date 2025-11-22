package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.safecity.model.Incident;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IncidentDAO
 * * Gestion complète des opérations CRUD sur la table "incidents".
 * CORRIGÉ : Utilise la table 'users' et gère correctement les alias SQL.
 */
public class IncidentDAO {

    private static final String TAG = "IncidentDAO";

    private final DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public IncidentDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        db = null;
    }

    private void ensureDb() {
        if (db == null || !db.isOpen()) {
            open();
        }
    }

    private boolean isValidStatut(String statut) {
        if (statut == null) return false;
        return Incident.STATUT_NOUVEAU.equals(statut)
                || Incident.STATUT_EN_COURS.equals(statut)
                || Incident.STATUT_TRAITE.equals(statut);
    }

    // =========================================================
    // CREATE
    // =========================================================
    public long insertIncident(Incident incident) {
        ensureDb();
        long result = -1;
        if (incident == null) return -1;

        String statut = incident.getStatut();
        if (statut == null || !isValidStatut(statut)) {
            statut = Incident.STATUT_NOUVEAU;
        }

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("id_utilisateur", incident.getIdUtilisateur());

            Long idCategorie = incident.getIdCategorie(); // Peut être null (long objet) ou 0 (long primitif)
            if (idCategorie != null && idCategorie > 0) {
                values.put("id_categorie", idCategorie);
            } else {
                values.putNull("id_categorie");
            }

            values.put("description", incident.getDescription());
            values.put("photo_url", incident.getPhotoUrl());
            values.put("latitude", incident.getLatitude());
            values.put("longitude", incident.getLongitude());
            values.put("statut", statut);

            if (incident.getDateSignalement() != null) {
                values.put("date_signalement", incident.getDateSignalement());
            }

            result = db.insert("incidents", null, values);
            if (result != -1) {
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.e(TAG, "insertIncident exception: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return result;
    }

    // =========================================================
    // READ : Tous les incidents (FIL D'ACTU)
    // =========================================================
    public List<Incident> getAllIncidents() {
        ensureDb();
        List<Incident> incidents = new ArrayList<>();
        Cursor cursor = null;
        try {
            // CORRECTION SQL :
            // 1. Table 'users' au lieu de 'utilisateurs'
            // 2. Alias 'u.nom AS userName'
            String query = "SELECT i.*, c.nom_categorie, u.nom AS userName " +
                    "FROM incidents i " +
                    "LEFT JOIN categories c ON i.id_categorie = c.id_categorie " +
                    "LEFT JOIN users u ON i.id_utilisateur = u.id_utilisateur " +
                    "ORDER BY i.date_signalement DESC";

            cursor = db.rawQuery(query, null);

            while (cursor != null && cursor.moveToNext()) {
                incidents.add(cursorToIncident(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllIncidents : " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return incidents;
    }

    // =========================================================
    // READ : Filtrage par Utilisateur (PROFIL)
    // =========================================================
    public List<Incident> getIncidentsByUtilisateur(long idUtilisateur) {
        ensureDb();
        List<Incident> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            // CORRECTION SQL ICI AUSSI : Table users + Alias userName
            String query = "SELECT i.*, c.nom_categorie, u.nom AS userName " +
                    "FROM incidents i " +
                    "LEFT JOIN categories c ON i.id_categorie = c.id_categorie " +
                    "LEFT JOIN users u ON i.id_utilisateur = u.id_utilisateur " +
                    "WHERE i.id_utilisateur = ? " +
                    "ORDER BY i.date_signalement DESC";

            cursor = db.rawQuery(query, new String[]{String.valueOf(idUtilisateur)});

            while (cursor != null && cursor.moveToNext()) {
                list.add(cursorToIncident(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getIncidentsByUtilisateur : " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    // =========================================================
    // READ : Par ID
    // =========================================================
    public Incident getIncidentById(long id) {
        ensureDb();
        Incident incident = null;
        Cursor cursor = null;
        try {
            cursor = db.query("incidents", null, "id_incident = ?",
                    new String[]{String.valueOf(id)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                incident = cursorToIncident(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getIncidentById : " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return incident;
    }

    // =========================================================
    // UPDATE
    // =========================================================
    public int updateIncident(Incident incident) {
        ensureDb();
        int rows = 0;
        if (incident == null) return 0;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("description", incident.getDescription());
            values.put("photo_url", incident.getPhotoUrl());
            values.put("statut", incident.getStatut());

            Long idCategorie = incident.getIdCategorie();
            if (idCategorie != null && idCategorie > 0) {
                values.put("id_categorie", idCategorie);
            } else {
                values.putNull("id_categorie");
            }

            values.put("latitude", incident.getLatitude());
            values.put("longitude", incident.getLongitude());

            rows = db.update("incidents", values, "id_incident = ?",
                    new String[]{String.valueOf(incident.getId())});

            if (rows > 0) db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "updateIncident exception: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    // =========================================================
    // DELETE
    // =========================================================
    public int deleteIncident(long idIncident) {
        ensureDb();
        int rows = 0;
        try {
            rows = db.delete("incidents", "id_incident = ?",
                    new String[]{String.valueOf(idIncident)});
        } catch (Exception e) {
            Log.e(TAG, "deleteIncident exception: " + e.getMessage());
        }
        return rows;
    }

    // =========================================================
    // CONVERSION CURSOR -> MODEL
    // =========================================================
    private Incident cursorToIncident(Cursor cursor) {
        Incident incident = new Incident();

        // --- Champs Standard ---
        incident.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id_incident")));
        incident.setIdUtilisateur(cursor.getLong(cursor.getColumnIndexOrThrow("id_utilisateur")));

        int idxCat = cursor.getColumnIndex("id_categorie");
        if (idxCat != -1 && !cursor.isNull(idxCat)) {
            incident.setIdCategorie(cursor.getLong(idxCat));
        }

        int idxDesc = cursor.getColumnIndex("description");
        if (idxDesc != -1) incident.setDescription(cursor.getString(idxDesc));

        int idxPhoto = cursor.getColumnIndex("photo_url");
        if (idxPhoto != -1) incident.setPhotoUrl(cursor.getString(idxPhoto));

        int idxLat = cursor.getColumnIndex("latitude");
        if (idxLat != -1) incident.setLatitude(cursor.getDouble(idxLat));

        int idxLon = cursor.getColumnIndex("longitude");
        if (idxLon != -1) incident.setLongitude(cursor.getDouble(idxLon));

        int idxStatut = cursor.getColumnIndex("statut");
        if (idxStatut != -1) incident.setStatut(cursor.getString(idxStatut));

        int idxDate = cursor.getColumnIndex("date_signalement");
        if (idxDate != -1) incident.setDateSignalement(cursor.getString(idxDate));

        // --- Champs JOINTS (Catégorie et User) ---

        // 1. Nom Catégorie
        int idxNomCat = cursor.getColumnIndex("nom_categorie");
        if (idxNomCat != -1 && !cursor.isNull(idxNomCat)) {
            incident.setNomCategorie(cursor.getString(idxNomCat));
        }

        // 2. Nom Utilisateur (Via l'alias 'userName' défini dans la requête)
        int idxUserName = cursor.getColumnIndex("userName");
        if (idxUserName != -1 && !cursor.isNull(idxUserName)) {
            incident.setUserName(cursor.getString(idxUserName));
        } else {
            // Fallback si l'alias n'est pas trouvé (ex: getIncidentById sans join)
            // On pourrait laisser null ou mettre "Utilisateur Inconnu" par défaut dans l'adaptateur
        }

        return incident;
    }

    // =========================================================
    // STATISTIQUES
    // =========================================================

    public Map<String, Integer> countIncidentsByStatut() {
        ensureDb();
        Map<String, Integer> stats = new LinkedHashMap<>();
        String query = "SELECT statut, COUNT(*) FROM incidents GROUP BY statut";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            while (cursor != null && cursor.moveToNext()) {
                stats.put(cursor.getString(0), cursor.getInt(1));
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { if (cursor != null) cursor.close(); }

        if (!stats.containsKey(Incident.STATUT_NOUVEAU)) stats.put(Incident.STATUT_NOUVEAU, 0);
        if (!stats.containsKey(Incident.STATUT_EN_COURS)) stats.put(Incident.STATUT_EN_COURS, 0);
        if (!stats.containsKey(Incident.STATUT_TRAITE)) stats.put(Incident.STATUT_TRAITE, 0);
        return stats;
    }

    public Map<String, Integer> countIncidentsByCategory(Context context) {
        ensureDb();
        Map<String, Integer> stats = new LinkedHashMap<>();
        String query = "SELECT c.nom_categorie, COUNT(i.id_incident) " +
                "FROM incidents i " +
                "JOIN categories c ON i.id_categorie = c.id_categorie " +
                "GROUP BY c.nom_categorie";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            while (cursor != null && cursor.moveToNext()) {
                stats.put(cursor.getString(0), cursor.getInt(1));
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { if (cursor != null) cursor.close(); }
        return stats;
    }
}


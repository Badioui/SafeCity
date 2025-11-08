package com.example.safecity.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.safecity.model.Incident;

import java.util.ArrayList;
import java.util.List;

/**
 * IncidentDAO
 *
 * Gestion complète des opérations CRUD sur la table "incidents".
 * Conforme au Cahier des Charges SafeCity.
 *  Auteur : Asmaa
 */
public class IncidentDAO {

    private static final String TAG = "IncidentDAO";

    private final DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public IncidentDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    // --- Ouvrir / Fermer la base ---
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

    // --- Utility : ensure DB open ---
    private void ensureDb() {
        if (db == null || !db.isOpen()) {
            open();
        }
    }

    // --- Validate statut against allowed constants ---
    private boolean isValidStatut(String statut) {
        if (statut == null) return false;
        return Incident.STATUT_NOUVEAU.equals(statut)
                || Incident.STATUT_EN_COURS.equals(statut)
                || Incident.STATUT_TRAITE.equals(statut);
    }

    // =========================================================
    // CREATE : Ajouter un incident (transaction)
    // =========================================================
    public long insertIncident(Incident incident) {
        ensureDb();
        long result = -1;
        if (incident == null) {
            Log.e(TAG, "insertIncident: incident is null");
            return -1;
        }
        // Validate statut (if not provided, set default)
        String statut = incident.getStatut();
        if (statut == null || !isValidStatut(statut)) {
            statut = Incident.STATUT_NOUVEAU;
            incident.setStatut(statut);
        }

        db.beginTransaction();
        Cursor cursor = null;
        try {
            ContentValues values = new ContentValues();
            values.put("id_utilisateur", incident.getIdUtilisateur());

            // --- DEBUT DE LA CORRECTION ---
            // 1. Récupérer l'objet Long
            Long idCategorie = incident.getIdCategorie();

            // 2. CORRECTION : Vérifier si l'objet Long est NULL AVANT de le comparer (> 0)
            if (idCategorie != null && idCategorie > 0) {
                values.put("id_categorie", idCategorie);
            } else {
                // Si null ou <= 0 (convention -1), on insère NULL en base
                values.putNull("id_categorie");
            }
            // --- FIN DE LA CORRECTION ---

            values.put("description", incident.getDescription());
            values.put("photo_url", incident.getPhotoUrl());
            values.put("latitude", incident.getLatitude());
            values.put("longitude", incident.getLongitude());
            values.put("statut", statut);

            result = db.insert("incidents", null, values);
            if (result == -1) {
                Log.e(TAG, "insertIncident: insertion failed");
            } else {
                db.setTransactionSuccessful();
                Log.i(TAG, "insertIncident: success id=" + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "insertIncident exception: " + e.getMessage());
        } finally {
            db.endTransaction();
            if (cursor != null) cursor.close();
        }
        return result;
    }

    // =========================================================
    // READ : Récupérer un incident par son ID
    // =========================================================
    public Incident getIncidentById(long id) {
        ensureDb();
        Incident incident = null;
        Cursor cursor = null;
        try {
            cursor = db.query(
                    "incidents",
                    null,
                    "id_incident = ?",
                    new String[]{String.valueOf(id)},
                    null, null, null
            );

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
    // READ : Tous les incidents (DESC date)
    // =========================================================
    public List<Incident> getAllIncidents() {
        ensureDb();
        List<Incident> incidents = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query("incidents", null, null, null, null, null, "date_signalement DESC");
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
    // READ : Filtrage par statut
    // =========================================================
    public List<Incident> getIncidentsByStatut(String statut) {
        ensureDb();
        List<Incident> list = new ArrayList<>();
        if (!isValidStatut(statut)) {
            Log.w(TAG, "getIncidentsByStatut: statut invalide -> " + statut);
            return list;
        }
        Cursor cursor = null;
        try {
            cursor = db.query(
                    "incidents",
                    null,
                    "statut = ?",
                    new String[]{statut},
                    null, null,
                    "date_signalement DESC"
            );
            while (cursor != null && cursor.moveToNext()) {
                list.add(cursorToIncident(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getIncidentsByStatut : " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    // =========================================================
    // READ : Incidents d’un utilisateur
    // =========================================================
    public List<Incident> getIncidentsByUtilisateur(long idUtilisateur) {
        ensureDb();
        List<Incident> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    "incidents",
                    null,
                    "id_utilisateur = ?",
                    new String[]{String.valueOf(idUtilisateur)},
                    null, null,
                    "date_signalement DESC"
            );
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
    // READ : Filtrage par catégorie
    // =========================================================
    public List<Incident> getIncidentsByCategorie(long idCategorie) {
        ensureDb();
        List<Incident> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    "incidents",
                    null,
                    "id_categorie = ?",
                    new String[]{String.valueOf(idCategorie)},
                    null, null,
                    "date_signalement DESC"
            );
            while (cursor != null && cursor.moveToNext()) {
                list.add(cursorToIncident(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getIncidentsByCategorie : " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    // =========================================================
    // UPDATE : Modifier un incident (transaction)
    // =========================================================
    public int updateIncident(Incident incident) {
        ensureDb();
        if (incident == null) {
            Log.e(TAG, "updateIncident: incident is null");
            return 0;
        }
        int rows = 0;
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            if (incident.getDescription() != null) values.put("description", incident.getDescription());
            values.put("photo_url", incident.getPhotoUrl());
            if (isValidStatut(incident.getStatut())) {
                values.put("statut", incident.getStatut());
            } else {
                Log.w(TAG, "updateIncident: statut invalide -> " + incident.getStatut());
            }

            Long idCategorie = incident.getIdCategorie();
            if (idCategorie != null && idCategorie > 0) {
                values.put("id_categorie", idCategorie);
            } else {
                values.putNull("id_categorie");
            }
            values.put("latitude", incident.getLatitude());
            values.put("longitude", incident.getLongitude());

            rows = db.update(
                    "incidents",
                    values,
                    "id_incident = ?",
                    new String[]{String.valueOf(incident.getId())}
            );
            if (rows > 0) {
                db.setTransactionSuccessful();
                Log.i(TAG, "updateIncident: rows=" + rows);
            } else {
                Log.w(TAG, "updateIncident: aucune ligne mise à jour pour id=" + incident.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "updateIncident exception: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    // =========================================================
    // DELETE : Supprimer un incident (transaction)
    // =========================================================
    public int deleteIncident(long idIncident) {
        ensureDb();
        int rows = 0;
        db.beginTransaction();
        try {
            rows = db.delete("incidents", "id_incident = ?", new String[]{String.valueOf(idIncident)});
            if (rows > 0) {
                db.setTransactionSuccessful();
                Log.i(TAG, "deleteIncident: id=" + idIncident + " supprimé");
            } else {
                Log.w(TAG, "deleteIncident: id=" + idIncident + " non trouvé");
            }
        } catch (Exception e) {
            Log.e(TAG, "deleteIncident exception: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
        return rows;
    }

    // =========================================================
    // FILTRAGE PAR PROXIMITÉ (km) — bounding box + Haversine
    // =========================================================
    public List<Incident> getIncidentsByProximity(double latitude, double longitude, double rayonKm) {
        ensureDb();
        List<Incident> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            // 1) bounding box approx: 1 deg latitude ~ 111 km
            double latDelta = rayonKm / 111.0;
            double latMin = latitude - latDelta;
            double latMax = latitude + latDelta;

            // Longitude delta depends on latitude
            double lonDelta = rayonKm / (111.320 * Math.cos(Math.toRadians(latitude)));
            double lonMin = longitude - lonDelta;
            double lonMax = longitude + lonDelta;

            String where = "latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?";
            String[] args = {
                    String.valueOf(latMin),
                    String.valueOf(latMax),
                    String.valueOf(lonMin),
                    String.valueOf(lonMax)
            };

            cursor = db.query("incidents", null, where, args, null, null, "date_signalement DESC");
            while (cursor != null && cursor.moveToNext()) {
                Incident inc = cursorToIncident(cursor);
                // compute exact distance (Haversine)
                double distanceKm = haversine(latitude, longitude, inc.getLatitude(), inc.getLongitude());
                if (distanceKm <= rayonKm) {
                    list.add(inc);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getIncidentsByProximity : " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    // Haversine formula (distance in kilometers)
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of earth in KM
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // =========================================================
    // Conversion Cursor -> Incident (gère les NULLs)
    // =========================================================
    private Incident cursorToIncident(Cursor cursor) {
        Incident incident = new Incident();
        // id
        incident.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id_incident")));
        // user
        incident.setIdUtilisateur(cursor.getLong(cursor.getColumnIndexOrThrow("id_utilisateur")));
        // id_categorie may be NULL
        int idxCat = cursor.getColumnIndexOrThrow("id_categorie");
        if (!cursor.isNull(idxCat)) {
            incident.setIdCategorie(cursor.getLong(idxCat));
        } else {
            incident.setIdCategorie(null); // -1 => aucune catégorie
        }
        // description / photo
        int idxDesc = cursor.getColumnIndexOrThrow("description");
        if (!cursor.isNull(idxDesc)) incident.setDescription(cursor.getString(idxDesc));
        int idxPhoto = cursor.getColumnIndexOrThrow("photo_url");
        if (!cursor.isNull(idxPhoto)) incident.setPhotoUrl(cursor.getString(idxPhoto));
        // lat / lon (default 0 if null)
        int idxLat = cursor.getColumnIndexOrThrow("latitude");
        if (!cursor.isNull(idxLat)) incident.setLatitude(cursor.getDouble(idxLat));
        else incident.setLatitude(0.0);
        int idxLon = cursor.getColumnIndexOrThrow("longitude");
        if (!cursor.isNull(idxLon)) incident.setLongitude(cursor.getDouble(idxLon));
        else incident.setLongitude(0.0);
        // statut
        int idxStatut = cursor.getColumnIndexOrThrow("statut");
        if (!cursor.isNull(idxStatut)) incident.setStatut(cursor.getString(idxStatut));
        else incident.setStatut(Incident.STATUT_NOUVEAU);
        // date_signalement (TEXT)
        int idxDate = cursor.getColumnIndexOrThrow("date_signalement");
        if (!cursor.isNull(idxDate)) incident.setDateSignalement(cursor.getString(idxDate));
        else incident.setDateSignalement(null);
        return incident;
    }

    // =========================================================
    // Utilitaire : compte d'incidents (utile pour tests)
    // =========================================================
    public long countIncidents() {
        ensureDb();
        long count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM incidents", null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "countIncidents : " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }



// =========================================================
// STATISTIQUES :
// =========================================================

    /**
     * Calcule le nombre d'incidents par statut (Nouveau, En cours, Traité).
     * @return Map<Statut, Count>
     */
    public java.util.Map<String, Integer> countIncidentsByStatut() {
        ensureDb();
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();

        // Requête : SELECT statut, COUNT(*) FROM incidents GROUP BY statut
        String query = "SELECT statut, COUNT(*) FROM incidents GROUP BY statut";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            while (cursor != null && cursor.moveToNext()) {
                String statut = cursor.getString(0);
                int count = cursor.getInt(1);
                if (statut != null) {
                    stats.put(statut, count);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "countIncidentsByStatut exception: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        // Assurer que les clés standard sont toujours présentes (même avec 0)
        if (!stats.containsKey(Incident.STATUT_NOUVEAU)) stats.put(Incident.STATUT_NOUVEAU, 0);
        if (!stats.containsKey(Incident.STATUT_EN_COURS)) stats.put(Incident.STATUT_EN_COURS, 0);
        if (!stats.containsKey(Incident.STATUT_TRAITE)) stats.put(Incident.STATUT_TRAITE, 0);

        return stats;
    }

    /**
     * Calcule le nombre d'incidents par catégorie.
     * @return Map<NomCategorie, Count>
     */
    public java.util.Map<String, Integer> countIncidentsByCategory(Context context) {
        ensureDb();
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();

        // Jointure : incidents (pour le compte) + categories (pour le nom)
        // GROUP BY id_categorie (pour les statistiques)
        String query = "SELECT c.nom_categorie, COUNT(i.id_incident) " +
                "FROM incidents i " +
                "JOIN categories c ON i.id_categorie = c.id_categorie " +
                "GROUP BY c.nom_categorie " +
                "ORDER BY COUNT(i.id_incident) DESC";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            while (cursor != null && cursor.moveToNext()) {
                String nomCategorie = cursor.getString(0);
                int count = cursor.getInt(1);
                stats.put(nomCategorie, count);
            }
        } catch (Exception e) {
            Log.e(TAG, "countIncidentsByCategory exception: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        // Gérer les incidents sans catégorie (id_categorie NULL)
        // Requête : SELECT COUNT(*) FROM incidents WHERE id_categorie IS NULL
        String nullQuery = "SELECT COUNT(*) FROM incidents WHERE id_categorie IS NULL";
        Cursor nullCursor = null;
        try {
            nullCursor = db.rawQuery(nullQuery, null);
            if (nullCursor != null && nullCursor.moveToFirst()) {
                int nullCount = nullCursor.getInt(0);
                if (nullCount > 0) {
                    // Utiliser une clé explicite pour les incidents non classés
                    stats.put("Non Classé", nullCount);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "count IncidentsByCategory (NULL) exception: " + e.getMessage());
        } finally {
            if (nullCursor != null) nullCursor.close();
        }

        return stats;
    }
}


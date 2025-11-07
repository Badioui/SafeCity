package com.example.safecity.model;

/**
 * Modèle représentant un incident signalé par un citoyen.
 * Correspond à la table "incidents".
 */
public class Incident {

    // --- Constantes de statut ---
    public static final String STATUT_NOUVEAU = "Nouveau";
    public static final String STATUT_EN_COURS = "En cours";
    public static final String STATUT_TRAITE = "Traité";

    // --- Champs ---
    private long id;                 // id_incident
    private String photoUrl;         // photo_url
    private String description;      // description
    private Long idCategorie;        // FK vers categories(id_categorie)
    private double latitude;         // latitude
    private double longitude;        // longitude
    private String dateSignalement;  // TEXT (SQLite timestamp)
    private String statut;           // Nouveau / En cours / Traité
    private long idUtilisateur;      // FK vers users(id_utilisateur)

    // --- Constructeurs ---
    public Incident() {}

    public Incident(long id, String photoUrl, String description,
                    long idCategorie, double latitude, double longitude,
                    String dateSignalement, String statut, long idUtilisateur) {
        this.id = id;
        this.photoUrl = photoUrl;
        this.description = description;
        this.idCategorie = idCategorie;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dateSignalement = dateSignalement;
        this.statut = statut;
        this.idUtilisateur = idUtilisateur;
    }

    // --- Getters / Setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getIdCategorie() { return idCategorie; }
    public void setIdCategorie(long idCategorie) { this.idCategorie = idCategorie; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDateSignalement() { return dateSignalement; }
    public void setDateSignalement(String dateSignalement) { this.dateSignalement = dateSignalement; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public long getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(long idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    // --- Utilitaires ---
    public boolean isTraite() {
        return STATUT_TRAITE.equalsIgnoreCase(statut);
    }

    @Override
    public String toString() {
        return "[" + statut + "] " + description + " (" + latitude + "," + longitude + ")";
    }
}


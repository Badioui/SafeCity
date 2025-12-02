package com.example.safecity.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem; // Import nécessaire pour le clustering
import com.google.firebase.firestore.Exclude;

import java.util.Date;

/**
 * Modèle représentant un incident signalé par un citoyen.
 * Adapté pour Firebase Firestore et le Clustering Google Maps.
 * Auteur : Asmaa
 */
public class Incident implements ClusterItem { // Implements ClusterItem ajouté

    // --- Constantes de statut ---
    public static final String STATUT_NOUVEAU = "Nouveau";
    public static final String STATUT_EN_COURS = "En cours";
    public static final String STATUT_TRAITE = "Traité";

    // --- Champs ---
    private String id;               // id_incident (Firestore Document ID)
    private String photoUrl;         // photo_url
    private String description;      // description
    private String idCategorie;      // FK vers collection categories
    private double latitude;         // latitude
    private double longitude;        // longitude
    private Date dateSignalement;    // Date formatée
    private String statut;           // Nouveau / En cours / Traité
    private String idUtilisateur;    // FK vers collection users (Auth UID)

    private String nomCategorie;
    private String nomUtilisateur;
    // Note: 'userName' semblait redondant avec 'nomUtilisateur', je garde les deux pour compatibilité
    private String userName;

    // --- Constructeur Vide (OBLIGATOIRE POUR FIREBASE) ---
    public Incident() {
    }

    // --- Constructeur Complet ---
    public Incident(String id, String photoUrl, String description,
                    String idCategorie, double latitude, double longitude,
                    Date dateSignalement, String statut, String idUtilisateur) {
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIdCategorie() { return idCategorie; }
    public void setIdCategorie(String idCategorie) { this.idCategorie = idCategorie; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Date getDateSignalement() { return dateSignalement; }
    public void setDateSignalement(Date dateSignalement) { this.dateSignalement = dateSignalement; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }

    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    // --- Utilitaires ---
    @Exclude
    public boolean isTraite() {
        return STATUT_TRAITE.equalsIgnoreCase(statut);
    }

    @Override
    public String toString() {
        return "[" + statut + "] " + description + " (" + latitude + "," + longitude + ")";
    }

    // =================================================================
    // IMPLÉMENTATION DE L'INTERFACE ClusterItem
    // Ces méthodes sont utilisées par le ClusterManager pour placer les marqueurs
    // =================================================================

    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    @Override
    public String getTitle() {
        // Le titre du marqueur sera le nom de la catégorie (ex: "Accident")
        return nomCategorie != null ? nomCategorie : "Incident";
    }

    @Override
    public String getSnippet() {
        // Le sous-titre du marqueur sera la description
        return description;
    }

    // Note: getZIndex() est optionnel selon la version de la librairie,
    // par défaut il retourne null/0.0f si non implémenté.
}
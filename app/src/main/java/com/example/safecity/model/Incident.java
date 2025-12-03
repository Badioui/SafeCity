package com.example.safecity.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Incident implements ClusterItem {

    // --- Constantes de statut ---
    public static final String STATUT_NOUVEAU = "Nouveau";
    public static final String STATUT_EN_COURS = "En cours";
    public static final String STATUT_TRAITE = "Traité";

    // --- Champs principaux (Stockés dans Firestore) ---
    private String id;
    private String photoUrl;
    private String description;
    private String idCategorie;
    private double latitude;
    private double longitude;
    private Date dateSignalement;
    private String statut;
    private String idUtilisateur;

    // --- Champs dénormalisés (Pour l'affichage rapide) ---
    private String nomCategorie;
    private String nomUtilisateur;
    // NOTE : Suppression de 'userName' pour éviter les doublons et erreurs d'affichage

    // --- Constructeur Vide (Requis par Firestore) ---
    public Incident() {
        // Valeur par défaut
        this.statut = STATUT_NOUVEAU;
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

    // C'est le seul champ pour le nom de l'utilisateur désormais
    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    // --- Utilitaires ---
    @Exclude
    public boolean isTraite() {
        return STATUT_TRAITE.equalsIgnoreCase(statut);
    }

    @Override
    public String toString() {
        return "[" + statut + "] " + description;
    }

    // --- Implémentation de ClusterItem (Google Maps) ---
    @Override
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    @Override
    public String getTitle() {
        return nomCategorie != null ? nomCategorie : "Incident";
    }

    @Override
    public String getSnippet() {
        return description;
    }
}
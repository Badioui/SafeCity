package com.example.safecity.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Incident implements ClusterItem {

    // --- Constantes de statut ---
    public static final String STATUT_NOUVEAU = "Nouveau";
    public static final String STATUT_EN_COURS = "En cours";
    public static final String STATUT_TRAITE = "Traité";

    // --- Champs principaux (Stockés dans Firestore) ---
    private String id;
    private String photoUrl;
    private String videoUrl; // Support Vidéo V2
    private String description;
    private String idCategorie;
    private double latitude;
    private double longitude;
    private Date dateSignalement;
    private String statut;
    private String idUtilisateur;

    // --- NOUVEAUX CHAMPS V2.5 ---
    private String auteurPhotoUrl; // Avatar de l'auteur
    private int likesCount;        // Nombre de J'aime
    private List<String> likedBy;  // Liste des ID utilisateurs qui ont liké

    // --- Champs dénormalisés (Pour l'affichage rapide) ---
    private String nomCategorie;
    private String nomUtilisateur;

    // --- Constructeur Vide (Requis par Firestore) ---
    public Incident() {
        // Valeurs par défaut
        this.statut = STATUT_NOUVEAU;
        this.likesCount = 0;
        this.likedBy = new ArrayList<>();
    }

    // --- Constructeur Standard ---
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
        // Init likes
        this.likesCount = 0;
        this.likedBy = new ArrayList<>();
    }

    // --- Constructeur Complet (Avec Vidéo) ---
    public Incident(String id, String photoUrl, String videoUrl, String description,
                    String idCategorie, double latitude, double longitude,
                    Date dateSignalement, String statut, String idUtilisateur) {
        this(id, photoUrl, description, idCategorie, latitude, longitude, dateSignalement, statut, idUtilisateur);
        this.videoUrl = videoUrl;
    }

    // --- Getters / Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

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

    // --- Getters / Setters Nouveaux Champs ---
    public String getAuteurPhotoUrl() { return auteurPhotoUrl; }
    public void setAuteurPhotoUrl(String auteurPhotoUrl) { this.auteurPhotoUrl = auteurPhotoUrl; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }

    // --- Utilitaires ---
    @Exclude
    public boolean isTraite() {
        return STATUT_TRAITE.equalsIgnoreCase(statut);
    }

    @Exclude
    public boolean hasMedia() {
        return (photoUrl != null && !photoUrl.isEmpty()) || (videoUrl != null && !videoUrl.isEmpty());
    }

    /**
     * Vérifie si l'utilisateur donné a déjà liké cet incident.
     * @param userId L'ID de l'utilisateur courant
     * @return true si l'utilisateur a liké, false sinon
     */
    @Exclude
    public boolean isLikedBy(String userId) {
        return likedBy != null && likedBy.contains(userId);
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
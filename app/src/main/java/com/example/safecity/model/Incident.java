package com.example.safecity.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Modèle représentant un incident signalé dans SafeCity.
 * Implémente ClusterItem pour permettre le regroupement de marqueurs sur la carte.
 * Utilise @IgnoreExtraProperties pour garantir la compatibilité ascendante de la base de données.
 */
@IgnoreExtraProperties
public class Incident implements ClusterItem {

    // --- Constantes de statut ---
    public static final String STATUT_NOUVEAU = "Nouveau";
    public static final String STATUT_EN_COURS = "En cours";
    public static final String STATUT_TRAITE = "Traité";

    // --- Champs persistés dans Firestore ---
    private String id;
    private String photoUrl;
    private String videoUrl;
    private String description;
    private String idCategorie;
    private double latitude;
    private double longitude;
    private Date dateSignalement;
    private String statut;
    private String idUtilisateur;

    // --- Champs Sociaux & Engagement (V2.5) ---
    private String auteurPhotoUrl; // Stocke l'URL de l'avatar au moment du post (performance)
    private int likesCount;
    private List<String> likedBy;  // Liste des UIDs pour vérifier l'état du bouton Like
    private int commentsCount;

    // --- Champs Dénormalisés (Performance UI) ---
    // Ces champs évitent de faire des requêtes supplémentaires pour récupérer le nom du user ou de la catégorie
    private String nomCategorie;
    private String nomUtilisateur;

    /**
     * Constructeur vide requis pour la désérialisation Firebase Firestore.
     */
    public Incident() {
        this.statut = STATUT_NOUVEAU;
        this.likesCount = 0;
        this.commentsCount = 0;
        this.likedBy = new ArrayList<>();
    }

    /**
     * Constructeur standard pour les signalements de base.
     */
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

        // Initialisation des compteurs sociaux
        this.likesCount = 0;
        this.commentsCount = 0;
        this.likedBy = new ArrayList<>();
    }

    /**
     * Constructeur complet incluant le support vidéo.
     */
    public Incident(String id, String photoUrl, String videoUrl, String description,
                    String idCategorie, double latitude, double longitude,
                    Date dateSignalement, String statut, String idUtilisateur) {
        this(id, photoUrl, description, idCategorie, latitude, longitude, dateSignalement, statut, idUtilisateur);
        this.videoUrl = videoUrl;
    }

    // --- Getters et Setters ---

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

    public String getAuteurPhotoUrl() { return auteurPhotoUrl; }
    public void setAuteurPhotoUrl(String auteurPhotoUrl) { this.auteurPhotoUrl = auteurPhotoUrl; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }

    // --- Méthodes Utilitaires (Exclues de Firestore) ---

    @Exclude
    public boolean isTraite() {
        return STATUT_TRAITE.equalsIgnoreCase(statut);
    }

    @Exclude
    public boolean hasMedia() {
        return (photoUrl != null && !photoUrl.isEmpty()) || (videoUrl != null && !videoUrl.isEmpty());
    }

    /**
     * Vérifie si un utilisateur spécifique a aimé cet incident.
     * @param userId L'UID de l'utilisateur à vérifier.
     * @return true si l'utilisateur est dans la liste likedBy.
     */
    @Exclude
    public boolean isLikedBy(String userId) {
        return likedBy != null && likedBy.contains(userId);
    }

    // --- Implémentation de ClusterItem (Google Maps Utils) ---

    @Override
    @Exclude
    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }

    @Override
    @Exclude
    public String getTitle() {
        return nomCategorie != null ? nomCategorie : "Incident";
    }

    @Override
    @Exclude
    public String getSnippet() {
        return description;
    }

    @Override
    public String toString() {
        return "[" + statut + "] " + (nomCategorie != null ? nomCategorie : "Sans catégorie") + " : " + description;
    }
}
package com.example.safecity.model;
import java.util.Date;
/**
 * Modèle représentant un incident signalé par un citoyen.
 * Adapté pour Firebase Firestore (IDs en String).
 * Auteur : Asmaa
 */
public class Incident {

    // --- Constantes de statut ---
    public static final String STATUT_NOUVEAU = "Nouveau";
    public static final String STATUT_EN_COURS = "En cours";
    public static final String STATUT_TRAITE = "Traité";

    // --- Champs ---
    // Firestore utilise des Strings pour les IDs (Document ID)
    private String id;               // id_incident (Firestore Document ID)

    private String photoUrl;         // photo_url
    private String description;      // description

    // Les clés étrangères deviennent des Strings car les IDs cibles sont des Strings dans Firestore
    private String idCategorie;      // FK vers collection categories

    private double latitude;         // latitude
    private double longitude;        // longitude
    private Date  dateSignalement;  // Date formatée
    private String statut;           // Nouveau / En cours / Traité

    private String idUtilisateur;    // FK vers collection users (Auth UID)

    private String userName;
    private String nomCategorie;
    private String nomUtilisateur;

    // --- Constructeur Vide (OBLIGATOIRE POUR FIREBASE) ---
    public Incident() {
        // Firebase a besoin de ce constructeur vide pour reconstruire l'objet
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

    // ID est maintenant un String
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Categorie ID est maintenant un String
    public String getIdCategorie() { return idCategorie; }
    public void setIdCategorie(String idCategorie) { this.idCategorie = idCategorie; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public  Date getDateSignalement() { return dateSignalement; }
    public void setDateSignalement(Date dateSignalement) { this.dateSignalement = dateSignalement; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    // Utilisateur ID est maintenant un String (UID)
    public String getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(String idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }

    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    // --- Utilitaires ---
    public boolean isTraite() {
        return STATUT_TRAITE.equalsIgnoreCase(statut);
    }

    @Override
    public String toString() {
        return "[" + statut + "] " + description + " (" + latitude + "," + longitude + ")";
    }
}


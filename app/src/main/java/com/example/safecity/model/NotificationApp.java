package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

/**
 * Modèle pour les notifications et alertes de SafeCity.
 * Enrichi pour gérer les interactions personnalisées (likes, commentaires).
 */
public class NotificationApp {

    @DocumentId
    private String id;
    private String titre;
    private String message;
    private Date date;
    private String type; // "alerte", "info", "validation", "like", "comment"

    // Nouveaux champs pour la personnalisation
    private String idDestinataire;   // UID de l'utilisateur qui doit recevoir la notif
    private String idIncidentSource; // ID de l'incident lié à la notification
    private String nomExpediteur;    // Nom de la personne qui a liké/commenté
    private boolean lu;

    // Constructeur vide OBLIGATOIRE pour la désérialisation Firebase
    public NotificationApp() {
        this.date = new Date();
        this.lu = false;
    }

    /**
     * Constructeur pour les alertes globales ou systèmes.
     */
    public NotificationApp(String titre, String message, String type) {
        this();
        this.titre = titre;
        this.message = message;
        this.type = type;
    }

    /**
     * Constructeur complet pour les notifications personnalisées (Likes/Comments).
     */
    public NotificationApp(String titre, String message, String type,
                           String idDestinataire, String idIncidentSource, String nomExpediteur) {
        this(titre, message, type);
        this.idDestinataire = idDestinataire;
        this.idIncidentSource = idIncidentSource;
        this.nomExpediteur = nomExpediteur;
    }

    // --- Getters et Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getIdDestinataire() { return idDestinataire; }
    public void setIdDestinataire(String idDestinataire) { this.idDestinataire = idDestinataire; }

    public String getIdIncidentSource() { return idIncidentSource; }
    public void setIdIncidentSource(String idIncidentSource) { this.idIncidentSource = idIncidentSource; }

    public String getNomExpediteur() { return nomExpediteur; }
    public void setNomExpediteur(String nomExpediteur) { this.nomExpediteur = nomExpediteur; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
}
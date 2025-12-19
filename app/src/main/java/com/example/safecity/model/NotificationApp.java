package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

/**
 * Modèle pour les notifications et alertes.
 * Les setters ont été corrigés pour permettre la désérialisation Firestore.
 */
public class NotificationApp {

    @DocumentId
    private String id;
    private String titre;
    private String message;
    private Date date;
    private String type; // "alerte", "info", "validation", "ALERTE_OFFICIELLE"
    private boolean lu;

    // Constructeur vide OBLIGATOIRE pour Firebase
    public NotificationApp() {}

    public NotificationApp(String titre, String message, String type) {
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.date = new Date();
        this.lu = false;
    }

    // --- Getters et Setters (Corrigés) ---

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

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
}
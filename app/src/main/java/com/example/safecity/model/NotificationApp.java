package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

public class NotificationApp {
    @DocumentId
    private String id;
    private String titre;
    private String message;
    private Date date;
    private String type; // "alerte", "info", "validation"
    private boolean lu;

    public NotificationApp() {} // Constructeur vide requis par Firebase

    public NotificationApp(String titre, String message, String type) {
        this.titre = titre;
        this.message = message;
        this.type = type;
        this.date = new Date();
        this.lu = false;
    }

    // Getters
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public Date getDate() { return date; }
    public String getType() { return type; }
}
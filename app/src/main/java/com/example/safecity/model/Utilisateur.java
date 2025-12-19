package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/**
 * Modèle représentant un utilisateur SafeCity.
 * Le grade est calculé dynamiquement pour éviter les désynchronisations.
 */
public class Utilisateur {

    @DocumentId
    private String id;

    private String nom;
    private String email;
    private String motDePasseHash;
    private String idRole; // "admin", "autorite", "citoyen"
    private String dateCreation;
    private String photoProfilUrl;
    private int score;

    public Utilisateur() {
        this.score = 0;
    }

    public Utilisateur(String id, String nom, String email, String idRole, String dateCreation) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.idRole = idRole;
        this.dateCreation = dateCreation;
        this.score = 0;
    }

    // --- Getters / Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public String getIdRole() { return idRole; }
    public void setIdRole(String idRole) { this.idRole = idRole; }

    public String getDateCreation() { return dateCreation; }
    public void setDateCreation(String dateCreation) { this.dateCreation = dateCreation; }

    public String getPhotoProfilUrl() { return photoProfilUrl; }
    public void setPhotoProfilUrl(String photoProfilUrl) { this.photoProfilUrl = photoProfilUrl; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    /**
     * Calcule le grade en fonction du score actuel.
     * @Exclude empêche Firestore d'essayer d'écrire ce champ en base.
     */
    @Exclude
    public String getGrade() {
        if (score < 50) return "Novice";
        if (score < 100) return "Éclaireur";
        if (score < 500) return "Gardien";
        return "Héros de la Cité";
    }

    @Override
    public String toString() {
        return nom + " (" + getGrade() + ")";
    }
}
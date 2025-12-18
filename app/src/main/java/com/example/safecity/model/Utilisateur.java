package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Modèle représentant un utilisateur (citoyen, autorité ou admin).
 * Adapté pour Firestore.
 */
public class Utilisateur {

    @DocumentId // L'ID du document Firestore (ex: UID de l'utilisateur)
    private String id;

    private String nom;
    private String email;
    private String motDePasseHash; // Optionnel avec Firebase Auth
    private String idRole; // FK vers la collection roles
    private String dateCreation;

    // NOUVEAU : URL de la photo de profil (Avatar)
    private String photoProfilUrl;

    // NOUVEAU CHAMP : Score de gamification
    private int score;

    // --- Constructeur vide OBLIGATOIRE pour Firestore ---
    public Utilisateur() {
        this.score = 0; // Valeur par défaut
    }

    public Utilisateur(String id, String nom, String email,
                       String motDePasseHash, String idRole, String dateCreation) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.idRole = idRole;
        this.dateCreation = dateCreation;
        this.score = 0; // Initialisation par défaut
    }

    // Constructeur complet avec Photo
    public Utilisateur(String id, String nom, String email,
                       String motDePasseHash, String idRole, String dateCreation, String photoProfilUrl) {
        this(id, nom, email, motDePasseHash, idRole, dateCreation);
        this.photoProfilUrl = photoProfilUrl;
    }

    // --- Getters / Setters ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public String getIdRole() {
        return idRole;
    }

    public void setIdRole(String idRole) {
        this.idRole = idRole;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    // NOUVEAU : Getter/Setter Photo de Profil
    public String getPhotoProfilUrl() {
        return photoProfilUrl;
    }

    public void setPhotoProfilUrl(String photoProfilUrl) {
        this.photoProfilUrl = photoProfilUrl;
    }

    // --- GESTION DU SCORE (GAMIFICATION) ---

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    // Méthode utilitaire pour déterminer le grade
    public String getGrade() {
        if (score < 50) return "Novice";
        if (score < 100) return "Éclaireur";
        if (score < 500) return "Gardien";
        return "Héros de la Cité";
    }

    // --- Utilitaire ---
    @Override
    public String toString() {
        return nom + " (" + email + ") - " + getGrade();
    }
}
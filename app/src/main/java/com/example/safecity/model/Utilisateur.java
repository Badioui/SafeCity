package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Modèle représentant un utilisateur (citoyen, autorité ou admin).
 * Adapté pour Firestore :
 * - id est maintenant une String (correspond souvent à l'UID Firebase Auth)
 * - idRole est maintenant une String (référence au document Role)
 */
public class Utilisateur {

    @DocumentId // L'ID du document Firestore (ex: UID de l'utilisateur)
    private String id;

    private String nom;
    private String email;
    // Note : Le mot de passe n'est généralement pas stocké dans Firestore si on utilise Firebase Auth,
    // mais on peut garder le champ si besoin de compatibilité legacy, ou le retirer.
    private String motDePasseHash;

    private String idRole; // FK vers la collection roles (String maintenant)
    private String dateCreation;

    // --- Constructeur vide OBLIGATOIRE pour Firestore ---
    public Utilisateur() {}

    public Utilisateur(String id, String nom, String email,
                       String motDePasseHash, String idRole, String dateCreation) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.idRole = idRole;
        this.dateCreation = dateCreation;
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

    // CHANGEMENT MAJEUR : long -> String
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

    // --- Utilitaire ---
    @Override
    public String toString() {
        return nom + " (" + email + ")";
    }
}

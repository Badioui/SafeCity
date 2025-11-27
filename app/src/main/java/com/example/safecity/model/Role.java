package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Modèle représentant un rôle utilisateur dans SafeCity.
 * Adapté pour Firestore (ID String).
 * (Admin, Autorité, Citoyen)
 */
public class Role {

    @DocumentId // Cette annotation permet à Firestore de mapper l'ID du document ici
    private String id;
    private String nomRole;

    // --- Constructeur vide OBLIGATOIRE pour Firestore ---
    public Role() {}

    public Role(String id, String nomRole) {
        this.id = id;
        this.nomRole = nomRole;
    }

    // --- Getters / Setters ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomRole() {
        return nomRole;
    }

    public void setNomRole(String nomRole) {
        this.nomRole = nomRole;
    }

    // --- Utilitaire ---
    @Override
    public String toString() {
        return nomRole;
    }
}

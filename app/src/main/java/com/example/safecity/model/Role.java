package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Modèle pour les rôles utilisateurs.
 */
public class Role {

    @DocumentId
    private String id;
    private String nomRole;

    public Role() {}

    public Role(String id, String nomRole) {
        this.id = id;
        this.nomRole = nomRole;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNomRole() { return nomRole; }
    public void setNomRole(String nomRole) { this.nomRole = nomRole; }

    @Override
    public String toString() {
        return nomRole;
    }
}
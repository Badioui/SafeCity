package com.example.safecity.model;

/**
 * Modèle représentant un rôle utilisateur dans SafeCity.
 * (Admin, Autorité, Citoyen)
 */
public class Role {

    private long id;            // Correspond à id_role
    private String nomRole;     // Nom du rôle (unique)

    // --- Constructeurs ---
    public Role() {}

    public Role(long id, String nomRole) {
        this.id = id;
        this.nomRole = nomRole;
    }

    // --- Getters / Setters ---
    public long getId() {
        return id;
    }

    public void setId(long id) {
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


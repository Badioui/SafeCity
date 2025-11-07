package com.example.safecity.model;

/**
 * Modèle représentant une catégorie d’incident.
 * (Vol, Accident, Incendie, etc.)
 * Correspond à la table "categories".
 */
public class Categorie {

    private long id;                // id_categorie
    private String nomCategorie;    // nom_categorie (unique)

    // --- Constructeurs ---
    public Categorie() {}

    public Categorie(long id, String nomCategorie) {
        this.id = id;
        this.nomCategorie = nomCategorie;
    }

    // --- Getters / Setters ---
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNomCategorie() {
        return nomCategorie;
    }

    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;
    }

    // --- Utilitaire ---
    @Override
    public String toString() {
        return nomCategorie;
    }
}

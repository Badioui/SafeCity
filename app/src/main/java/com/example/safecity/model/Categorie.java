package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Modèle pour les catégories d'incidents.
 */
public class Categorie {

    @DocumentId
    private String id;
    private String nomCategorie;

    public Categorie() {}

    public Categorie(String id, String nomCategorie) {
        this.id = id;
        this.nomCategorie = nomCategorie;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }

    @Override
    public String toString() {
        return nomCategorie;
    }
}
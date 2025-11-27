package com.example.safecity.model;

import com.google.firebase.firestore.DocumentId;

/**
 * Modèle représentant une catégorie d’incident.
 * Adapté pour Firestore (ID String).
 */
public class Categorie {

    @DocumentId // Cette annotation remplit automatiquement l'ID avec celui du document Firestore lors de la lecture
    private String id;
    private String nomCategorie;

    // --- Constructeur vide OBLIGATOIRE pour Firestore ---
    public Categorie() {}

    public Categorie(String id, String nomCategorie) {
        this.id = id;
        this.nomCategorie = nomCategorie;
    }

    // --- Getters / Setters ---
    // Note : L'ID est maintenant un String
    public String getId() {
        return id;
    }

    public void setId(String id) {
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
        return nomCategorie; // Ce qui sera affiché dans le Spinner
    }
}
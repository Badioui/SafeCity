package com.example.safecity.model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Modèle représentant un commentaire sur un incident.
 * Utilise @ServerTimestamp pour une gestion cohérente du temps via Firebase.
 */
@IgnoreExtraProperties
public class Comment {

    private String id;
    private String idIncident;
    private String idUtilisateur;
    private String texte;

    // --- Champs Dénormalisés (Performance) ---
    private String nomUtilisateur;
    private String auteurPhotoUrl;

    // --- Horodatage Serveur ---
    @ServerTimestamp
    private Date datePublication;

    /**
     * Constructeur vide requis pour Firebase.
     */
    public Comment() {
    }

    /**
     * Constructeur complet pour la création locale avant envoi.
     */
    public Comment(String idIncident, String idUtilisateur, String nomUtilisateur, String auteurPhotoUrl, String texte) {
        this.idIncident = idIncident;
        this.idUtilisateur = idUtilisateur;
        this.nomUtilisateur = nomUtilisateur;
        this.auteurPhotoUrl = auteurPhotoUrl;
        this.texte = texte;
    }

    // --- Getters et Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdIncident() {
        return idIncident;
    }

    public void setIdIncident(String idIncident) {
        this.idIncident = idIncident;
    }

    public String getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(String idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }

    public String getAuteurPhotoUrl() {
        return auteurPhotoUrl;
    }

    public void setAuteurPhotoUrl(String auteurPhotoUrl) {
        this.auteurPhotoUrl = auteurPhotoUrl;
    }

    public Date getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(Date datePublication) {
        this.datePublication = datePublication;
    }
}
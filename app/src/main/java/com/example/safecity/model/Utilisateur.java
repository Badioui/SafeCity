package com.example.safecity.model;

/**
 * Modèle représentant un utilisateur (citoyen, autorité ou admin).
 * Correspond à la table "users".
 *  Auteur : Asmaa
 */
public class Utilisateur {

    private long id;                 // id_utilisateur
    private String nom;              // Nom complet
    private String email;            // Email unique
    private String motDePasseHash;   // Mot de passe hashé (bcrypt)
    private long idRole;             // FK vers roles(id_role)
    private String dateCreation;     // TEXT (SQLite : CURRENT_TIMESTAMP)

    // --- Constructeurs ---
    public Utilisateur() {}

    public Utilisateur(long id, String nom, String email,
                       String motDePasseHash, long idRole, String dateCreation) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.idRole = idRole;
        this.dateCreation = dateCreation;
    }

    // --- Getters / Setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public long getIdRole() { return idRole; }
    public void setIdRole(long idRole) { this.idRole = idRole; }

    public String getDateCreation() { return dateCreation; }
    public void setDateCreation(String dateCreation) { this.dateCreation = dateCreation; }

    // --- Utilitaire ---
    @Override
    public String toString() {
        return nom + " (" + email + ")";
    }
}


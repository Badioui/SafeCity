package com.example.safecity.models;


public class Role {
    private int id;
    private String nomRole; // Citoyen, Autorité, Admin

    // Constructeur
    public Role(int id, String nomRole) {
        this.id = id;
        this.nomRole = nomRole;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomRole() { return nomRole; }
    public void setNomRole(String nomRole) { this.nomRole = nomRole; }
}

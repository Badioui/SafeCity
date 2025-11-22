package com.example.safecity.utils;

import android.util.Log;
import com.example.safecity.model.Incident;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        // Initialisation de l'instance Firestore
        db = FirebaseFirestore.getInstance();
    }

    // --- 1. AJOUTER UN INCIDENT ---
    public void addIncident(Incident incident, OnFirestoreTaskComplete listener) {
        // On ajoute l'incident dans la collection "incidents"
        db.collection("incidents")
                .add(incident)
                .addOnSuccessListener(documentReference -> {
                    // Firestore a généré un ID unique, on le met à jour dans l'objet
                    incident.setId(documentReference.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Erreur ajout", e);
                    listener.onError(e);
                });
    }

    // --- 2. LIRE LES INCIDENTS (TEMPS RÉEL) ---
    public interface OnDataLoadListener {
        void onIncidentsLoaded(List<Incident> incidents);
        void onError(Exception e);
    }

    public void getIncidentsRealtime(OnDataLoadListener listener) {
        db.collection("incidents")
                .orderBy("dateSignalement", Query.Direction.DESCENDING) // Tri par date
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }

                    if (snapshots != null) {
                        List<Incident> incidents = new ArrayList<>();
                        // Transformation des documents bruts en objets Java Incident
                        incidents = snapshots.toObjects(Incident.class);

                        // Pour récupérer l'ID du document et le mettre dans l'objet
                        for (int i = 0; i < snapshots.size(); i++) {
                            incidents.get(i).setId(snapshots.getDocuments().get(i).getId());
                        }

                        listener.onIncidentsLoaded(incidents);
                    }
                });
    }

    // Interface simple pour les callbacks (succès/erreur)
    public interface OnFirestoreTaskComplete {
        void onSuccess();
        void onError(Exception e);
    }
}
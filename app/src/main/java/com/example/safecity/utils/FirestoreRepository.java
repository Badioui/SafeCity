package com.example.safecity.utils;

import com.example.safecity.model.Incident;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.List;

public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // --- Interfaces de Callback ---
    public interface OnFirestoreTaskComplete {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnDataLoadListener {
        void onIncidentsLoaded(List<Incident> incidents);
        void onError(Exception e);
    }

    // ============================================================
    // 1. AJOUTER (CREATE)
    // ============================================================
    public void addIncident(Incident incident, OnFirestoreTaskComplete listener) {
        db.collection("incidents")
                .add(incident)
                .addOnSuccessListener(documentReference -> {
                    // 1. On récupère l'ID généré par Firestore
                    String generatedId = documentReference.getId();

                    // 2. On met à jour l'objet Java local
                    incident.setId(generatedId);

                    // 3. IMPORTANT : On met à jour le champ "id" DANS le document Firestore
                    // Cela permet à toObjects(Incident.class) de remplir l'ID automatiquement plus tard
                    documentReference.update("id", generatedId);

                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    // ============================================================
    // 2. LIRE TOUT EN TEMPS RÉEL (READ ALL)
    // ============================================================
    public void getIncidentsRealtime(OnDataLoadListener listener) {
        db.collection("incidents")
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (snapshots != null) {
                        // Grâce à l'étape 3 du addIncident, l'ID est mappé automatiquement
                        listener.onIncidentsLoaded(snapshots.toObjects(Incident.class));
                    }
                });
    }

    // ============================================================
    // 3. LIRE PAR UTILISATEUR (READ BY USER) - Pour "Mes Incidents"
    // ============================================================
    public void getMyIncidents(String userId, OnDataLoadListener listener) {
        // Attention : Si vous ajoutez un orderBy("dateSignalement") ici aussi,
        // Firestore demandera de créer un "Index Composite" (lien dans le Logcat).
        // Pour l'instant, on filtre juste par utilisateur.

        db.collection("incidents")
                .whereEqualTo("idUtilisateur", userId)
                .get() // On utilise get() (lecture unique) au lieu de snapshot listener pour simplifier
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Incident> list = queryDocumentSnapshots.toObjects(Incident.class);
                    listener.onIncidentsLoaded(list);
                })
                .addOnFailureListener(listener::onError);
    }

    // ============================================================
    // 4. SUPPRIMER (DELETE)
    // ============================================================
    public void deleteIncident(String incidentId, OnFirestoreTaskComplete listener) {
        if (incidentId == null || incidentId.isEmpty()) {
            listener.onError(new Exception("ID invalide"));
            return;
        }

        db.collection("incidents").document(incidentId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }
}
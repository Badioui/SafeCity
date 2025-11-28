package com.example.safecity.utils;

import com.example.safecity.model.Categorie;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Role;
import com.example.safecity.model.Utilisateur;
import com.google.firebase.firestore.FieldValue; // Import ajouté pour l'incrémentation
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface OnFirestoreTaskComplete {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnDataLoadListener {
        void onIncidentsLoaded(List<Incident> incidents);
        void onError(Exception e);
    }

    public interface OnRolesLoadedListener {
        void onRolesLoaded(List<Role> roles);
        void onError(Exception e);
    }

    public interface OnCategoriesLoadedListener {
        void onCategoriesLoaded(List<Categorie> categories);
        void onError(Exception e);
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(Utilisateur utilisateur);
        void onError(Exception e);
    }

    // 1. AJOUTER INCIDENT
    public void addIncident(Incident incident, OnFirestoreTaskComplete listener) {
        db.collection("incidents")
                .add(incident)
                .addOnSuccessListener(documentReference -> {
                    String generatedId = documentReference.getId();
                    incident.setId(generatedId);
                    documentReference.update("id", generatedId);
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    // 2. METTRE À JOUR STATUT INCIDENT (Validation)
    public void updateIncidentStatus(String incidentId, String newStatus, OnFirestoreTaskComplete listener) {
        db.collection("incidents").document(incidentId)
                .update("statut", newStatus)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // NOUVELLE MÉTHODE : Incrémenter le score (Gamification)
    public void incrementUserScore(String userId, int points) {
        if (userId == null) return;
        db.collection("users").document(userId)
                .update("score", FieldValue.increment(points))
                .addOnFailureListener(e -> {
                    // Gestion silencieuse ou log (ex: Log.e("Firestore", "Error updating score", e));
                });
    }

    // 3. LIRE (Optimisé : Limit 50)
    public ListenerRegistration getIncidentsRealtime(OnDataLoadListener listener) {
        return db.collection("incidents")
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (snapshots != null) {
                        listener.onIncidentsLoaded(snapshots.toObjects(Incident.class));
                    }
                });
    }

    // 4. LIRE MES INCIDENTS
    public void getMyIncidents(String userId, OnDataLoadListener listener) {
        db.collection("incidents")
                .whereEqualTo("idUtilisateur", userId)
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Incident> list = queryDocumentSnapshots.toObjects(Incident.class);
                    listener.onIncidentsLoaded(list);
                })
                .addOnFailureListener(listener::onError);
    }

    // 5. RÉCUPÉRER UN UTILISATEUR
    public void getUser(String uid, OnUserLoadedListener listener) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onUserLoaded(documentSnapshot.toObject(Utilisateur.class));
                    } else {
                        listener.onError(new Exception("Utilisateur introuvable"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // 6. SUPPRIMER (Optimisé : Image + Doc)
    public void deleteIncident(String incidentId, String photoUrl, OnFirestoreTaskComplete listener) {
        if (incidentId == null || incidentId.isEmpty()) {
            listener.onError(new Exception("ID invalide"));
            return;
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            try {
                FirebaseStorage.getInstance()
                        .getReferenceFromUrl(photoUrl)
                        .delete()
                        .addOnSuccessListener(aVoid -> deleteFirestoreDoc(incidentId, listener))
                        .addOnFailureListener(e -> deleteFirestoreDoc(incidentId, listener));
            } catch (Exception e) {
                deleteFirestoreDoc(incidentId, listener);
            }
        } else {
            deleteFirestoreDoc(incidentId, listener);
        }
    }

    private void deleteFirestoreDoc(String incidentId, OnFirestoreTaskComplete listener) {
        db.collection("incidents").document(incidentId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // 7. & 8. ROLES ET CATEGORIES
    public void getRoles(OnRolesLoadedListener listener) {
        db.collection("roles").get().addOnSuccessListener(q -> {
            if (q != null) listener.onRolesLoaded(q.toObjects(Role.class));
        }).addOnFailureListener(listener::onError);
    }

    public void getCategories(OnCategoriesLoadedListener listener) {
        db.collection("categories").orderBy("nomCategorie").get().addOnSuccessListener(q -> {
            if (q != null) listener.onCategoriesLoaded(q.toObjects(Categorie.class));
        }).addOnFailureListener(listener::onError);
    }
}
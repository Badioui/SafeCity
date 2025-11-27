package com.example.safecity.utils;

import com.example.safecity.model.Categorie;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Role;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage; // Import Storage

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

    // 1. AJOUTER
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

    // 2. LIRE (Optimisé : Limit 50)
    public ListenerRegistration getIncidentsRealtime(OnDataLoadListener listener) {
        return db.collection("incidents")
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .limit(50) // <--- OPTIMISATION : On ne charge que les 50 derniers
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

    // 3. LIRE MES INCIDENTS
    public void getMyIncidents(String userId, OnDataLoadListener listener) {
        db.collection("incidents")
                .whereEqualTo("idUtilisateur", userId)
                .orderBy("dateSignalement", Query.Direction.DESCENDING) // Si index composite créé
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Incident> list = queryDocumentSnapshots.toObjects(Incident.class);
                    listener.onIncidentsLoaded(list);
                })
                .addOnFailureListener(listener::onError);
    }

    // 4. SUPPRIMER (Optimisé : Image + Doc)
    public void deleteIncident(String incidentId, String photoUrl, OnFirestoreTaskComplete listener) {
        if (incidentId == null || incidentId.isEmpty()) {
            listener.onError(new Exception("ID invalide"));
            return;
        }

        // Si il y a une photo, on la supprime du Storage d'abord
        if (photoUrl != null && !photoUrl.isEmpty()) {
            try {
                FirebaseStorage.getInstance()
                        .getReferenceFromUrl(photoUrl)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            // Succès suppression image -> suppression doc
                            deleteFirestoreDoc(incidentId, listener);
                        })
                        .addOnFailureListener(e -> {
                            // Échec suppression image (peut-être déjà supprimée) -> on force suppression doc
                            deleteFirestoreDoc(incidentId, listener);
                        });
            } catch (Exception e) {
                // Url malformée ou autre -> on supprime le doc quand même
                deleteFirestoreDoc(incidentId, listener);
            }
        } else {
            // Pas de photo, suppression directe
            deleteFirestoreDoc(incidentId, listener);
        }
    }

    // Méthode privée helper pour ne pas répéter le code
    private void deleteFirestoreDoc(String incidentId, OnFirestoreTaskComplete listener) {
        db.collection("incidents").document(incidentId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // 5. & 6. ROLES ET CATEGORIES (Inchangé)
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
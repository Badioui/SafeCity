package com.example.safecity.utils;

import com.example.safecity.model.Categorie;
import com.example.safecity.model.Incident;
// IMPORT AJOUTÉ
import com.example.safecity.model.NotificationApp;
import com.example.safecity.model.Role;
import com.example.safecity.model.Utilisateur;
import com.google.firebase.firestore.FieldValue;
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

    // --- INTERFACES DE CALLBACK ---

    public interface OnFirestoreTaskComplete {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnDataLoadListener {
        void onIncidentsLoaded(List<Incident> incidents);
        void onError(Exception e);
    }

    public interface OnIncidentLoadedListener {
        void onIncidentLoaded(Incident incident);
        void onError(Exception e);
    }

    // AJOUT : Interface pour les notifications
    public interface OnNotificationsLoadedListener {
        void onNotificationsLoaded(List<NotificationApp> notifications);
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

    // ==================================================================
    // MÉTHODES D'ÉCRITURE (CREATE / UPDATE)
    // ==================================================================

    // 1. AJOUTER INCIDENT (Nouveau)
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

    // 2. METTRE À JOUR STATUT (Validation Admin/Autorité)
    public void updateIncidentStatus(String incidentId, String newStatus, OnFirestoreTaskComplete listener) {
        db.collection("incidents").document(incidentId)
                .update("statut", newStatus)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // 3. METTRE À JOUR LES DÉTAILS (Modification par l'utilisateur)
    public void updateIncidentDetails(Incident incident, OnFirestoreTaskComplete listener) {
        if (incident.getId() == null) {
            listener.onError(new Exception("ID manquant pour la mise à jour"));
            return;
        }
        db.collection("incidents").document(incident.getId())
                .set(incident)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    // 4. INCRÉMENTER SCORE (Gamification)
    public void incrementUserScore(String userId, int points) {
        if (userId == null) return;
        db.collection("users").document(userId)
                .update("score", FieldValue.increment(points))
                .addOnFailureListener(e -> { });
    }

    // AJOUT : AJOUTER UNE NOTIFICATION (Système)
    public void addNotification(NotificationApp notification) {
        db.collection("notifications").add(notification);
    }

    // ==================================================================
    // MÉTHODES DE LECTURE (READ)
    // ==================================================================

    // 5. LIRE TOUS LES INCIDENTS (Temps réel)
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

    // 6. LIRE MES INCIDENTS
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

    // 7. RÉCUPÉRER UN SEUL INCIDENT
    public void getIncident(String incidentId, OnIncidentLoadedListener listener) {
        db.collection("incidents").document(incidentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        listener.onIncidentLoaded(doc.toObject(Incident.class));
                    } else {
                        listener.onError(new Exception("Incident introuvable"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // 8. RÉCUPÉRER UN UTILISATEUR
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

    // AJOUT : RÉCUPÉRER LES NOTIFICATIONS
    public void getNotifications(OnNotificationsLoadedListener listener) {
        db.collection("notifications")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null) {
                        listener.onNotificationsLoaded(querySnapshot.toObjects(NotificationApp.class));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    // ==================================================================
    // SUPPRESSION & UTILITAIRES
    // ==================================================================

    // 9. SUPPRIMER INCIDENT
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

    // 10. ROLES ET CATEGORIES
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
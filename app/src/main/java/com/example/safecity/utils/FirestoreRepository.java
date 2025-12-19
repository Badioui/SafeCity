package com.example.safecity.utils;

import com.example.safecity.model.Categorie;
import com.example.safecity.model.Comment;
import com.example.safecity.model.Incident;
import com.example.safecity.model.NotificationApp;
import com.example.safecity.model.Role;
import com.example.safecity.model.Utilisateur;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository central pour la gestion des données Firestore de l'application SafeCity.
 * Gère les incidents, les utilisateurs, les notifications et les commentaires.
 */
public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ==================================================================
    // INTERFACES DE CALLBACK (LISTENERS)
    // ==================================================================

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

    public interface OnCommentsLoadedListener {
        void onCommentsLoaded(List<Comment> comments);
        void onError(Exception e);
    }

    // ==================================================================
    // MÉTHODES D'ÉCRITURE (INCIDENTS ET NOTIFICATIONS)
    // ==================================================================

    /**
     * Ajoute un nouvel incident et enregistre l'ID généré dans le document.
     */
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

    /**
     * Met à jour le statut d'un incident.
     */
    public void updateIncidentStatus(String incidentId, String newStatus, OnFirestoreTaskComplete listener) {
        db.collection("incidents").document(incidentId)
                .update("statut", newStatus)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /**
     * Met à jour l'intégralité des détails d'un incident.
     */
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

    /**
     * Incrémente le score de l'utilisateur.
     */
    public void incrementUserScore(String userId, int points) {
        if (userId == null) return;
        db.collection("users").document(userId)
                .update("score", FieldValue.increment(points))
                .addOnFailureListener(e -> { });
    }

    /**
     * Ajoute une alerte officielle.
     */
    public void addOfficialAlert(NotificationApp alert, OnFirestoreTaskComplete listener) {
        db.collection("official_alerts")
                .add(alert)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /**
     * Ajoute une notification au système.
     */
    public void addNotification(NotificationApp notification) {
        db.collection("notifications").add(notification);
    }

    // ==================================================================
    // MÉTHODES POUR LES COMMENTAIRES (AVEC TRANSACTION)
    // ==================================================================

    /**
     * Ajoute un commentaire et incrémente atomiquement le compteur sur l'incident.
     */
    public void addComment(Comment comment, OnFirestoreTaskComplete listener) {
        DocumentReference incidentRef = db.collection("incidents").document(comment.getIdIncident());
        DocumentReference commentRef = incidentRef.collection("comments").document();

        db.runTransaction(transaction -> {
                    // 1. Incrémenter le compteur sur l'incident parent
                    transaction.update(incidentRef, "commentsCount", FieldValue.increment(1));

                    // 2. Ajouter le commentaire dans la sous-collection
                    transaction.set(commentRef, comment);

                    return null;
                }).addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    /**
     * Récupère les commentaires d'un incident en temps réel.
     */
    public ListenerRegistration getCommentsRealtime(String incidentId, OnCommentsLoadedListener listener) {
        return db.collection("incidents").document(incidentId).collection("comments")
                .orderBy("datePublication", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    List<Comment> comments = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment c = doc.toObject(Comment.class);
                            if (c != null) {
                                c.setId(doc.getId());
                                comments.add(c);
                            }
                        }
                    }
                    listener.onCommentsLoaded(comments);
                });
    }

    // ==================================================================
    // MÉTHODES DE LECTURE (READ)
    // ==================================================================

    /**
     * Récupère tous les incidents en temps réel avec extraction de l'ID.
     */
    public ListenerRegistration getIncidentsRealtime(OnDataLoadListener listener) {
        return db.collection("incidents")
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    List<Incident> incidents = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Incident incident = doc.toObject(Incident.class);
                            if (incident != null) {
                                incident.setId(doc.getId());
                                incidents.add(incident);
                            }
                        }
                    }
                    listener.onIncidentsLoaded(incidents);
                });
    }

    /**
     * Récupère les incidents d'un utilisateur spécifique.
     */
    public void getMyIncidents(String userId, OnDataLoadListener listener) {
        db.collection("incidents")
                .whereEqualTo("idUtilisateur", userId)
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Incident> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Incident incident = doc.toObject(Incident.class);
                        if (incident != null) {
                            incident.setId(doc.getId());
                            list.add(incident);
                        }
                    }
                    listener.onIncidentsLoaded(list);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Récupère un incident spécifique par son ID.
     */
    public void getIncident(String incidentId, OnIncidentLoadedListener listener) {
        db.collection("incidents").document(incidentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Incident incident = doc.toObject(Incident.class);
                        if (incident != null) incident.setId(doc.getId());
                        listener.onIncidentLoaded(incident);
                    } else {
                        listener.onError(new Exception("Incident introuvable"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Récupère les informations d'un utilisateur.
     */
    public void getUser(String uid, OnUserLoadedListener listener) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Utilisateur user = documentSnapshot.toObject(Utilisateur.class);
                        if (user != null) {
                            // CORRECTION : Utilisation de setId au lieu de setUid
                            user.setId(documentSnapshot.getId());
                        }
                        listener.onUserLoaded(user);
                    } else {
                        listener.onError(new Exception("Utilisateur introuvable"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Récupère les dernières notifications.
     */
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
    // SUPPRESSION ET RÉFÉRENTIELS
    // ==================================================================

    /**
     * Supprime un incident et son média associé.
     */
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

    /**
     * Charge les rôles.
     */
    public void getRoles(OnRolesLoadedListener listener) {
        db.collection("roles").get().addOnSuccessListener(q -> {
            if (q != null) listener.onRolesLoaded(q.toObjects(Role.class));
        }).addOnFailureListener(listener::onError);
    }

    /**
     * Charge les catégories.
     */
    public void getCategories(OnCategoriesLoadedListener listener) {
        db.collection("categories").orderBy("nomCategorie").get().addOnSuccessListener(q -> {
            if (q != null) listener.onCategoriesLoaded(q.toObjects(Categorie.class));
        }).addOnFailureListener(listener::onError);
    }
}
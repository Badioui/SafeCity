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
 * Repository central pour SafeCity.
 * Gère les incidents, utilisateurs, notifications, catégories et commentaires.
 */
public class FirestoreRepository {

    private final FirebaseFirestore db;

    private static final String COL_INCIDENTS = "incidents";
    private static final String COL_USERS = "utilisateurs";
    private static final String COL_NOTIFS = "notifications";
    private static final String COL_OFFICIAL_ALERTS = "official_alerts";
    private static final String COL_CATEGORIES = "categories";
    private static final String COL_ROLES = "roles";
    private static final String COL_COMMENTS = "comments";

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ==================================================================
    // INTERFACES DE CALLBACK
    // ==================================================================

    public interface OnFirestoreTaskComplete { void onSuccess(); void onError(Exception e); }
    public interface OnDataLoadListener { void onIncidentsLoaded(List<Incident> incidents); void onError(Exception e); }
    public interface OnIncidentLoadedListener { void onIncidentLoaded(Incident incident); void onError(Exception e); }
    public interface OnNotificationsLoadedListener { void onNotificationsLoaded(List<NotificationApp> notifications); void onError(Exception e); }
    public interface OnUserLoadedListener { void onUserLoaded(Utilisateur utilisateur); void onError(Exception e); }
    public interface OnCommentsLoadedListener { void onCommentsLoaded(List<Comment> comments); void onError(Exception e); }
    public interface OnCategoriesLoadedListener { void onCategoriesLoaded(List<Categorie> categories); void onError(Exception e); }
    public interface OnRolesLoadedListener { void onRolesLoaded(List<Role> roles); void onError(Exception e); }

    // ==================================================================
    // MÉTHODES D'ÉCRITURE
    // ==================================================================

    public void addIncident(Incident incident, OnFirestoreTaskComplete listener) {
        db.collection(COL_INCIDENTS).add(incident)
                .addOnSuccessListener(ref -> {
                    // On met à jour l'objet localement
                    incident.setId(ref.getId());
                    // Optionnel : on ne sauvegarde plus l'ID dans le document pour éviter les conflits
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateIncidentStatus(String incidentId, String newStatus, OnFirestoreTaskComplete listener) {
        db.collection(COL_INCIDENTS).document(incidentId).update("statut", newStatus)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void updateIncidentDetails(Incident incident, OnFirestoreTaskComplete listener) {
        if (incident.getId() == null) {
            listener.onError(new Exception("ID manquant"));
            return;
        }
        db.collection(COL_INCIDENTS).document(incident.getId()).set(incident)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void incrementUserScore(String userId, int points) {
        if (userId == null) return;
        db.collection(COL_USERS).document(userId).update("score", FieldValue.increment(points));
    }

    public void addOfficialAlert(NotificationApp alert, OnFirestoreTaskComplete listener) {
        db.collection(COL_OFFICIAL_ALERTS).add(alert)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void addNotification(NotificationApp notification) {
        db.collection(COL_NOTIFS).add(notification);
    }

    public void addComment(Comment comment, OnFirestoreTaskComplete listener) {
        DocumentReference incidentRef = db.collection(COL_INCIDENTS).document(comment.getIdIncident());
        DocumentReference commentRef = incidentRef.collection(COL_COMMENTS).document();

        db.runTransaction(transaction -> {
            transaction.update(incidentRef, "commentsCount", FieldValue.increment(1));
            transaction.set(commentRef, comment);
            return null;
        }).addOnSuccessListener(aVoid -> listener.onSuccess()).addOnFailureListener(listener::onError);
    }

    // ==================================================================
    // MÉTHODES DE LECTURE (SÉCURISÉES CONTRE LE CRASH ID)
    // ==================================================================

    public ListenerRegistration getIncidentsRealtime(OnDataLoadListener listener) {
        return db.collection(COL_INCIDENTS).orderBy("dateSignalement", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { listener.onError(e); return; }
                    List<Incident> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Incident inc = doc.toObject(Incident.class);
                            if (inc != null) {
                                inc.setId(doc.getId()); // Injection manuelle sécurisée
                                list.add(inc);
                            }
                        }
                    }
                    listener.onIncidentsLoaded(list);
                });
    }

    public void getMyIncidents(String userId, OnDataLoadListener listener) {
        db.collection(COL_INCIDENTS).whereEqualTo("idUtilisateur", userId)
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Incident> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snaps.getDocuments()) {
                        Incident inc = doc.toObject(Incident.class);
                        if (inc != null) { inc.setId(doc.getId()); list.add(inc); }
                    }
                    listener.onIncidentsLoaded(list);
                }).addOnFailureListener(listener::onError);
    }

    public void getIncident(String incidentId, OnIncidentLoadedListener listener) {
        db.collection(COL_INCIDENTS).document(incidentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Incident incident = doc.toObject(Incident.class);
                        if (incident != null) incident.setId(doc.getId());
                        listener.onIncidentLoaded(incident);
                    } else { listener.onError(new Exception("Introuvable")); }
                }).addOnFailureListener(listener::onError);
    }

    public void getUser(String uid, OnUserLoadedListener listener) {
        db.collection(COL_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Utilisateur user = doc.toObject(Utilisateur.class);
                        if (user != null) user.setId(doc.getId());
                        listener.onUserLoaded(user);
                    } else { listener.onError(new Exception("Profil inexistant")); }
                }).addOnFailureListener(listener::onError);
    }

    public void getNotifications(OnNotificationsLoadedListener listener) {
        db.collection(COL_NOTIFS).orderBy("date", Query.Direction.DESCENDING).limit(30)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps != null) listener.onNotificationsLoaded(snaps.toObjects(NotificationApp.class));
                }).addOnFailureListener(listener::onError);
    }

    public void getCategories(OnCategoriesLoadedListener listener) {
        db.collection(COL_CATEGORIES).orderBy("nomCategorie").get()
                .addOnSuccessListener(snaps -> {
                    if (snaps != null) listener.onCategoriesLoaded(snaps.toObjects(Categorie.class));
                }).addOnFailureListener(listener::onError);
    }

    public void getRoles(OnRolesLoadedListener listener) {
        db.collection(COL_ROLES).get()
                .addOnSuccessListener(snaps -> {
                    if (snaps != null) listener.onRolesLoaded(snaps.toObjects(Role.class));
                }).addOnFailureListener(listener::onError);
    }

    public ListenerRegistration getCommentsRealtime(String incidentId, OnCommentsLoadedListener listener) {
        return db.collection(COL_INCIDENTS).document(incidentId).collection(COL_COMMENTS)
                .orderBy("datePublication", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { listener.onError(error); return; }
                    List<Comment> list = new ArrayList<>();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment c = doc.toObject(Comment.class);
                            if (c != null) { c.setId(doc.getId()); list.add(c); }
                        }
                    }
                    listener.onCommentsLoaded(list);
                });
    }

    // ==================================================================
    // SUPPRESSION
    // ==================================================================

    public void deleteIncident(String incidentId, String photoUrl, OnFirestoreTaskComplete listener) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(photoUrl).delete()
                    .addOnCompleteListener(task -> db.collection(COL_INCIDENTS).document(incidentId).delete()
                            .addOnSuccessListener(a -> listener.onSuccess())
                            .addOnFailureListener(listener::onError));
        } else {
            db.collection(COL_INCIDENTS).document(incidentId).delete()
                    .addOnSuccessListener(a -> listener.onSuccess())
                    .addOnFailureListener(listener::onError);
        }
    }
}
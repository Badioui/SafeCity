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
 * Version unifiée utilisant la collection "utilisateurs" pour la cohérence avec l'inscription.
 */
public class FirestoreRepository {

    private final FirebaseFirestore db;

    // Constantes pour éviter les fautes de frappe
    private static final String COL_INCIDENTS = "incidents";
    private static final String COL_USERS = "utilisateurs"; // Unifié ici
    private static final String COL_NOTIFS = "notifications";
    private static final String COL_COMMENTS = "comments";

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // --- CALLBACKS ---
    public interface OnFirestoreTaskComplete { void onSuccess(); void onError(Exception e); }
    public interface OnDataLoadListener { void onIncidentsLoaded(List<Incident> incidents); void onError(Exception e); }
    public interface OnIncidentLoadedListener { void onIncidentLoaded(Incident incident); void onError(Exception e); }
    public interface OnNotificationsLoadedListener { void onNotificationsLoaded(List<NotificationApp> notifications); void onError(Exception e); }
    public interface OnUserLoadedListener { void onUserLoaded(Utilisateur utilisateur); void onError(Exception e); }
    public interface OnCommentsLoadedListener { void onCommentsLoaded(List<Comment> comments); void onError(Exception e); }

    // --- ÉCRITURE ---

    public void addIncident(Incident incident, OnFirestoreTaskComplete listener) {
        db.collection(COL_INCIDENTS).add(incident)
                .addOnSuccessListener(ref -> {
                    incident.setId(ref.getId());
                    ref.update("id", ref.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateIncidentStatus(String incidentId, String newStatus, OnFirestoreTaskComplete listener) {
        db.collection(COL_INCIDENTS).document(incidentId).update("statut", newStatus)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void incrementUserScore(String userId, int points) {
        if (userId == null) return;
        db.collection(COL_USERS).document(userId).update("score", FieldValue.increment(points));
    }

    // --- COMMENTAIRES (Transaction) ---

    public void addComment(Comment comment, OnFirestoreTaskComplete listener) {
        DocumentReference incidentRef = db.collection(COL_INCIDENTS).document(comment.getIdIncident());
        DocumentReference commentRef = incidentRef.collection(COL_COMMENTS).document();

        db.runTransaction(transaction -> {
            transaction.update(incidentRef, "commentsCount", FieldValue.increment(1));
            transaction.set(commentRef, comment);
            return null;
        }).addOnSuccessListener(aVoid -> listener.onSuccess()).addOnFailureListener(listener::onError);
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

    // --- LECTURE ---

    public ListenerRegistration getIncidentsRealtime(OnDataLoadListener listener) {
        return db.collection(COL_INCIDENTS).orderBy("dateSignalement", Query.Direction.DESCENDING).limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { listener.onError(e); return; }
                    List<Incident> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Incident inc = doc.toObject(Incident.class);
                            if (inc != null) { inc.setId(doc.getId()); list.add(inc); }
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

    // --- SUPPRESSION ---

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
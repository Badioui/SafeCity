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

    public interface OnFirestoreTaskComplete { void onSuccess(); void onError(Exception e); }
    public interface OnDataLoadListener { void onIncidentsLoaded(List<Incident> incidents); void onError(Exception e); }
    public interface OnIncidentLoadedListener { void onIncidentLoaded(Incident incident); void onError(Exception e); }
    public interface OnNotificationsLoadedListener { void onNotificationsLoaded(List<NotificationApp> notifications); void onError(Exception e); }
    public interface OnUserLoadedListener { void onUserLoaded(Utilisateur utilisateur); void onError(Exception e); }
    public interface OnCommentsLoadedListener { void onCommentsLoaded(List<Comment> comments); void onError(Exception e); }
    public interface OnRolesLoadedListener { void onRolesLoaded(List<Role> roles); void onError(Exception e); }
    // CORRECTION : Ajout de l'interface manquante
    public interface OnCategoriesLoadedListener { void onCategoriesLoaded(List<Categorie> categories); void onError(Exception e); }


    public void validateIncident(String incidentId, String authorId, boolean isValid, OnFirestoreTaskComplete listener) {
        String newStatus = isValid ? Incident.STATUT_TRAITE : "Rejeté";
        int pointsChange = isValid ? 20 : -10;

        db.runTransaction(transaction -> {
            DocumentReference incRef = db.collection(COL_INCIDENTS).document(incidentId);
            transaction.update(incRef, "statut", newStatus);

            if (authorId != null && !authorId.isEmpty()) {
                DocumentReference userRef = db.collection(COL_USERS).document(authorId);
                DocumentSnapshot userSnap = transaction.get(userRef);
                if (userSnap.exists()) {
                    transaction.update(userRef, "score", FieldValue.increment(pointsChange));
                }
            }
            return null;
        }).addOnSuccessListener(aVoid -> listener.onSuccess())
          .addOnFailureListener(listener::onError);
    }

    public void updateUserScore(String userId, int points) {
        if (userId == null) return;
        db.collection(COL_USERS).document(userId).update("score", FieldValue.increment(points));
    }

    public void incrementUserScore(String userId, int points) {
        updateUserScore(userId, points);
    }

    public void addIncident(Incident incident, OnFirestoreTaskComplete listener) {
        db.collection(COL_INCIDENTS).add(incident)
                .addOnSuccessListener(ref -> {
                    incident.setId(ref.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateIncidentDetails(Incident incident, OnFirestoreTaskComplete listener) {
        if (incident.getId() == null) {
            listener.onError(new Exception("ID de l'incident manquant pour la mise à jour"));
            return;
        }
        db.collection(COL_INCIDENTS).document(incident.getId()).set(incident)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void addNotification(NotificationApp notification) {
        db.collection(COL_NOTIFS).add(notification);
    }

    public void addOfficialAlert(NotificationApp alert, OnFirestoreTaskComplete listener) {
        db.collection(COL_OFFICIAL_ALERTS).add(alert)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void addComment(Comment comment, OnFirestoreTaskComplete listener) {
        DocumentReference incidentRef = db.collection(COL_INCIDENTS).document(comment.getIdIncident());
        DocumentReference commentRef = incidentRef.collection(COL_COMMENTS).document();

        db.runTransaction(transaction -> {
                    DocumentSnapshot incidentSnap = transaction.get(incidentRef);
                    String authorId = incidentSnap.getString("idUtilisateur");
                    String categoryName = incidentSnap.getString("nomCategorie");
                    if (categoryName == null) categoryName = "Incident";

                    transaction.update(incidentRef, "commentsCount", FieldValue.increment(1));
                    transaction.set(commentRef, comment);

                    if (authorId != null && !comment.getIdUtilisateur().equals(authorId)) {
                        String senderName = comment.getNomUtilisateur() != null ? comment.getNomUtilisateur() : "Un citoyen";
                        NotificationApp notif = new NotificationApp(
                                "Nouveau commentaire",
                                senderName + " a commenté votre signalement (" + categoryName + ").",
                                "comment"
                        );
                        notif.setIdDestinataire(authorId);
                        notif.setIdIncidentSource(comment.getIdIncident());
                        notif.setNomExpediteur(senderName);
                        transaction.set(db.collection(COL_NOTIFS).document(), notif);
                    }

                    return null;
                }).addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

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
        db.collection(COL_INCIDENTS)
                .whereEqualTo("idUtilisateur", userId)
                .orderBy("dateSignalement", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Incident> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
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

    public void getUser(String uid, OnUserLoadedListener listener) {
        db.collection(COL_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Utilisateur user = doc.toObject(Utilisateur.class);
                        if (user != null) user.setId(doc.getId());
                        listener.onUserLoaded(user);
                    } else { listener.onError(new Exception("Profil inexistant dans la collection " + COL_USERS)); }
                }).addOnFailureListener(listener::onError);
    }

    public void getIncident(String incidentId, OnIncidentLoadedListener listener) {
        db.collection(COL_INCIDENTS).document(incidentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Incident inc = doc.toObject(Incident.class);
                        if (inc != null) inc.setId(doc.getId());
                        listener.onIncidentLoaded(inc);
                    } else { listener.onError(new Exception("Signalement introuvable")); }
                }).addOnFailureListener(listener::onError);
    }

    public ListenerRegistration getNotifications(String userId, OnNotificationsLoadedListener listener) {
        return db.collection(COL_NOTIFS)
                .whereEqualTo("idDestinataire", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (snapshots != null) {
                        listener.onNotificationsLoaded(snapshots.toObjects(NotificationApp.class));
                    }
                });
    }

    public void getCategories(OnCategoriesLoadedListener listener) {
        db.collection(COL_CATEGORIES).orderBy("nomCategorie").get()
                .addOnSuccessListener(snaps -> {
                    if (snaps != null) listener.onCategoriesLoaded(snaps.toObjects(Categorie.class));
                }).addOnFailureListener(listener::onError);
    }

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

    public void toggleLike(String incidentId, String userId) {
        DocumentReference ref = db.collection(COL_INCIDENTS).document(incidentId);
        DocumentReference userLikerRef = db.collection(COL_USERS).document(userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref);
            DocumentSnapshot likerSnap = transaction.get(userLikerRef);

            String authorId = snapshot.getString("idUtilisateur");
            String likerName = likerSnap.getString("nom");
            if (likerName == null) likerName = "Un utilisateur";

            List<String> likedBy = (List<String>) snapshot.get("likedBy");
            if (likedBy == null) likedBy = new ArrayList<>();

            if (likedBy.contains(userId)) {
                likedBy.remove(userId);
                transaction.update(ref, "likedBy", likedBy);
                transaction.update(ref, "likesCount", FieldValue.increment(-1));
            } else {
                likedBy.add(userId);
                transaction.update(ref, "likedBy", likedBy);
                transaction.update(ref, "likesCount", FieldValue.increment(1));

                if (authorId != null && !userId.equals(authorId)) {
                    NotificationApp notif = new NotificationApp(
                            "Nouveau Like",
                            likerName + " a aimé votre signalement.",
                            "like"
                    );
                    notif.setIdDestinataire(authorId);
                    notif.setIdIncidentSource(incidentId);
                    notif.setNomExpediteur(likerName);
                    transaction.set(db.collection(COL_NOTIFS).document(), notif);
                }
            }
            return null;
        });
    }

    public ListenerRegistration getIncidentListener(String incidentId, OnIncidentLoadedListener listener) {
        return db.collection(COL_INCIDENTS).document(incidentId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { listener.onError(e); return; }
                    if (snapshot != null && snapshot.exists()) {
                        Incident inc = snapshot.toObject(Incident.class);
                        if (inc != null) {
                            inc.setId(snapshot.getId());
                            listener.onIncidentLoaded(inc);
                        }
                    }
                });
    }
}

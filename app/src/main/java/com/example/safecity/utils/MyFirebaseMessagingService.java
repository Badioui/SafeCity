package com.example.safecity.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.safecity.MainActivity;
import com.example.safecity.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message reçu de: " + remoteMessage.getFrom());

        // 1. GESTION DES DONNÉES (DATA PAYLOAD)
        // C'est ici que le serveur Node.js envoie les coordonnées "cachées"
        if (remoteMessage.getData().size() > 0) {
            String latStr = remoteMessage.getData().get("lat");
            String lngStr = remoteMessage.getData().get("lng");
            String incidentId = remoteMessage.getData().get("idIncidentSource");

            // On récupère le titre et le corps.
            // Si le serveur les met dans 'notification', on les prend de là.
            // Sinon, on met des valeurs par défaut.
            String title = remoteMessage.getNotification() != null ?
                    remoteMessage.getNotification().getTitle() : "Alerte SafeCity";
            String body = remoteMessage.getNotification() != null ?
                    remoteMessage.getNotification().getBody() : "Nouvel incident signalé.";

            if (latStr != null && lngStr != null) {
                try {
                    double lat = Double.parseDouble(latStr);
                    double lng = Double.parseDouble(lngStr);

                    // Vérification de la distance (5km pour le test, 500m en réel)
                    if (isIncidentClose(lat, lng)) {
                        // IMPORTANT : On passe latStr et lngStr à la méthode sendNotification
                        sendNotification(title, body, latStr, lngStr, incidentId);
                    } else {
                        Log.d(TAG, "Incident ignoré car trop loin.");
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Erreur format GPS", e);
                }
            } else {
                // Pas de GPS dans le message ? Notification simple
                sendNotification(title, body, null, null, incidentId);
            }
        }
        // 2. GESTION NOTIFICATION SIMPLE (Sans data GPS)
        else if (remoteMessage.getNotification() != null) {
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    null, null, null
            );
        }
    }

    /**
     * Vérifie si l'incident est dans un rayon acceptable.
     */
    private boolean isIncidentClose(double incLat, double incLng) {
        SharedPreferences prefs = getSharedPreferences("safe_city_prefs", MODE_PRIVATE);
        float myLat = prefs.getFloat("last_lat", 0);
        float myLng = prefs.getFloat("last_lng", 0);

        // Si on n'a jamais eu de position, on affiche l'alerte par sécurité
        if (myLat == 0 && myLng == 0) return true;

        float[] results = new float[1];
        Location.distanceBetween(myLat, myLng, incLat, incLng, results);

        // Seuil : 5000 mètres (5km) pour faciliter vos tests.
        // Pour la prod, mettez 500.
        return results[0] <= 5000;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nouveau token FCM: " + token);
    }

    // --- MÉTHODE MISE À JOUR ---
    private void sendNotification(String title, String messageBody, String lat, String lng, String incidentId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // C'EST ICI LA CLÉ DU SYSTÈME :
        // On attache les coordonnées et l'ID de l'incident à l'intent.
        // Quand MainActivity s'ouvrira, elle lira ces valeurs.
        if (lat != null && lng != null) {
            intent.putExtra("lat", lat);
            intent.putExtra("lng", lng);
            // On ajoute l'action pour correspondre au filtre du Manifest
            intent.setAction("MainActivity");
        }

        if (incidentId != null && !incidentId.isEmpty()) {
            intent.putExtra("incidentId", incidentId);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "safecity_alerts_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH) // Important pour l'affichage tête haute
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Création du canal de notification (Obligatoire Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Alertes SafeCity",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
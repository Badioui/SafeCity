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

        // 1. LOGIQUE DE FILTRAGE PAR POSITION
        // On vérifie si le message contient des données de coordonnées (lat/lng)
        if (remoteMessage.getData().size() > 0 && remoteMessage.getData().containsKey("lat")) {
            try {
                String latStr = remoteMessage.getData().get("lat");
                String lngStr = remoteMessage.getData().get("lng");

                if (latStr != null && lngStr != null) {
                    double incidentLat = Double.parseDouble(latStr);
                    double incidentLng = Double.parseDouble(lngStr);

                    // Vérifier la distance
                    if (isIncidentClose(incidentLat, incidentLng)) {
                        // C'est proche ! On construit le titre/body
                        String title = remoteMessage.getNotification() != null ?
                                remoteMessage.getNotification().getTitle() : "Alerte Proximité";
                        String body = remoteMessage.getNotification() != null ?
                                remoteMessage.getNotification().getBody() : "Un incident a été signalé près de vous.";

                        sendNotification(title, body);
                    } else {
                        Log.d(TAG, "Incident ignoré car trop loin de la position utilisateur.");
                    }
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Erreur de format des coordonnées dans la notif", e);
            }
        }
        // 2. CAS STANDARD (Pas de coordonnées dans les données)
        // C'est une alerte générale envoyée à tout le monde
        else if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            sendNotification(title, body);
        }
    }

    /**
     * Vérifie si l'incident est dans un rayon acceptable (500m).
     */
    private boolean isIncidentClose(double incLat, double incLng) {
        // 1. Récupérer ma dernière position connue (sauvegardée par MainActivity)
        // ATTENTION : On utilise le même nom de fichier que dans MainActivity ("safe_city_prefs")
        SharedPreferences prefs = getSharedPreferences("safe_city_prefs", MODE_PRIVATE);

        float myLat = prefs.getFloat("last_lat", 0);
        float myLng = prefs.getFloat("last_lng", 0);

        // Si on n'a jamais eu de position (0,0), on affiche l'alerte par sécurité
        if (myLat == 0 && myLng == 0) return true;

        // 2. Créer des objets Location pour le calcul
        Location incidentLoc = new Location("incident");
        incidentLoc.setLatitude(incLat);
        incidentLoc.setLongitude(incLng);

        Location myLoc = new Location("moi");
        myLoc.setLatitude(myLat);
        myLoc.setLongitude(myLng);

        // 3. Calculer la distance en mètres
        float distanceEnMetres = myLoc.distanceTo(incidentLoc);

        Log.d(TAG, "Distance incident: " + distanceEnMetres + "m");

        // 4. Vérifier si c'est inférieur à 500m
        return distanceEnMetres <= 500;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nouveau token FCM: " + token);
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

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
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Alertes SafeCity",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
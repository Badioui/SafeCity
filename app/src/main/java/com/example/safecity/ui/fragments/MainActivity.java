package com.example.safecity.ui.fragments; // Adapter votre nom de package

import androidx.appcompat.app.AppCompatActivity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.example.safecity.R;

public class MainActivity extends AppCompatActivity {

    // ID du canal de notification (pour B5)
    public static final String CHANNEL_ID = "admin_channel";
    private static final String CHANNEL_NAME = "Actions Administrateur";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Utilisation du layout renommé : R.layout.activity
        setContentView(R.layout.activity);

        // Tâche B5: Configurer le framework de notification
        createNotificationChannel();
    }

    /**
     * Tâche B5 : Crée le NotificationChannel.
     * Logique D4 : Importance élevée pour les actions Admin/Autorité.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Importance ÉLEVÉE pour les actions Admin/Autorité (D4)
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    importance);

            channel.setDescription("Notifications pour les actions de haute priorité de l'administrateur.");
            channel.enableLights(true);
            channel.enableVibration(true);

            // Enregistre le canal auprès du système
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
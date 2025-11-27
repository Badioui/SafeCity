package com.example.safecity; // Adapter votre nom de package

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

public class MonFragment extends Fragment {

    // ID unique pour cette notification spécifique
    private static final int NOTIFICATION_ID = 1001;

    public MonFragment() {
        // Constructeur public requis
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate le layout du fragment
        return inflater.inflate(R.layout.fragment_mon, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button sendButton = view.findViewById(R.id.btn_send_notification);
        sendButton.setOnClickListener(v -> sendNotification());
    }

    /**
     * Tâche B6 : Coder la fonction qui déclenche une notification locale.
     * Cette fonction simule l'appel suite à une action Admin/Autorité (D4).
     */
    private void sendNotification() {
        Context context = requireContext();

        // 1. Créer le contenu de la notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                MainActivity.CHANNEL_ID) // Utilise l'ID du canal créé en B5
                .setSmallIcon(R.drawable.ic_notification) // **IMPORTANT : Remplacer par votre icône**
                .setContentTitle("Alerte Admin : Nouvelle Action")
                .setContentText("Un nouvel utilisateur 'Autorité' a été enregistré dans le système.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Ceci est une notification de haute priorité liée à une action Admin/Autorité."))
                .setPriority(NotificationCompat.PRIORITY_HIGH); // Correspond à l'importance du canal

        // 2. Obtenir le NotificationManager
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 3. Afficher la notification
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}
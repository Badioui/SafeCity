package com.example.safecity.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.R;
import com.example.safecity.model.NotificationApp;
import com.example.safecity.ui.adapters.NotificationAdapter;
import com.example.safecity.utils.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private FirestoreRepository firestoreRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. Initialisation avec une liste vide pour éviter les erreurs avant le chargement
        adapter = new NotificationAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // 2. Initialisation du repository
        firestoreRepo = new FirestoreRepository();

        // 3. Chargement des données réelles depuis Firestore
        loadNotifications();
    }

    private void loadNotifications() {
        firestoreRepo.getNotifications(new FirestoreRepository.OnNotificationsLoadedListener() {
            @Override
            public void onNotificationsLoaded(List<NotificationApp> notifications) {
                // On vérifie que le fragment est toujours actif avant de toucher à l'UI
                if (isAdded() && adapter != null) {
                    // Mise à jour de l'adaptateur via la méthode créée à l'étape 1
                    adapter.updateData(notifications);

                    if (notifications.isEmpty()) {
                        Toast.makeText(getContext(), "Aucune notification pour le moment", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Erreur chargement : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
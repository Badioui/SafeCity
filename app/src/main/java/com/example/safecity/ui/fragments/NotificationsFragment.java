package com.example.safecity.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    public interface NotificationNavigationListener {
        void navigateToIncident(String incidentId);
    }

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private NotificationAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private ListenerRegistration notificationListener;
    private String currentUserId;
    private NotificationNavigationListener navigationListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NotificationNavigationListener) {
            navigationListener = (NotificationNavigationListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NotificationNavigationListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_notifications);
        tvEmptyState = view.findViewById(R.id.tv_empty_state_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new NotificationAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        firestoreRepo = new FirestoreRepository();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            Toast.makeText(getContext(), "Veuillez vous connecter pour voir les notifications", Toast.LENGTH_LONG).show();
            updateEmptyState(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        loadNotifications();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void loadNotifications() {
        if (currentUserId == null) return;

        notificationListener = firestoreRepo.getNotifications(currentUserId, new FirestoreRepository.OnNotificationsLoadedListener() {
            @Override
            public void onNotificationsLoaded(List<NotificationApp> notifications) {
                if (isAdded() && adapter != null) {
                    adapter.updateData(notifications);
                    updateEmptyState(notifications.isEmpty());
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Erreur chargement : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState(true);
                }
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNotificationClick(NotificationApp notification) {
        if (notification.getIdIncidentSource() != null && !notification.getIdIncidentSource().isEmpty()) {
            if (navigationListener != null) {
                navigationListener.navigateToIncident(notification.getIdIncidentSource());
            }
        } else {
            Toast.makeText(getContext(), "Cette notification n'est pas liée à un incident.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }
}

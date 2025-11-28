package com.example.safecity.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.R;
import com.example.safecity.model.NotificationApp;
import com.example.safecity.ui.adapters.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;

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

        // Données factices pour la démo (En attendant de relier à Firebase)
        List<NotificationApp> fakeNotifs = new ArrayList<>();
        fakeNotifs.add(new NotificationApp("Bienvenue !", "Ravi de vous voir sur SafeCity.", "info"));
        fakeNotifs.add(new NotificationApp("Incident Validé", "Votre signalement 'Accident' a été validé.", "validation"));
        fakeNotifs.add(new NotificationApp("Alerte Proximité", "Travaux signalés rue de la Paix.", "alerte"));

        adapter = new NotificationAdapter(getContext(), fakeNotifs);
        recyclerView.setAdapter(adapter);
    }
}
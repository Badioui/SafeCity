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
import com.example.safecity.dao.IncidentDAO;
import com.example.safecity.model.Incident;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.AppExecutors;
import com.example.safecity.utils.AuthManager;

import java.util.List;

public class MyIncidentsFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private IncidentDAO incidentDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_incidents, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.incidents_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadIncidents();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadIncidents();
    }

    // =========================================================
    // A. CHARGEMENT DES INCIDENTS (Avec AppExecutors)
    // =========================================================
    private void loadIncidents() {
        long currentUserId = AuthManager.getCurrentUserId(getContext());
        if (currentUserId == -1) return;

        AppExecutors.getInstance().diskIO().execute(() -> {
            incidentDAO = new IncidentDAO(getContext());
            incidentDAO.open();
            List<Incident> incidents = incidentDAO.getIncidentsByUtilisateur(currentUserId);
            incidentDAO.close();

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isAdded() && getActivity() != null) {
                    if (incidents != null && !incidents.isEmpty()) {
                        IncidentAdapter adapter = new IncidentAdapter(getContext(), incidents, this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        recyclerView.setAdapter(null);
                        // Optionnel : Afficher un texte "Aucun incident"
                    }
                }
            });
        });
    }

    // =========================================================
    // IMPLÉMENTATION DES ACTIONS
    // =========================================================

    @Override
    public void onMapClick(Incident incident) {
        Toast.makeText(getContext(), "Localisation : " + incident.getLatitude() + ", " + incident.getLongitude(), Toast.LENGTH_SHORT).show();
        // TODO: Redirection vers la Map
    }

    @Override
    public void onEditClick(Incident incident) {
        Toast.makeText(getContext(), "Modification à venir pour l'ID : " + incident.getId(), Toast.LENGTH_SHORT).show();
    }

    // =========================================================
    // B. SUPPRESSION (Avec AppExecutors)
    // =========================================================
    @Override
    public void onDeleteClick(Incident incident) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            // On s'assure que le DAO est instancié pour ce thread
            incidentDAO = new IncidentDAO(getContext());
            incidentDAO.open();
            incidentDAO.deleteIncident(incident.getId());
            incidentDAO.close();

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getContext(), "Incident supprimé !", Toast.LENGTH_SHORT).show();
                    loadIncidents(); // Recharger la liste
                }
            });
        });
    }
}
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

import java.util.List;

public class MyIncidentsFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private IncidentDAO incidentDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // On charge le layout XML qui contient le RecyclerView
        return inflater.inflate(R.layout.fragment_my_incidents, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialisation de la liste
        recyclerView = view.findViewById(R.id.incidents_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Chargement des données
        loadIncidents();
    }

    @Override
    public void onResume() {
        super.onResume();
        // On recharge la liste quand on revient sur l'écran (au cas où il y a eu des ajouts)
        loadIncidents();
    }

    private void loadIncidents() {
        // Sécurité pour éviter les crashs si le fragment n'est pas attaché
        if (getContext() == null) return;

        new Thread(() -> {
            incidentDAO = new IncidentDAO(getContext());
            incidentDAO.open();

            // Récupération de tous les incidents
            // Note: Plus tard, vous pourrez filtrer par utilisateur avec : incidentDAO.getIncidentsByUtilisateur(1);
            List<Incident> incidents = incidentDAO.getAllIncidents();

            incidentDAO.close();

            // Mise à jour de l'interface utilisateur (UI) sur le thread principal
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // ✅ Création de l'adaptateur avec le contexte, la liste, et le listener (this)
                    if (incidents != null) {
                        IncidentAdapter adapter = new IncidentAdapter(getContext(), incidents, this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Toast.makeText(getContext(), "Aucun incident trouvé.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    // =========================================================
    // IMPLÉMENTATION DES ACTIONS (Interface IncidentAdapter)
    // =========================================================

    @Override
    public void onMapClick(Incident incident) {
        // Action quand on clique sur le bouton "Carte" d'un item
        Toast.makeText(getContext(), "Localisation : " + incident.getLatitude() + ", " + incident.getLongitude(), Toast.LENGTH_SHORT).show();
        // TODO: Plus tard, rediriger vers MapFragment avec ces coordonnées
    }

    @Override
    public void onEditClick(Incident incident) {
        // Action quand on clique sur le bouton "Modifier"
        Toast.makeText(getContext(), "Modification à venir pour l'ID : " + incident.getId(), Toast.LENGTH_SHORT).show();
        // TODO: Ouvrir SignalementFragment en mode édition
    }

    @Override
    public void onDeleteClick(Incident incident) {
        // Action quand on clique sur "Supprimer" -> Suppression réelle en BDD
        new Thread(() -> {
            incidentDAO = new IncidentDAO(getContext());
            incidentDAO.open();
            incidentDAO.deleteIncident(incident.getId());
            incidentDAO.close();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Incident supprimé !", Toast.LENGTH_SHORT).show();
                    // Recharger la liste pour faire disparaître l'élément supprimé
                    loadIncidents();
                });
            }
        }).start();
    }
}
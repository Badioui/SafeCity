/**
 * ATTENTION: FICHIER DE TEST / ARTEFACT D'INTÉGRATION TEMPORAIRE
 * * ⚠️ Ce fichier (MyIncidentsFragment.java) NE REPRÉSENTE PAS la version finale
 * et Complète de la tâche C4.
 * * Ce code a été créé et modifié UNIQUEMENT dans le but de résoudre les
 * problèmes de compilation (erreurs d'import, IDs manquants,
 * implémentation d'interfaces abstraites) afin de lancer l'application (Tâche C1).
 * * Il manque :
 * 1. La gestion réelle du rôle pour les boutons Modifier/Supprimer (C5).
 * 2. La gestion du passage des coordonnées à la Map (B1/B3).
 * 3. Le traitement asynchrone pour l'appel au DAO (A3).
 * 4. L'affichage des images (Chargement via URL).
 * * Ce fichier doit être révisé et complété avant le rendu final.
 */
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
import com.example.safecity.dao.IncidentDAO;
import com.example.safecity.model.Incident;
import com.example.safecity.ui.adapters.IncidentAdapter; // L'adaptateur corrigé

import java.util.List;

// L'implémentation de l'interface est maintenant complète
public class MyIncidentsFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private IncidentDAO incidentDAO;
    private long currentUserId = 1; // ID utilisateur simulé (A4)

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

    private void loadIncidents() {
        if (getContext() == null) return;

        incidentDAO = new IncidentDAO(getContext());
        incidentDAO.open();

        List<Incident> incidents = incidentDAO.getIncidentsByUtilisateur(currentUserId);

        incidentDAO.close();

        // Le constructeur prend seulement la liste et le listener (this)
        IncidentAdapter adapter = new IncidentAdapter(incidents, this);
        recyclerView.setAdapter(adapter);
    }

    // =========================================================
    // IMPLÉMENTATION DES MÉTHODES DE L'INTERFACE OnIncidentActionListener
    // =========================================================

    // 1. Gère le clic sur le bouton Map (CORRECTION du nom de méthode)
    @Override
    public void onMapClick(Incident incident) {
        // Logique de navigation B1/B3: centrer la carte sur les coordonnées
        if (incident.getLatitude() != 0.0 || incident.getLongitude() != 0.0) {
            System.out.println("Naviguer vers la carte aux coordonnées: " + incident.getLatitude() + ", " + incident.getLongitude());
        }
    }

    // 2. Gère le clic sur le bouton Edit (MÉTHODE MANQUANTE - Tâche C5)
    @Override
    public void onEditClick(Incident incident) {
        // Logique pour ouvrir l'écran de modification de l'incident
        System.out.println("Ouvrir la modification pour l'incident ID: " + incident.getId());
    }

    // 3. Gère le clic sur le bouton Delete (MÉTHODE MANQUANTE - Tâche C5/D4)
    @Override
    public void onDeleteClick(Incident incident) {
        // Logique pour demander confirmation de suppression et appeler DAO A3
        System.out.println("Confirmation de suppression pour l'incident ID: " + incident.getId());
    }
}
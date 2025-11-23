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
import com.example.safecity.model.Incident;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.FirestoreRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyIncidentsFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    // Remplacement du DAO par le Repository Firestore
    private FirestoreRepository firestoreRepo;
    private IncidentAdapter adapter;

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

        // Initialisation de l'adapter vide pour éviter les erreurs d'affichage
        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Initialisation du Repo
        firestoreRepo = new FirestoreRepository();

        loadIncidents();
    }

    @Override
    public void onResume() {
        super.onResume();
        // On recharge la liste quand on revient sur l'écran (ex: après une modif)
        loadIncidents();
    }

    private void loadIncidents() {
        // Récupération de l'utilisateur connecté via Firebase Auth
        // Cela correspond à l'ID (String) utilisé lors de la création de l'incident
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Appel Firestore asynchrone
        firestoreRepo.getMyIncidents(userId, new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (isAdded() && getActivity() != null) {
                    if (incidents != null && !incidents.isEmpty()) {
                        // Mise à jour de l'adapter existant ou création d'un nouveau
                        adapter = new IncidentAdapter(getContext(), incidents, MyIncidentsFragment.this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        // Liste vide
                        adapter.updateData(new ArrayList<>());
                        Toast.makeText(getContext(), "Aucun incident signalé.", Toast.LENGTH_SHORT).show();
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

    // =========================================================
    // IMPLEMENTATION DES ACTIONS (Interface Adapter)
    // =========================================================

    @Override
    public void onMapClick(Incident incident) {
        // TODO: Redirection vers l'onglet Carte avec centrage sur l'incident
        Toast.makeText(getContext(), "Localisation : " + incident.getLatitude() + ", " + incident.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditClick(Incident incident) {
        // Navigation vers le fragment Signalement en mode édition
        // On utilise la méthode newInstance(String) créée à l'étape précédente
        Fragment fragment = SignalementFragment.newInstance(incident.getId());

        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment) // Assurez-vous que l'ID du container est bon
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        // Utilisation de la suppression Firestore
        firestoreRepo.deleteIncident(incident.getId(), new FirestoreRepository.OnFirestoreTaskComplete() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Incident supprimé avec succès", Toast.LENGTH_SHORT).show();
                    loadIncidents(); // Recharger la liste pour voir la disparition
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
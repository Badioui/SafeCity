package com.example.safecity.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.MainActivity;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.ui.adapters.IncidentAdapter;

import com.google.android.material.chip.ChipGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private IncidentAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private ListenerRegistration firestoreListener;

    private ChipGroup chipGroup;
    private List<Incident> allIncidents = new ArrayList<>();

    private String searchQuery = null;
    private String myUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            searchQuery = getArguments().getString("search_query");
        }
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_home);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        chipGroup = view.findViewById(R.id.chip_group_filters_home);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firestoreRepo = new FirestoreRepository();

        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        if (searchQuery != null) {
            Toast.makeText(getContext(), "Recherche : " + searchQuery, Toast.LENGTH_SHORT).show();
        }

        // --- GESTION UTILISATEUR & SIMULATION ADMIN ---
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            myUserId = fbUser.getUid();

            // ASTUCE POUR LA DÉMO :
            // Si l'email contient le mot "admin", on force le rôle, peu importe la base de données.
            if (fbUser.getEmail() != null && fbUser.getEmail().contains("admin")) {

                // On notifie l'adaptateur qu'on est ADMIN
                adapter.setCurrentUser(myUserId, "admin");

                // Petit message discret pour confirmer que le hack a fonctionné
                // Toast.makeText(getContext(), "Mode Admin Activé (Démo)", Toast.LENGTH_SHORT).show();

            } else {
                // COMPORTEMENT NORMAL (Récupération depuis Firestore)
                firestoreRepo.getUser(myUserId, new FirestoreRepository.OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(Utilisateur utilisateur) {
                        if (utilisateur != null && isAdded()) {
                            adapter.setCurrentUser(myUserId, utilisateur.getIdRole());
                        }
                    }
                    @Override
                    public void onError(Exception e) { }
                });
            }
        }

        // --- LISTENER DES CHIPS (FILTRES) ---
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            applyFilters();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        loadData();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    private void loadData() {
        firestoreListener = firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (!isAdded() || getActivity() == null) return;

                allIncidents = incidents != null ? incidents : new ArrayList<>();
                applyFilters();
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilters() {
        List<Incident> filteredList = new ArrayList<>();
        String queryLower = (searchQuery != null) ? searchQuery.toLowerCase() : null;

        String categoryFilter = null;
        int checkedId = chipGroup.getCheckedChipId();

        if (checkedId == R.id.chip_accident) categoryFilter = "Accident";
        else if (checkedId == R.id.chip_vol) categoryFilter = "Vol";
        else if (checkedId == R.id.chip_travaux) categoryFilter = "Travaux";
        else if (checkedId == R.id.chip_incendie) categoryFilter = "Incendie";

        for (Incident i : allIncidents) {
            boolean matchesSearch = true;
            boolean matchesCategory = true;

            if (queryLower != null) {
                boolean inDesc = i.getDescription() != null && i.getDescription().toLowerCase().contains(queryLower);
                boolean inCat = i.getNomCategorie() != null && i.getNomCategorie().toLowerCase().contains(queryLower);
                if (!inDesc && !inCat) matchesSearch = false;
            }

            if (categoryFilter != null) {
                if (i.getNomCategorie() == null || !i.getNomCategorie().contains(categoryFilter)) {
                    matchesCategory = false;
                }
            }

            if (matchesSearch && matchesCategory) {
                filteredList.add(i);
            }
        }

        adapter.updateData(filteredList);

        if (filteredList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            if (categoryFilter != null) tvEmptyState.setText("Aucun " + categoryFilter + " trouvé.");
            else if (searchQuery != null) tvEmptyState.setText("Aucun résultat pour \"" + searchQuery + "\"");
            else tvEmptyState.setText("Aucun incident.");
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onValidateClick(Incident incident) {
        new AlertDialog.Builder(getContext())
                .setTitle("Validation")
                .setMessage("Confirmer la prise en charge de cet incident ? Il passera au statut 'Traité'.")
                .setPositiveButton("Oui", (dialog, which) -> {
                    firestoreRepo.updateIncidentStatus(incident.getId(), "Traité", new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Incident validé avec succès !", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onEditClick(Incident incident) {
        Toast.makeText(getContext(), "Fonctionnalité d'édition à venir", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        new AlertDialog.Builder(getContext())
                .setTitle("Suppression")
                .setMessage("Voulez-vous vraiment supprimer ce signalement ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    firestoreRepo.deleteIncident(incident.getId(), incident.getPhotoUrl(), new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Incident supprimé.", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onMapClick(Incident incident) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToMapAndFocus(incident.getLatitude(), incident.getLongitude());
        }
    }
}
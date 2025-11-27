package com.example.safecity.ui.fragments;

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
import com.example.safecity.model.Incident;
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private IncidentAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private ListenerRegistration firestoreListener;

    // Variable pour stocker la requête de recherche
    private String searchQuery = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // On récupère la requête si elle existe
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new IncidentAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        firestoreRepo = new FirestoreRepository();

        // Si on est en mode recherche, on peut changer le titre ou afficher un Toast
        if (searchQuery != null) {
            Toast.makeText(getContext(), "Résultats pour : " + searchQuery, Toast.LENGTH_SHORT).show();
        }
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

                List<Incident> displayList;

                // --- LOGIQUE DE FILTRAGE (RECHERCHE) ---
                if (searchQuery != null && !searchQuery.isEmpty() && incidents != null) {
                    displayList = new ArrayList<>();
                    String queryLower = searchQuery.toLowerCase();

                    for (Incident i : incidents) {
                        boolean matchesDesc = i.getDescription() != null && i.getDescription().toLowerCase().contains(queryLower);
                        boolean matchesCat = i.getNomCategorie() != null && i.getNomCategorie().toLowerCase().contains(queryLower);

                        // On garde l'incident si la description OU la catégorie contient le mot
                        if (matchesDesc || matchesCat) {
                            displayList.add(i);
                        }
                    }
                } else {
                    // Pas de recherche, on affiche tout
                    displayList = incidents;
                }

                // --- MISE A JOUR UI ---
                if (displayList != null && !displayList.isEmpty()) {
                    adapter.updateData(displayList);
                    tvEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    if (searchQuery != null) {
                        tvEmptyState.setText("Aucun résultat trouvé pour \"" + searchQuery + "\"");
                    } else {
                        tvEmptyState.setText("Aucun incident signalé pour le moment.");
                    }
                    tvEmptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur chargement : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
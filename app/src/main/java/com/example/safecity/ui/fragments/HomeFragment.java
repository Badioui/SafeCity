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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private IncidentAdapter adapter;

    // Remplacement de IncidentDAO par FirestoreRepository
    private FirestoreRepository firestoreRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_home);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        // Configuration liste verticale
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Adapter vide au départ
        adapter = new IncidentAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Initialisation du repository Firestore
        firestoreRepo = new FirestoreRepository();

        // Lancer le chargement (écoute en temps réel)
        loadData();
    }

    // Note : Avec Firestore Realtime, pas besoin de onResume() pour recharger,
    // le listener mis en place dans loadData() reste actif tant que le fragment vit.

    private void loadData() {
        // Appel à la méthode temps réel du repository
        firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                // Vérifier si le fragment est toujours attaché pour éviter les crashs
                if (!isAdded() || getActivity() == null) return;

                if (incidents != null && !incidents.isEmpty()) {
                    adapter.updateData(incidents);
                    tvEmptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyState.setText("Aucun incident signalé pour le moment.");
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
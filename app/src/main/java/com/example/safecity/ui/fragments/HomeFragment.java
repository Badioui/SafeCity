package com.example.safecity.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private IncidentAdapter adapter;
    private IncidentDAO incidentDAO;

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

        incidentDAO = new IncidentDAO(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recharger la liste à chaque fois qu'on revient sur cet écran
        loadData();
    }

    private void loadData() {
        com.example.safecity.utils.AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                incidentDAO.open();
                List<Incident> incidents = incidentDAO.getAllIncidents();
                incidentDAO.close();

                // DEBUG : Afficher dans les logs le nombre d'incidents trouvés
                android.util.Log.d("DEBUG_HOME", "Nombre d'incidents trouvés : " + (incidents != null ? incidents.size() : "NULL"));

                com.example.safecity.utils.AppExecutors.getInstance().mainThread().execute(() -> {
                    if (isAdded() && getActivity() != null) {
                        if (incidents != null && !incidents.isEmpty()) {
                            adapter.updateData(incidents);
                            tvEmptyState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            tvEmptyState.setText("Aucun incident trouvé en base de données."); // Message plus précis
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace(); // Afficher l'erreur dans le Logcat
            }
        });
    }
}
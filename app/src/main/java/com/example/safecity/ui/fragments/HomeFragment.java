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
        new Thread(() -> {
            incidentDAO.open();
            // On récupère TOUS les signalements
            List<Incident> incidents = incidentDAO.getAllIncidents();
            incidentDAO.close();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (incidents != null && !incidents.isEmpty()) {
                        adapter.updateData(incidents);
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }
}
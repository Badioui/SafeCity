package com.example.safecity.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.LoginActivity; // Assurez-vous d'avoir créé LoginActivity
import com.example.safecity.R;
import com.example.safecity.dao.IncidentDAO;
import com.example.safecity.dao.UserDAO;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.AppExecutors;
import com.example.safecity.utils.AuthManager;

import java.util.List;

public class ProfileFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private TextView tvName, tvEmail;
    private Button btnLogout;
    private RecyclerView recyclerView;
    private IncidentDAO incidentDAO;
    private UserDAO userDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        recyclerView = view.findViewById(R.id.recycler_profile_incidents);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Gestion Déconnexion
        btnLogout.setOnClickListener(v -> {
            AuthManager.logout(getContext());
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadProfileData();
    }

    private void loadProfileData() {
        long userId = AuthManager.getCurrentUserId(getContext());
        if (userId == -1) return; // Pas connecté

        AppExecutors.getInstance().diskIO().execute(() -> {
            // 1. Charger Infos User
            userDAO = new UserDAO(getContext());
            userDAO.open();
            Utilisateur user = userDAO.getUserById(userId);
            userDAO.close();

            // 2. Charger Ses Incidents
            incidentDAO = new IncidentDAO(getContext());
            incidentDAO.open();
            List<Incident> myIncidents = incidentDAO.getIncidentsByUtilisateur(userId);
            incidentDAO.close();

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isAdded() && getActivity() != null) {
                    // Mise à jour Infos
                    if (user != null) {
                        tvName.setText(user.getNom());
                        tvEmail.setText(user.getEmail());
                    }

                    // Mise à jour Liste
                    if (myIncidents != null && !myIncidents.isEmpty()) {
                        IncidentAdapter adapter = new IncidentAdapter(getContext(), myIncidents, this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        // Optionnel : Afficher un message "Aucun post"
                    }
                }
            });
        });
    }

    // --- GESTION DES BOUTONS (Modifier / Supprimer) ---
    @Override
    public void onMapClick(Incident incident) {
        // Rediriger vers la Map (A faire plus tard si besoin)
    }

    @Override
    public void onEditClick(Incident incident) {
        // Ouvrir SignalementFragment en mode édition
        SignalementFragment fragment = SignalementFragment.newInstance(incident.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            incidentDAO = new IncidentDAO(getContext());
            incidentDAO.open();
            incidentDAO.deleteIncident(incident.getId());
            incidentDAO.close();

            // Recharger la page
            loadProfileData();
        });
    }
}
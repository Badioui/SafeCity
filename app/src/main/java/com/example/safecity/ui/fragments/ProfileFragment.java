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

import com.example.safecity.LoginActivity;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.FirestoreRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private TextView tvName, tvEmail;
    private Button btnLogout;
    private RecyclerView recyclerView;

    // Remplacement des DAO par les outils Firebase
    private FirestoreRepository firestoreRepo;
    private FirebaseAuth auth;

    private IncidentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialisation UI
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        recyclerView = view.findViewById(R.id.recycler_profile_incidents);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialisation Adapter vide
        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance();
        firestoreRepo = new FirestoreRepository();

        // Gestion Déconnexion Firebase
        btnLogout.setOnClickListener(v -> {
            auth.signOut(); // Déconnexion Firebase

            // Retour au Login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadProfileData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recharger les données au retour (cas suppression ou modification)
        loadProfileData();
    }

    private void loadProfileData() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            // Sécurité : si pas d'utilisateur, on force la déconnexion ou on affiche rien
            return;
        }

        // 1. Afficher les infos utilisateur (Disponibles immédiatement via FirebaseUser)
        String name = user.getDisplayName();
        String email = user.getEmail();

        tvName.setText(name != null && !name.isEmpty() ? name : "Utilisateur");
        tvEmail.setText(email != null ? email : "Email non disponible");

        // 2. Charger les incidents de cet utilisateur via Firestore
        firestoreRepo.getMyIncidents(user.getUid(), new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (isAdded() && getActivity() != null) {
                    if (incidents != null && !incidents.isEmpty()) {
                        adapter = new IncidentAdapter(getContext(), incidents, ProfileFragment.this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateData(new ArrayList<>());
                        // Optionnel : Afficher un Toast ou View vide
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

    // --- GESTION DES BOUTONS (Modifier / Supprimer) ---

    @Override
    public void onMapClick(Incident incident) {
        Toast.makeText(getContext(), "Localisation : " + incident.getLatitude() + ", " + incident.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditClick(Incident incident) {
        // Ouvrir SignalementFragment en mode édition avec l'ID String
        Fragment fragment = SignalementFragment.newInstance(incident.getId());

        // Note: Assurez-vous que R.id.fragment_container correspond bien à l'ID dans votre MainActivity
        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        // Suppression via Firestore
        firestoreRepo.deleteIncident(incident.getId(), new FirestoreRepository.OnFirestoreTaskComplete() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Incident supprimé !", Toast.LENGTH_SHORT).show();
                    loadProfileData(); // Recharger la liste
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Erreur suppression : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
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
import com.example.safecity.model.Utilisateur; // Import Utilisateur
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.FirestoreRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private TextView tvName, tvEmail, tvScore; // Ajout tvScore
    private Button btnLogout;
    private RecyclerView recyclerView;
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

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvScore = view.findViewById(R.id.tv_profile_score); // Initialisation
        btnLogout = view.findViewById(R.id.btn_logout);
        recyclerView = view.findViewById(R.id.recycler_profile_incidents);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // On initialise l'adapter
        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        firestoreRepo = new FirestoreRepository();

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            }
        });

        loadProfileData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // 1. Infos de base Auth
        String name = user.getDisplayName();
        String email = user.getEmail();

        tvName.setText(name != null && !name.isEmpty() ? name : "Utilisateur");
        tvEmail.setText(email != null ? email : "Email masqué");

        // 2. Récupérer les infos détaillées (Score, Grade, Rôle) depuis Firestore
        firestoreRepo.getUser(user.getUid(), new FirestoreRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(Utilisateur utilisateur) {
                if (isAdded() && utilisateur != null) {
                    // Mise à jour de l'affichage du score
                    tvScore.setText("Score : " + utilisateur.getScore() + " pts (" + utilisateur.getGrade() + ")");

                    // On met à jour l'adaptateur avec le rôle pour activer les boutons d'édition/suppression
                    adapter.setCurrentUser(user.getUid(), utilisateur.getIdRole());
                }
            }

            @Override
            public void onError(Exception e) {
                // Erreur silencieuse, on garde les valeurs par défaut
            }
        });

        // 3. Récupérer mes incidents
        firestoreRepo.getMyIncidents(user.getUid(), new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (isAdded() && getActivity() != null) {
                    if (incidents != null && !incidents.isEmpty()) {
                        adapter.updateData(incidents);
                        // S'assurer que l'adapter est bien attaché
                        if (recyclerView.getAdapter() == null) recyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateData(new ArrayList<>());
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

    // --- ACTIONS DE L'ADAPTATEUR ---

    @Override
    public void onMapClick(Incident incident) {
        // Optionnel
    }

    @Override
    public void onEditClick(Incident incident) {
        // Ouvrir le fragment de signalement en mode édition (avec l'ID)
        Fragment fragment = SignalementFragment.newInstance(incident.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        // Suppression avec gestion de l'image
        firestoreRepo.deleteIncident(incident.getId(), incident.getPhotoUrl(), new FirestoreRepository.OnFirestoreTaskComplete() {
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

    @Override
    public void onValidateClick(Incident incident) {
        // Dans le profil, le bouton valider n'est généralement pas visible (sauf si admin regarde ses propres posts)
        // On peut laisser vide ou mettre un Toast
    }
}
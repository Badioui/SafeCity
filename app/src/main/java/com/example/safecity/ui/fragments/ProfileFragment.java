package com.example.safecity.ui.fragments;

import android.app.Dialog; // Import nécessaire pour le Dialog image
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton; // Import pour le bouton fermer
import android.widget.ImageView; // Import pour l'image plein écran
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide
import com.example.safecity.LoginActivity;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.FirestoreRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private TextView tvName, tvEmail, tvScore;
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
        tvScore = view.findViewById(R.id.tv_profile_score);
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
                    tvScore.setText("Score : " + utilisateur.getScore() + " pts (" + utilisateur.getGrade() + ")");
                    adapter.setCurrentUser(user.getUid(), utilisateur.getIdRole());
                }
            }

            @Override
            public void onError(Exception e) {
                // Erreur silencieuse
            }
        });

        // 3. Récupérer mes incidents
        firestoreRepo.getMyIncidents(user.getUid(), new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (isAdded() && getActivity() != null) {
                    if (incidents != null && !incidents.isEmpty()) {
                        adapter.updateData(incidents);
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
        // Optionnel, peut rester vide ou afficher un Toast
    }

    @Override
    public void onEditClick(Incident incident) {
        Fragment fragment = SignalementFragment.newInstance(incident.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Suppression")
                .setMessage("Voulez-vous vraiment supprimer cet incident ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    performDelete(incident);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // --- CORRECTION : Ajout de l'implémentation manquante ---
    @Override
    public void onImageClick(Incident incident) {
        if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
            showFullImageDialog(incident.getPhotoUrl());
        }
    }

    // --- Méthode Helper pour afficher le Dialog Plein Écran (copiée de HomeFragment) ---
    private void showFullImageDialog(String imageUrl) {
        if (getContext() == null) return;

        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullImageView = dialog.findViewById(R.id.img_full_screen);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_dialog);

        Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .placeholder(R.drawable.ic_incident_placeholder)
                .into(fullImageView);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onValidateClick(Incident incident) {
        // Non utilisé dans le profil
    }

    private void performDelete(Incident incident) {
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
}
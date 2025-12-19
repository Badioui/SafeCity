package com.example.safecity.ui.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.safecity.LoginActivity;
import com.example.safecity.MainActivity;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.FirestoreRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment de profil permettant à l'utilisateur de gérer ses informations,
 * de voir ses signalements et de mettre à jour son identité visuelle.
 */
public class ProfileFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    // --- VUES ---
    private ImageView imgProfile;
    private TextView tvName, tvEmail, tvScore;
    private Button btnLogout;
    private FloatingActionButton btnEditProfile;
    private RecyclerView recyclerView;

    // --- LOGIQUE ---
    private FirestoreRepository firestoreRepo;
    private FirebaseAuth auth;
    private IncidentAdapter adapter;

    // --- GESTION IMAGE ---
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation du sélecteur de galerie
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // On affiche immédiatement l'image locale pour un feedback rapide
                Glide.with(this).load(uri).circleCrop().into(imgProfile);
                uploadProfileImage(uri);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgProfile = view.findViewById(R.id.img_profile);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvScore = view.findViewById(R.id.tv_profile_score);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        recyclerView = view.findViewById(R.id.recycler_profile_incidents);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        firestoreRepo = new FirestoreRepository();

        // TOUCHER L'IMAGE POUR CHANGER
        imgProfile.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // TOUCHER LE NOM POUR CHANGER AUSSI (Plus intuitif)
        tvName.setOnClickListener(v -> showEditNameDialog());

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditNameDialog());
        }

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

        tvEmail.setText(user.getEmail());

        firestoreRepo.getUser(user.getUid(), new FirestoreRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(Utilisateur utilisateur) {
                if (isAdded() && utilisateur != null) {
                    tvName.setText(utilisateur.getNom() != null ? utilisateur.getNom() : "Citoyen");
                    tvScore.setText("Score : " + utilisateur.getScore() + " pts (" + utilisateur.getGrade() + ")");

                    // Correction Cache Glide : On utilise une signature (timestamp) pour forcer le refresh
                    Glide.with(ProfileFragment.this)
                            .load(utilisateur.getPhotoProfilUrl())
                            .placeholder(R.drawable.ic_profile)
                            .signature(new ObjectKey(System.currentTimeMillis())) // Force le rafraîchissement
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .circleCrop()
                            .into(imgProfile);

                    adapter.setCurrentUser(user.getUid(), utilisateur.getIdRole());
                }
            }
            @Override
            public void onError(Exception e) {}
        });

        firestoreRepo.getMyIncidents(user.getUid(), new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (isAdded()) adapter.updateData(incidents != null ? incidents : new ArrayList<>());
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void uploadProfileImage(Uri imageUri) {
        if (imageUri == null || getContext() == null) return;

        String userId = auth.getCurrentUser().getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + userId + ".jpg");

        Toast.makeText(getContext(), "Téléchargement de la photo...", Toast.LENGTH_SHORT).show();

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateProfileLinks(uri.toString(), uri);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur upload : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileLinks(String urlString, Uri uriObj) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("utilisateurs").document(user.getUid())
                .update("photoProfilUrl", urlString)
                .addOnSuccessListener(aVoid -> {
                    // On met à jour le profil Firebase Auth avec l'URL publique, pas l'URI locale
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setPhotoUri(Uri.parse(urlString))
                            .build();

                    user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                        updateAllUserIncidents(user.getUid(), urlString);
                    });
                });
    }

    /**
     * Met à jour rétroactivement l'avatar sur tous les signalements de l'utilisateur.
     */
    private void updateAllUserIncidents(String userId, String newPhotoUrl) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("incidents")
                .whereEqualTo("idUtilisateur", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        loadProfileData();
                        return;
                    }

                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "auteurPhotoUrl", newPhotoUrl);
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Profil mis à jour !", Toast.LENGTH_SHORT).show();
                            loadProfileData();
                        }
                    });
                });
    }

    private void showEditNameDialog() {
        if (getContext() == null) return;

        FrameLayout container = new FrameLayout(getContext());
        final EditText input = new EditText(getContext());
        input.setHint("Entrez votre nom");
        input.setText(tvName.getText().toString());
        input.setSelection(input.getText().length()); // Place le curseur à la fin

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        int marginH = (int) (24 * getResources().getDisplayMetrics().density);
        int marginV = (int) (12 * getResources().getDisplayMetrics().density);
        params.setMargins(marginH, marginV, marginH, marginV);
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Modifier mon nom")
                .setView(container)
                .setPositiveButton("Enregistrer", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateProfileName(newName);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateProfileName(String newName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Toast.makeText(getContext(), "Modification du nom...", Toast.LENGTH_SHORT).show();

        FirebaseFirestore.getInstance().collection("utilisateurs").document(user.getUid())
                .update("nom", newName)
                .addOnSuccessListener(aVoid -> {
                    UserProfileChangeRequest updates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build();

                    user.updateProfile(updates).addOnCompleteListener(task -> {
                        tvName.setText(newName);
                        updateNameInAllIncidents(user.getUid(), newName);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Met à jour rétroactivement le nom de l'auteur sur tous ses signalements.
     */
    private void updateNameInAllIncidents(String userId, String newName) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("incidents")
                .whereEqualTo("idUtilisateur", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        loadProfileData();
                        return;
                    }

                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot doc : querySnapshot) {
                        batch.update(doc.getReference(), "nomUtilisateur", newName);
                    }
                    batch.commit().addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Nom actualisé partout !", Toast.LENGTH_SHORT).show();
                            loadProfileData();
                        }
                    });
                });
    }

    @Override
    public void onMapClick(Incident incident) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToMapAndFocus(incident.getLatitude(), incident.getLongitude());
        }
    }

    @Override
    public void onEditClick(Incident incident) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, SignalementFragment.newInstance(incident.getId()))
                .addToBackStack(null).commit();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer ?")
                .setMessage("Voulez-vous supprimer ce signalement ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (d, w) -> firestoreRepo.deleteIncident(incident.getId(), incident.getPhotoUrl(), new FirestoreRepository.OnFirestoreTaskComplete() {
                    @Override public void onSuccess() {
                        Toast.makeText(getContext(), "Supprimé.", Toast.LENGTH_SHORT).show();
                        loadProfileData();
                    }
                    @Override public void onError(Exception e) {
                        Toast.makeText(getContext(), "Erreur suppression.", Toast.LENGTH_SHORT).show();
                    }
                }))
                .setNegativeButton("Annuler", null).show();
    }

    @Override
    public void onImageClick(Incident incident) {
        if (incident.getPhotoUrl() != null) showFullImageDialog(incident.getPhotoUrl());
    }

    private void showFullImageDialog(String imageUrl) {
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);
        ImageView img = dialog.findViewById(R.id.img_full_screen);
        ImageButton close = dialog.findViewById(R.id.btn_close_dialog);
        Glide.with(this).load(imageUrl).fitCenter().into(img);
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onValidateClick(Incident incident) {
        // Réservé aux autorités
    }

    @Override
    public void onCommentClick(Incident incident) {
        // Ouverture du BottomSheet de commentaires
        CommentFragment bottomSheet = CommentFragment.newInstance(incident.getId());
        bottomSheet.show(getParentFragmentManager(), "CommentBottomSheet");
    }
}
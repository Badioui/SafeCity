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

    // Variable pour stocker la requête de recherche
    private String searchQuery = null;
    private String myUserId;

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

        firestoreRepo = new FirestoreRepository();

        // Initialiser l'adaptateur avec 'this' comme listener pour les clics
        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Si on est en mode recherche, on peut changer le titre ou afficher un Toast
        if (searchQuery != null) {
            Toast.makeText(getContext(), "Résultats pour : " + searchQuery, Toast.LENGTH_SHORT).show();
        }

        // --- GESTION UTILISATEUR & RÔLES ---
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            myUserId = fbUser.getUid();
            // Récupérer les infos complètes (Rôle) depuis Firestore
            firestoreRepo.getUser(myUserId, new FirestoreRepository.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    if (utilisateur != null && isAdded()) {
                        // On passe l'ID et le Rôle à l'adaptateur pour configurer la visibilité des boutons
                        adapter.setCurrentUser(myUserId, utilisateur.getIdRole());
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Erreur silencieuse ou log, l'utilisateur verra l'interface par défaut (citoyen)
                }
            });
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

                        if (matchesDesc || matchesCat) {
                            displayList.add(i);
                        }
                    }
                } else {
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

    // --- IMPLÉMENTATION DES ACTIONS DE L'ADAPTATEUR ---

    @Override
    public void onValidateClick(Incident incident) {
        // Appelé quand une Autorité/Admin clique sur "Valider"
        new AlertDialog.Builder(getContext())
                .setTitle("Validation")
                .setMessage("Confirmer la prise en charge de cet incident ? Il passera au statut 'Traité'.")
                .setPositiveButton("Oui", (dialog, which) -> {
                    firestoreRepo.updateIncidentStatus(incident.getId(), "Traité", new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Incident validé avec succès !", Toast.LENGTH_SHORT).show();
                            // La liste se mettra à jour automatiquement grâce au Realtime Listener
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(getContext(), "Erreur lors de la validation : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onEditClick(Incident incident) {
        // Redirection vers un fragment d'édition (À implémenter selon besoins)
        Toast.makeText(getContext(), "Fonctionnalité d'édition à venir", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(Incident incident) {
        // Suppression (possible si c'est mon incident ou si je suis admin)
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
        // Redirection vers la Map
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToMapAndFocus(incident.getLatitude(), incident.getLongitude());
        }
    }
}
package com.example.safecity.ui.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import nécessaire pour le bouton du Dialog
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.graphics.Color;
import android.graphics.Typeface;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Import pour le type de dialogue retourné par le Builder
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.MainActivity;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.model.NotificationApp;
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.ui.adapters.IncidentAdapter;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private IncidentAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private ListenerRegistration firestoreListener;

    private ChipGroup chipGroup;
    private FloatingActionButton fabStats;
    private List<Incident> allIncidents = new ArrayList<>();

    private String searchQuery = null;
    private String myUserId;

    // Variable pour stocker si l'utilisateur est admin
    private boolean isAdminMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        chipGroup = view.findViewById(R.id.chip_group_filters_home);

        // Récupération sécurisée du FAB Stats
        fabStats = requireActivity().findViewById(R.id.fab_stats);
        if (fabStats != null) {
            fabStats.setVisibility(View.GONE);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firestoreRepo = new FirestoreRepository();

        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        if (searchQuery != null) {
            Toast.makeText(getContext(), "Recherche : " + searchQuery, Toast.LENGTH_SHORT).show();
        }

        // --- GESTION UTILISATEUR & RÔLES ---
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            myUserId = fbUser.getUid();

            firestoreRepo.getUser(myUserId, new FirestoreRepository.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    if (utilisateur != null && isAdded()) {
                        String role = utilisateur.getIdRole();

                        if ("admin".equalsIgnoreCase(role) || "autorite".equalsIgnoreCase(role)) {
                            isAdminMode = true;
                            if (fabStats != null) {
                                fabStats.setVisibility(View.VISIBLE);
                                // MODIFICATION : Menu Admin pour choisir Stats ou Alerte
                                fabStats.setOnClickListener(v -> showAdminMenu());
                            }
                        } else {
                            isAdminMode = false;
                            if (fabStats != null) {
                                fabStats.setVisibility(View.GONE);
                            }
                        }
                        adapter.setCurrentUser(myUserId, role);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if(isAdded()) {
                        adapter.setCurrentUser(myUserId, "user");
                        if (fabStats != null) fabStats.setVisibility(View.GONE);
                    }
                }
            });
        }

        // --- LISTENER DES CHIPS MIS À JOUR ---
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            applyFilters();
        });
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
        if (fabStats != null) {
            fabStats.setVisibility(View.GONE);
        }
    }

    private void loadData() {
        firestoreListener = firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (!isAdded() || getActivity() == null) return;
                allIncidents = incidents != null ? incidents : new ArrayList<>();
                applyFilters();
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // --- LOGIQUE DE FILTRAGE MISE À JOUR (Panne + Insensible à la casse) ---
    private void applyFilters() {
        List<Incident> filteredList = new ArrayList<>();
        String queryLower = (searchQuery != null) ? searchQuery.toLowerCase() : null;

        String categoryFilter = null;
        int checkedId = chipGroup.getCheckedChipId();

        // Correspondance avec les nouveaux IDs du XML
        if (checkedId == R.id.chip_accident) categoryFilter = "Accident";
        else if (checkedId == R.id.chip_vol) categoryFilter = "Vol";
        else if (checkedId == R.id.chip_incendie) categoryFilter = "Incendie";
        else if (checkedId == R.id.chip_panne) categoryFilter = "Panne";
        else if (checkedId == R.id.chip_autre) categoryFilter = "Autre";

        for (Incident i : allIncidents) {
            boolean matchesSearch = true;
            boolean matchesCategory = true;

            // 1. Filtre Recherche (Texte)
            if (queryLower != null) {
                boolean inDesc = i.getDescription() != null && i.getDescription().toLowerCase().contains(queryLower);
                boolean inCat = i.getNomCategorie() != null && i.getNomCategorie().toLowerCase().contains(queryLower);
                if (!inDesc && !inCat) matchesSearch = false;
            }

            // 2. Filtre Catégorie (Chips) - Comparaison insensible à la casse
            if (categoryFilter != null) {
                // On compare en minuscules pour trouver "panne" dans "Panne électrique" ou "PANNE MOTEUR"
                if (i.getNomCategorie() == null ||
                        !i.getNomCategorie().toLowerCase().contains(categoryFilter.toLowerCase())) {
                    matchesCategory = false;
                }
            }

            if (matchesSearch && matchesCategory) {
                filteredList.add(i);
            }
        }

        adapter.updateData(filteredList);

        // Gestion de l'affichage "Vide"
        if (filteredList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            if (categoryFilter != null) tvEmptyState.setText("Aucun incident de type '" + categoryFilter + "'");
            else if (searchQuery != null) tvEmptyState.setText("Aucun résultat pour \"" + searchQuery + "\"");
            else tvEmptyState.setText("Aucun incident.");
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // NOUVELLES MÉTHODES D'ALERTE OFFICIELLE - ROBUSTE
    // ==================================================================

    /**
     * Affiche un menu de choix pour l'administrateur.
     */
    private void showAdminMenu() {
        if (getContext() == null) return;
        String[] options = {"📊 Voir Tableau de Bord", "📢 Diffuser Alerte Officielle"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Administration")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showStatisticsDialog();
                    else showSendAlertDialog();
                })
                .show();
    }

    /**
     * Affiche une boîte de dialogue PROFESSIONNELLE pour envoyer une alerte.
     * Version CORRIGÉE : Ne ferme pas le popup si les champs sont vides.
     */
    private void showSendAlertDialog() {
        if (getContext() == null) return;

        // Layout container
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        // -- Section Priorité --
        TextView tvPriority = new TextView(getContext());
        tvPriority.setText("Niveau d'urgence");
        tvPriority.setTypeface(null, Typeface.BOLD);
        tvPriority.setTextColor(Color.DKGRAY);
        layout.addView(tvPriority);

        RadioGroup rgPriority = new RadioGroup(getContext());
        rgPriority.setOrientation(LinearLayout.HORIZONTAL);
        rgPriority.setPadding(0, 10, 0, 30);

        RadioButton rbInfo = new RadioButton(getContext());
        rbInfo.setText("Information");
        rbInfo.setChecked(true);

        RadioButton rbUrgent = new RadioButton(getContext());
        rbUrgent.setText("URGENCE");
        rbUrgent.setTextColor(Color.RED);
        rbUrgent.setTypeface(null, Typeface.BOLD);

        rgPriority.addView(rbInfo);
        rgPriority.addView(rbUrgent);
        layout.addView(rgPriority);

        // -- Section Titre --
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("Titre de l'alerte");
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.DKGRAY);
        layout.addView(tvTitle);

        final EditText etTitle = new EditText(getContext());
        etTitle.setHint("Ex: Tempête, Coupure d'eau...");
        layout.addView(etTitle);

        // -- Section Message --
        TextView tvMessage = new TextView(getContext());
        tvMessage.setText("Message détaillé");
        tvMessage.setTypeface(null, Typeface.BOLD);
        tvMessage.setTextColor(Color.DKGRAY);
        tvMessage.setPadding(0, 20, 0, 0);
        layout.addView(tvMessage);

        final EditText etMessage = new EditText(getContext());
        etMessage.setHint("Instructions pour les citoyens...");
        etMessage.setLines(3);
        layout.addView(etMessage);

        // 1. Création du Dialog SANS l'afficher tout de suite
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(layout)
                .setTitle("📢 Diffuser une Alerte")
                .setPositiveButton("ENVOYER", null) // IMPORTANT : On met null pour override le listener ensuite
                .setNegativeButton("Annuler", null)
                .create();

        // 2. On configure le comportement du bouton APRES l'affichage (onShow)
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String titleInput = etTitle.getText().toString().trim();
                String body = etMessage.getText().toString().trim();
                boolean isUrgent = rbUrgent.isChecked();

                if (!titleInput.isEmpty() && !body.isEmpty()) {
                    // C'est valide, on envoie et on ferme
                    String finalTitle = isUrgent ? "🚨 [URGENT] " + titleInput : "ℹ️ " + titleInput;
                    sendAlertToFirebase(finalTitle, body);
                    dialog.dismiss();
                } else {
                    // Erreur : on affiche le Toast MAIS on ne ferme pas le dialog
                    Toast.makeText(getContext(), "Le titre et le message sont requis.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * Crée un objet NotificationApp et l'envoie.
     */
    private void sendAlertToFirebase(String title, String message) {
        if (getContext() == null) return;

        NotificationApp alert = new NotificationApp();
        alert.setTitre(title);
        alert.setMessage(message);
        alert.setDate(new Date());
        alert.setType("ALERTE_OFFICIELLE");

        firestoreRepo.addOfficialAlert(alert, new FirestoreRepository.OnFirestoreTaskComplete() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Alerte envoyée à la population !", Toast.LENGTH_LONG).show();
                firestoreRepo.addNotification(alert);
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Erreur lors de l'envoi : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================================================================
    // ACTIONS ADAPTER (Implementation Interface)
    // ==================================================================

    @Override
    public void onValidateClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Validation")
                .setMessage("Confirmer la prise en charge de cet incident ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    firestoreRepo.updateIncidentStatus(incident.getId(), "Traité", new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Incident validé !", Toast.LENGTH_SHORT).show();
                            NotificationApp notif = new NotificationApp(
                                    "Incident Résolu ✅",
                                    "L'incident de type '" + incident.getNomCategorie() + "' a été traité par les autorités.",
                                    "validation"
                            );
                            firestoreRepo.addNotification(notif);
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
    public void onEditClick(Incident incident) {
        boolean isOwner = myUserId != null && myUserId.equals(incident.getIdUtilisateur());

        if (isOwner || isAdminMode) {
            SignalementFragment fragment = SignalementFragment.newInstance(incident.getId());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast.makeText(getContext(), "Vous n'avez pas la permission de modifier cet incident.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Suppression")
                .setMessage("Supprimer définitivement ce signalement ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    firestoreRepo.deleteIncident(incident.getId(), incident.getPhotoUrl(), new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Supprimé.", Toast.LENGTH_SHORT).show();
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
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToMapAndFocus(incident.getLatitude(), incident.getLongitude());
        }
    }

    // --- NOUVEAU : Clic sur l'image pour Zoom ---
    @Override
    public void onImageClick(Incident incident) {
        if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
            showFullImageDialog(incident.getPhotoUrl());
        }
    }

    // --- Méthode Helper pour afficher le Dialog Plein Écran ---
    private void showFullImageDialog(String imageUrl) {
        if (getContext() == null) return;

        // Utilisation d'un Dialog standard avec un thème plein écran
        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView fullImageView = dialog.findViewById(R.id.img_full_screen);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close_dialog);

        // Charger l'image avec Glide (fitCenter pour voir l'image entière)
        Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .placeholder(R.drawable.ic_incident_placeholder) // Assurez-vous d'avoir ce drawable ou retirez la ligne
                .into(fullImageView);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Affiche des statistiques détaillées et formatées professionnellement.
     */
    private void showStatisticsDialog() {
        if (allIncidents == null || allIncidents.isEmpty()) {
            Toast.makeText(getContext(), "Pas de données.", Toast.LENGTH_SHORT).show();
            return;
        }
        int nbAccidents = 0, nbVols = 0, nbIncendies = 0, nbTravaux = 0, nbTraites = 0;

        for (Incident i : allIncidents) {
            String cat = (i.getNomCategorie() != null) ? i.getNomCategorie().toLowerCase() : "";
            if (cat.contains("accident")) nbAccidents++;
            else if (cat.contains("vol")) nbVols++;
            else if (cat.contains("incendie")) nbIncendies++;
            else if (cat.contains("travaux") || cat.contains("panne")) nbTravaux++;

            if ("Traité".equalsIgnoreCase(i.getStatut())) nbTraites++;
        }

        // Calcul du pourcentage
        int total = allIncidents.size();
        int pourcentageReussite = (total > 0) ? (nbTraites * 100 / total) : 0;

        // Construction du rapport professionnel
        StringBuilder sb = new StringBuilder();
        sb.append("📅 SITUATION GLOBALE\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📌 Total Incidents : ").append(total).append("\n");
        sb.append("✅ Taux de Résolution : ").append(pourcentageReussite).append("%\n\n");

        sb.append("📂 RÉPARTITION PAR TYPE\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🚗  Accidents : ").append(nbAccidents).append("\n");
        sb.append("🏃  Sécurité (Vols) : ").append(nbVols).append("\n");
        sb.append("🔥  Incendies : ").append(nbIncendies).append("\n");
        sb.append("🔧  Technique : ").append(nbTravaux).append("\n");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("📊 Tableau de Bord")
                .setMessage(sb.toString())
                .setPositiveButton("Fermer", null)
                .show();
    }
}
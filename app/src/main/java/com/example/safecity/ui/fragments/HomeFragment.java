package com.example.safecity.ui.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.safecity.MainActivity;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.model.NotificationApp;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.ui.adapters.IncidentAdapter;
import com.example.safecity.utils.FirestoreRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment principal affichant le flux d'incidents signalés.
 * Correction : Intégration de validateIncident (4 arguments) pour la comptabilisation des points.
 */
public class HomeFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private TextView tvEmptyState;
    private IncidentAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private ListenerRegistration firestoreListener;

    private ChipGroup chipGroup;
    private FloatingActionButton fabStats;
    private List<Incident> allIncidents = new ArrayList<>();

    private String searchQuery = null;
    private String myUserId;
    private boolean isAdminMode = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

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
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        chipGroup = view.findViewById(R.id.chip_group_filters_home);

        // Liaison avec le bouton de la MainActivity
        fabStats = requireActivity().findViewById(R.id.fab_stats);

        // CORRECTION : On ne cache plus fabStats par défaut ici, car cela annule
        // les changements de visibilité faits par l'activité ou les callbacks.

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firestoreRepo = new FirestoreRepository();

        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // --- AUTH & ROLES ---
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            myUserId = fbUser.getUid();
            firestoreRepo.getUser(myUserId, new FirestoreRepository.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    if (utilisateur != null && isAdded()) {
                        String role = utilisateur.getIdRole();

                        // Gestion centralisée de la visibilité selon le rôle
                        if ("admin".equalsIgnoreCase(role) || "autorite".equalsIgnoreCase(role)) {
                            isAdminMode = true;
                            if (fabStats != null) {
                                fabStats.setVisibility(View.VISIBLE);
                                fabStats.setOnClickListener(v -> showAdminMenu());
                            }
                        } else {
                            isAdminMode = false;
                            if (fabStats != null) fabStats.setVisibility(View.GONE);
                        }

                        // Important : Passer le rôle à l'adapter pour afficher/cacher le bouton "Valider"
                        adapter.setCurrentUser(myUserId, role);
                    }
                }
                @Override
                public void onError(Exception e) {
                    if(isAdded()) adapter.setCurrentUser(myUserId, "user");
                }
            });
        }

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> applyFilters(searchQuery));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setSubmitButtonEnabled(true);
                searchView.setIconifiedByDefault(false);
                searchView.setQueryHint("Rechercher un incident...");

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        searchQuery = query;
                        applyFilters(query);
                        searchView.clearFocus();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        searchQuery = newText;
                        applyFilters(newText);
                        return true;
                    }
                });
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
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
                allIncidents = incidents != null ? incidents : new ArrayList<>();
                applyFilters(searchQuery);
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters(String queryText) {
        List<Incident> filteredList = new ArrayList<>();
        String queryLower = (queryText != null) ? queryText.toLowerCase() : null;

        String categoryFilter = null;
        int checkedId = chipGroup.getCheckedChipId();

        if (checkedId == R.id.chip_accident) categoryFilter = "Accident";
        else if (checkedId == R.id.chip_vol) categoryFilter = "Vol";
        else if (checkedId == R.id.chip_incendie) categoryFilter = "Incendie";
        else if (checkedId == R.id.chip_panne) categoryFilter = "Panne";
        else if (checkedId == R.id.chip_autre) categoryFilter = "Autre";

        for (Incident i : allIncidents) {
            boolean matchesSearch = true;
            boolean matchesCategory = true;

            if (queryLower != null && !queryLower.isEmpty()) {
                boolean inDesc = i.getDescription() != null && i.getDescription().toLowerCase().contains(queryLower);
                boolean inCat = i.getNomCategorie() != null && i.getNomCategorie().toLowerCase().contains(queryLower);
                if (!inDesc && !inCat) matchesSearch = false;
            }

            if (categoryFilter != null) {
                if (i.getNomCategorie() == null || !i.getNomCategorie().toLowerCase().contains(categoryFilter.toLowerCase())) {
                    matchesCategory = false;
                }
            }

            if (matchesSearch && matchesCategory) {
                filteredList.add(i);
            }
        }

        adapter.updateData(filteredList);

        if (filteredList.isEmpty()) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

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

    private void showSendAlertDialog() {
        if (getContext() == null) return;

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setFillViewport(true);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding/2, padding, padding);

        TextView tvPriority = new TextView(getContext());
        tvPriority.setText("Niveau d'urgence");
        tvPriority.setTypeface(null, Typeface.BOLD);
        layout.addView(tvPriority);

        RadioGroup rgPriority = new RadioGroup(getContext());
        rgPriority.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton rbInfo = new RadioButton(getContext());
        rbInfo.setText("Info");
        rbInfo.setChecked(true);
        RadioButton rbUrgent = new RadioButton(getContext());
        rbUrgent.setText("URGENCE");
        rbUrgent.setTextColor(Color.RED);
        rgPriority.addView(rbInfo);
        rgPriority.addView(rbUrgent);
        layout.addView(rgPriority);

        final EditText etTitle = new EditText(getContext());
        etTitle.setHint("Titre de l'alerte");
        layout.addView(etTitle);

        final EditText etMessage = new EditText(getContext());
        etMessage.setHint("Contenu du message");
        etMessage.setLines(3);
        layout.addView(etMessage);

        scrollView.addView(layout);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(scrollView)
                .setTitle("📢 Diffuser une Alerte")
                .setPositiveButton("ENVOYER", null)
                .setNegativeButton("Annuler", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String msg = etMessage.getText().toString().trim();
                if (!title.isEmpty() && !msg.isEmpty()) {
                    String finalTitle = rbUrgent.isChecked() ? "🚨 [URGENT] " + title : "ℹ️ " + title;
                    sendAlertToFirebase(finalTitle, msg);
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Champs requis.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    private void sendAlertToFirebase(String title, String message) {
        NotificationApp alert = new NotificationApp(title, message, "ALERTE_OFFICIELLE");
        alert.setDate(new Date());
        firestoreRepo.addOfficialAlert(alert, new FirestoreRepository.OnFirestoreTaskComplete() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Alerte envoyée !", Toast.LENGTH_LONG).show();
                firestoreRepo.addNotification(alert);
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Erreur envoi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Gère le clic sur le bouton de validation (Autorités uniquement).
     * Correction : Utilise validateIncident avec 4 arguments pour créditer les points à l'auteur.
     */
    @Override
    public void onValidateClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Validation du signalement")
                .setMessage("En validant ce signalement, vous confirmez que l'incident a été traité. L'auteur recevra des points de score.")
                .setPositiveButton("Valider", (d, w) -> {
                    // Changement : Utilisation de validateIncident(id, authorId, isValid, listener)
                    // isValid = true pour une validation réussie (+20 pts)
                    firestoreRepo.validateIncident(incident.getId(), incident.getIdUtilisateur(), true, new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Validé ! Points attribués à l'auteur.", Toast.LENGTH_SHORT).show();
                                firestoreRepo.addNotification(new NotificationApp("Résolu ✅", incident.getNomCategorie() + " a été traité.", "validation"));
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Erreur lors de la validation", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Annuler", null).show();
    }

    @Override
    public void onEditClick(Incident incident) {
        if ((myUserId != null && myUserId.equals(incident.getIdUtilisateur())) || isAdminMode) {
            SignalementFragment fragment = SignalementFragment.newInstance(incident.getId());
            getParentFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, fragment).addToBackStack(null).commit();
        } else {
            Toast.makeText(getContext(), "Accès refusé.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer ?")
                .setPositiveButton("Oui", (d, w) -> firestoreRepo.deleteIncident(incident.getId(), incident.getPhotoUrl(), new FirestoreRepository.OnFirestoreTaskComplete() {
                    @Override
                    public void onSuccess() { Toast.makeText(getContext(), "Supprimé.", Toast.LENGTH_SHORT).show(); }
                    @Override
                    public void onError(Exception e) {}
                }))
                .setNegativeButton("Non", null).show();
    }

    @Override
    public void onMapClick(Incident incident) {
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateToMapAndFocus(incident.getLatitude(), incident.getLongitude());
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

    private void showStatisticsDialog() {
        if (allIncidents.isEmpty()) { Toast.makeText(getContext(), "Pas de données.", Toast.LENGTH_SHORT).show(); return; }
        int nbAccidents = 0, nbVols = 0, nbIncendies = 0, nbTraites = 0;
        for (Incident i : allIncidents) {
            String cat = (i.getNomCategorie() != null) ? i.getNomCategorie().toLowerCase() : "";
            if (cat.contains("accident")) nbAccidents++;
            else if (cat.contains("vol")) nbVols++;
            else if (cat.contains("incendie")) nbIncendies++;
            if ("Traité".equalsIgnoreCase(i.getStatut())) nbTraites++;
        }
        int success = (allIncidents.size() > 0) ? (nbTraites * 100 / allIncidents.size()) : 0;
        String msg = "📌 Total: " + allIncidents.size() + "\n✅ Résolus: " + success + "%\n\n🚗 Accidents: " + nbAccidents + "\n🏃 Vols: " + nbVols + "\n🔥 Incendies: " + nbIncendies;
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Tableau de Bord").setMessage(msg).setPositiveButton("OK", null).show();
    }

    @Override
    public void onCommentClick(Incident incident) {
        CommentFragment bottomSheet = CommentFragment.newInstance(incident.getId());
        bottomSheet.show(getParentFragmentManager(), "CommentBottomSheet");
    }
}
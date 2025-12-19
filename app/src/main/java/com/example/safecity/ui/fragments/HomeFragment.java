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
 * Gère le filtrage par recherche textuelle et par catégorie de manière synchronisée avec le XML.
 */
public class HomeFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private View layoutEmptyState; // Conteneur complet de l'état vide
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

        // Liaison avec les nouveaux IDs du XML
        recyclerView = view.findViewById(R.id.recycler_view_home);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        chipGroup = view.findViewById(R.id.chip_group_filters_home);

        fabStats = requireActivity().findViewById(R.id.fab_stats);
        if (fabStats != null) {
            fabStats.setVisibility(View.GONE);
        }

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
                        adapter.setCurrentUser(myUserId, role);
                    }
                }
                @Override
                public void onError(Exception e) {
                    if(isAdded()) adapter.setCurrentUser(myUserId, "user");
                }
            });
        }

        // --- LISTENER CHIPS (Synchronisé avec les nouveaux IDs) ---
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

                ImageView searchGoBtn = searchView.findViewById(androidx.appcompat.R.id.search_go_btn);
                if (searchGoBtn != null) {
                    searchGoBtn.setColorFilter(Color.BLACK);
                }

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

                searchView.setOnCloseListener(() -> {
                    searchQuery = null;
                    applyFilters(null);
                    return false;
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
        if (fabStats != null) fabStats.setVisibility(View.GONE);
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

        // Synchronisation des catégories avec les Chips du XML
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

        // Gestion de l'affichage de l'état vide ou de la liste
        if (filteredList.isEmpty()) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (tvEmptyState != null) {
                if (categoryFilter != null) tvEmptyState.setText("Aucun '" + categoryFilter + "' trouvé.");
                else if (queryLower != null) tvEmptyState.setText("Aucun résultat pour \"" + queryText + "\"");
                else tvEmptyState.setText("Aucun signalement trouvé.\nSoyez le premier à informer la ville !");
            }
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
                .setItems(options, (dialog, Bayern) -> {
                    if (Bayern == 0) showStatisticsDialog();
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
        tvPriority.setTextColor(Color.DKGRAY);
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

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText("Titre");
        layout.addView(tvTitle);
        final EditText etTitle = new EditText(getContext());
        layout.addView(etTitle);

        TextView tvMessage = new TextView(getContext());
        tvMessage.setText("Message");
        tvMessage.setPadding(0, 20, 0, 0);
        layout.addView(tvMessage);
        final EditText etMessage = new EditText(getContext());
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

    @Override
    public void onValidateClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Validation")
                .setMessage("Valider la prise en charge ?")
                .setPositiveButton("Oui", (d, w) -> {
                    firestoreRepo.updateIncidentStatus(incident.getId(), "Traité", new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), "Validé !", Toast.LENGTH_SHORT).show();
                            firestoreRepo.addNotification(new NotificationApp("Résolu ✅", incident.getNomCategorie() + " traité.", "validation"));
                        }
                        @Override
                        public void onError(Exception e) {}
                    });
                })
                .setNegativeButton("Non", null).show();
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
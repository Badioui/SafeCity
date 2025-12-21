package com.example.safecity.ui.fragments;

import android.app.Dialog;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment implements IncidentAdapter.OnIncidentActionListener {

    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private IncidentAdapter adapter;
    private FirestoreRepository firestoreRepo;
    private ListenerRegistration firestoreListener;

    private ChipGroup chipGroup;
    private FloatingActionButton fabStats;
    private List<Incident> allIncidents = new ArrayList<>();

    private String searchQuery = null;
    private String focusIncidentId = null;
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
            focusIncidentId = getArguments().getString("focus_incident_id");
        }
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_home);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        chipGroup = view.findViewById(R.id.chip_group_filters_home);
        fabStats = requireActivity().findViewById(R.id.fab_stats);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firestoreRepo = new FirestoreRepository();

        adapter = new IncidentAdapter(getContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            myUserId = fbUser.getUid();
            firestoreRepo.getUser(myUserId, new FirestoreRepository.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    if (utilisateur != null && isAdded()) {
                        String role = utilisateur.getIdRole();
                        isAdminMode = "admin".equalsIgnoreCase(role) || "autorite".equalsIgnoreCase(role);
                        if (fabStats != null) {
                            fabStats.setVisibility(isAdminMode ? View.VISIBLE : View.GONE);
                            if (isAdminMode) fabStats.setOnClickListener(v -> showAdminMenu());
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

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> applyFilters(searchQuery));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home, menu);
        if (focusIncidentId != null) {
            menu.findItem(R.id.action_search).setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadData(focusIncidentId);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }

    private void loadData(String incidentId) {
        if (incidentId != null) {
            firestoreRepo.getIncident(incidentId, new FirestoreRepository.OnIncidentLoadedListener() {
                @Override
                public void onIncidentLoaded(Incident incident) {
                    if (isAdded() && incident != null) {
                        allIncidents = Collections.singletonList(incident);
                        applyFilters(null);
                    }
                }
                @Override
                public void onError(Exception e) {
                    if (isAdded()) Toast.makeText(getContext(), "Erreur: Incident introuvable", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
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
    }

    private void applyFilters(String queryText) {
        List<Incident> filteredList = new ArrayList<>();
        if (focusIncidentId != null) {
            filteredList.addAll(allIncidents);
            chipGroup.setVisibility(View.GONE);
        } else {
            String queryLower = (queryText != null) ? queryText.toLowerCase() : null;
            String categoryFilter = getCategoryFilter();

            for (Incident i : allIncidents) {
                boolean matchesSearch = true;
                boolean matchesCategory = true;

                if (queryLower != null && !queryLower.isEmpty()) {
                    matchesSearch = (i.getDescription() != null && i.getDescription().toLowerCase().contains(queryLower)) ||
                                  (i.getNomCategorie() != null && i.getNomCategorie().toLowerCase().contains(queryLower));
                }

                if (categoryFilter != null) {
                    matchesCategory = (i.getNomCategorie() != null && i.getNomCategorie().equalsIgnoreCase(categoryFilter));
                }

                if (matchesSearch && matchesCategory) {
                    filteredList.add(i);
                }
            }
        }

        adapter.updateData(filteredList);

        boolean isEmpty = filteredList.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private String getCategoryFilter() {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chip_accident) return "Accident";
        if (checkedId == R.id.chip_vol) return "Vol";
        if (checkedId == R.id.chip_incendie) return "Incendie";
        if (checkedId == R.id.chip_panne) return "Panne";
        if (checkedId == R.id.chip_autre) return "Autre";
        return null;
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
        // 1. On "gonfle" le layout XML
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_send_alert, null);

        // 2. On récupère les vues du layout
        final EditText etTitle = dialogView.findViewById(R.id.et_alert_title);
        final EditText etMessage = dialogView.findViewById(R.id.et_alert_message);
        final RadioButton rbUrgent = dialogView.findViewById(R.id.rb_urgent);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("📢 Diffuser une Alerte")
                .setView(dialogView) // 3. On utilise le layout
                .setPositiveButton("ENVOYER", null) // On met null pour gérer le clic manuellement
                .setNegativeButton("Annuler", null)
                .create();

        // 4. On ajoute la logique de validation
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String message = etMessage.getText().toString().trim();

                if (title.isEmpty()) {
                    etTitle.setError("Le titre est requis");
                    return;
                }
                if (message.isEmpty()) {
                    etMessage.setError("Le message est requis");
                    return;
                }

                String finalTitle = rbUrgent.isChecked() ? "🚨 [URGENT] " + title : "ℹ️ " + title;
                sendAlertToFirebase(finalTitle, message);
                dialog.dismiss(); // On ferme la boite de dialogue seulement si tout est valide
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
                if(isAdded()) Toast.makeText(getContext(), "Alerte envoyée !", Toast.LENGTH_LONG).show();
            }
            @Override
            public void onError(Exception e) {
                if(isAdded()) Toast.makeText(getContext(), "Erreur lors de l'envoi de l'alerte", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onValidateClick(Incident incident) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Validation du signalement")
                .setMessage("Confirmez-vous que cet incident a été traité ? L\'auteur recevra des points pour sa contribution.")
                .setPositiveButton("Valider", (d, w) -> {
                    firestoreRepo.validateIncident(incident.getId(), incident.getIdUtilisateur(), true, new FirestoreRepository.OnFirestoreTaskComplete() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Signalement validé !", Toast.LENGTH_SHORT).show();
                                NotificationApp notification = new NotificationApp(
                                        "Votre signalement a été traité !",
                                        "Merci pour votre contribution, votre signalement concernant \"" + incident.getNomCategorie() + "\" a été validé par les autorités.",
                                        "validation"
                                );
                                notification.setIdDestinataire(incident.getIdUtilisateur());
                                notification.setIdIncidentSource(incident.getId());
                                firestoreRepo.addNotification(notification);
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
                    public void onSuccess() { if(isAdded()) Toast.makeText(getContext(), "Supprimé.", Toast.LENGTH_SHORT).show(); }
                    @Override
                    public void onError(Exception e) { if(isAdded()) Toast.makeText(getContext(), "Erreur de suppression.", Toast.LENGTH_SHORT).show(); }
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
        if (allIncidents.isEmpty()) { Toast.makeText(getContext(), "Pas de données à afficher.", Toast.LENGTH_SHORT).show(); return; }
        int nbAccidents = 0, nbVols = 0, nbIncendies = 0, nbTraites = 0;
        for (Incident i : allIncidents) {
            String cat = (i.getNomCategorie() != null) ? i.getNomCategorie().toLowerCase() : "";
            if (cat.contains("accident")) nbAccidents++;
            else if (cat.contains("vol")) nbVols++;
            else if (cat.contains("incendie")) nbIncendies++;
            if ("Traité".equalsIgnoreCase(i.getStatut())) nbTraites++;
        }
        int success = (allIncidents.size() > 0) ? (nbTraites * 100 / allIncidents.size()) : 0;
        String msg = "📌 Total des signalements: " + allIncidents.size() + "\n✅ Taux de résolution: " + success + "%\n\nCatégories principales :\n🚗 Accidents: " + nbAccidents + "\n🏃 Vols: " + nbVols + "\n🔥 Incendies: " + nbIncendies;
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Tableau de Bord").setMessage(msg).setPositiveButton("OK", null).show();
    }

    @Override
    public void onCommentClick(Incident incident) {
        CommentFragment bottomSheet = CommentFragment.newInstance(incident.getId());
        bottomSheet.show(getParentFragmentManager(), "CommentBottomSheet");
    }
}

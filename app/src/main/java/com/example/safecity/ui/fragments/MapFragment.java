package com.example.safecity.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.utils.AppExecutors;
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.utils.LocationHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment fusionné gérant la cartographie interactive de SafeCity.
 * Gère le double filtrage combiné (Statut + Catégorie) et la BottomSheet avec média.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirestoreRepository firestoreRepo;
    private ClusterManager<Incident> clusterManager;
    private LocationHelper locationHelper;

    // Données et État des filtres
    private List<Incident> allIncidents = new ArrayList<>();
    private String selectedCategory = "Tous";
    private boolean isShowingTraite = false; // Bascule entre Nouveau et Traité

    // Focus de navigation (si ouvert depuis une notification ou liste)
    private Double targetLat = null;
    private Double targetLng = null;

    // Composants UI
    private ChipGroup chipGroupCategories;
    private Chip chipStatusToggle;
    private SearchView searchView;
    private FloatingActionButton fabLocation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            targetLat = getArguments().containsKey("focus_lat") ? getArguments().getDouble("focus_lat") : null;
            targetLng = getArguments().containsKey("focus_lng") ? getArguments().getDouble("focus_lng") : null;
        }
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreRepo = new FirestoreRepository();
        locationHelper = new LocationHelper(requireContext());

        chipGroupCategories = view.findViewById(R.id.chip_group_filters);
        chipStatusToggle = view.findViewById(R.id.chip_filter_status);
        searchView = view.findViewById(R.id.map_search_view);
        fabLocation = view.findViewById(R.id.fab_my_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupListeners();
    }

    /**
     * Configure les interactions : Filtres combinés, Recherche et GPS.
     */
    private void setupListeners() {
        // 1. Filtrage par Statut (Single Chip Toggle)
        if (chipStatusToggle != null) {
            chipStatusToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isShowingTraite = isChecked;
                chipStatusToggle.setText(isShowingTraite ? "Traité" : "Nouveau");

                // Indication visuelle de l'icône selon l'état
                int iconRes = isShowingTraite ? android.R.drawable.checkbox_on_background : android.R.drawable.ic_menu_info_details;
                chipStatusToggle.setChipIcon(ContextCompat.getDrawable(requireContext(), iconRes));

                applyCombinedFilters();
            });
        }

        // 2. Filtrage par Catégorie (ChipGroup)
        if (chipGroupCategories != null) {
            chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chip_all) selectedCategory = "Tous";
                else if (checkedId == R.id.chip_accident) selectedCategory = "Accident";
                else if (checkedId == R.id.chip_vol) selectedCategory = "Vol";
                else if (checkedId == R.id.chip_incendie) selectedCategory = "Incendie";
                else if (checkedId == R.id.chip_panne) selectedCategory = "Panne";
                else if (checkedId == R.id.chip_autre) selectedCategory = "Autre";

                applyCombinedFilters();
            });
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });

        fabLocation.setOnClickListener(v -> moveToCurrentLocation());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.setPadding(0, 200, 0, 0);

        if (targetLat != null && targetLng != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(targetLat, targetLng), 16));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(34.6814, -1.9076), 12));
        }

        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        clusterManager = new ClusterManager<>(getContext(), googleMap);
        googleMap.setOnCameraIdleListener(clusterManager);
        googleMap.setOnMarkerClickListener(clusterManager);

        clusterManager.setOnClusterItemClickListener(incident -> {
            showIncidentBottomSheet(incident);
            return true;
        });

        enableUserLocation();
        loadIncidentMarkers();
    }

    /**
     * Applique les filtres de Statut ET de Catégorie simultanément.
     */
    private void applyCombinedFilters() {
        if (allIncidents == null || clusterManager == null) return;

        List<Incident> filteredList = new ArrayList<>();
        String targetStatus = isShowingTraite ? Incident.STATUT_TRAITE : Incident.STATUT_NOUVEAU;

        for (Incident inc : allIncidents) {
            // Vérification du Statut
            boolean statusMatches = inc.getStatut() != null && inc.getStatut().equalsIgnoreCase(targetStatus);

            // Vérification de la Catégorie
            boolean categoryMatches = selectedCategory.equals("Tous") ||
                    (inc.getNomCategorie() != null && inc.getNomCategorie().equalsIgnoreCase(selectedCategory));

            if (statusMatches && categoryMatches) {
                if (Math.abs(inc.getLatitude()) > 0.001) {
                    filteredList.add(inc);
                }
            }
        }

        clusterManager.clearItems();
        clusterManager.addItems(filteredList);
        clusterManager.cluster();
    }

    private void loadIncidentMarkers() {
        firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (!isAdded()) return;
                allIncidents = incidents;
                applyCombinedFilters();
            }
            @Override
            public void onError(Exception e) {
                if (isAdded()) Toast.makeText(getContext(), "Erreur de synchronisation", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Affiche la BottomSheet avec correction de l'affichage média et synchronisation temps réel.
     */
    private void showIncidentBottomSheet(Incident incident) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_map_bottom_sheet, null);
        dialog.setContentView(view);

        // Liaison Composants
        ImageView imgProfile = view.findViewById(R.id.sheet_img_profile);
        TextView tvUsername = view.findViewById(R.id.sheet_tv_username);
        TextView tvCategory = view.findViewById(R.id.sheet_tv_category);
        TextView tvStatus = view.findViewById(R.id.sheet_tv_status);
        TextView tvDescription = view.findViewById(R.id.sheet_tv_description);
        ImageView imgIncident = view.findViewById(R.id.sheet_img_incident);
        View cardMedia = view.findViewById(R.id.card_incident_media);
        ImageView imgHeart = view.findViewById(R.id.sheet_img_heart);
        TextView tvLikes = view.findViewById(R.id.sheet_tv_likes);
        TextView tvComments = view.findViewById(R.id.sheet_tv_comments);

        // Données initiales
        tvUsername.setText(incident.getNomUtilisateur());
        tvCategory.setText("🚨 " + incident.getNomCategorie());
        tvDescription.setText(incident.getDescription());
        tvStatus.setText(incident.getStatut());

        if (Incident.STATUT_TRAITE.equalsIgnoreCase(incident.getStatut())) {
            tvStatus.setBackgroundResource(R.drawable.status_traite_bg);
        }

        // --- GESTION DES MÉDIAS (CORRIGÉE) ---
        if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
            if (cardMedia != null) cardMedia.setVisibility(View.VISIBLE);
            if (imgIncident != null) {
                imgIncident.setVisibility(View.VISIBLE); // S'assurer que l'image est visible
                Glide.with(this)
                        .load(incident.getPhotoUrl())
                        .placeholder(R.drawable.ic_incident_placeholder)
                        .into(imgIncident);
            }
        } else {
            if (cardMedia != null) cardMedia.setVisibility(View.GONE);
        }

        Glide.with(this).load(incident.getAuteurPhotoUrl()).circleCrop()
                .placeholder(R.drawable.ic_profile).into(imgProfile);

        // --- ECOUTEUR TEMPS RÉEL (Likes/Comments/Heart) ---
        ListenerRegistration sheetListener = firestoreRepo.getIncidentListener(incident.getId(), new FirestoreRepository.OnIncidentLoadedListener() {
            @Override
            public void onIncidentLoaded(Incident updatedIncident) {
                if (updatedIncident != null && isAdded()) {
                    String myUid = FirebaseAuth.getInstance().getUid();
                    boolean isLikedByMe = updatedIncident.isLikedBy(myUid);

                    // Couleur du cœur
                    if (imgHeart != null) {
                        int heartColor = isLikedByMe ? android.R.color.holo_red_dark : android.R.color.black;
                        imgHeart.setColorFilter(ContextCompat.getColor(requireContext(), heartColor));
                    }
                    // Compteurs
                    if (tvLikes != null) tvLikes.setText(updatedIncident.getLikesCount() + " J'aime");
                    if (tvComments != null) tvComments.setText(updatedIncident.getCommentsCount() + " comm.");
                }
            }
            @Override public void onError(Exception e) {}
        });

        // Action Like
        if (imgHeart != null) {
            imgHeart.setOnClickListener(v -> {
                String myUid = FirebaseAuth.getInstance().getUid();
                if (myUid != null) firestoreRepo.toggleLike(incident.getId(), myUid);
            });
        }

        // Action Commentaires
        if (tvComments != null) {
            tvComments.setOnClickListener(v -> {
                dialog.dismiss();
                CommentFragment.newInstance(incident.getId()).show(getParentFragmentManager(), "CommentBottomSheet");
            });
        }

        dialog.setOnDismissListener(d -> {
            if (sheetListener != null) sheetListener.remove();
        });

        dialog.show();
    }

    private void searchLocation(String locationName) {
        if (locationName == null || locationName.isEmpty()) return;
        AppExecutors.getInstance().networkIO().execute(() -> {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (addressList != null && !addressList.isEmpty() && isAdded()) {
                        LatLng latLng = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        searchView.clearFocus();
                    }
                });
            } catch (IOException ignored) {}
        });
    }

    private void moveToCurrentLocation() {
        locationHelper.startLocationUpdates(new LocationHelper.LocationListener() {
            @Override
            public void onLocationReceived(Location location) {
                if (location != null && googleMap != null) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 15));
                }
            }
            @Override public void onLocationError(String message) {
                if (isAdded()) Toast.makeText(getContext(), "GPS : " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) googleMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (locationHelper != null) locationHelper.stopLocationUpdates();
    }
}
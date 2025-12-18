package com.example.safecity.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.utils.FirestoreRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirestoreRepository firestoreRepo;
    private ClusterManager<Incident> clusterManager;

    private Double targetLat = null;
    private Double targetLng = null;

    private List<Incident> allIncidents = new ArrayList<>();
    private ChipGroup chipGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            if (getArguments().containsKey("focus_lat")) {
                targetLat = getArguments().getDouble("focus_lat");
                targetLng = getArguments().getDouble("focus_lng");
            }
        }
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreRepo = new FirestoreRepository();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment_view);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        chipGroup = view.findViewById(R.id.chip_group_filters);

        // --- LISTENER DES CHIPS (FILTRES) ---
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) {
                updateMapMarkers(allIncidents);
            } else if (checkedId == R.id.chip_accident) {
                filterMarkers("Accident");
            } else if (checkedId == R.id.chip_vol) {
                filterMarkers("Vol");
            } else if (checkedId == R.id.chip_incendie) {
                filterMarkers("Incendie");
            } else if (checkedId == R.id.chip_panne) {
                filterMarkers("Panne");
            } else if (checkedId == R.id.chip_autre) {
                filterMarkers("Autre");
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Padding pour éviter que les Chips ne cachent le bouton "Ma Position"
        int topPadding = 180;
        this.googleMap.setPadding(0, topPadding, 0, 0);

        if (targetLat != null && targetLng != null) {
            LatLng target = new LatLng(targetLat, targetLng);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16));
        } else {
            LatLng defaultLocation = new LatLng(34.6814, -1.9076);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        }

        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        // --- CONFIGURATION DU CLUSTER MANAGER ---
        clusterManager = new ClusterManager<>(getContext(), googleMap);
        googleMap.setOnCameraIdleListener(clusterManager);

        // Déléguer le clic sur le marqueur au ClusterManager
        googleMap.setOnMarkerClickListener(clusterManager);

        // Gérer le clic sur un Item (Incident) du cluster
        clusterManager.setOnClusterItemClickListener(incident -> {
            showIncidentBottomSheet(incident);
            return true; // Indique que l'événement est consommé
        });

        enableUserLocation();
        loadIncidentMarkers();
    }

    // --- AFFICHER LE BOTTOM SHEET (DÉTAILS INCIDENT) ---
    private void showIncidentBottomSheet(Incident incident) {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.layout_map_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        // Binding des vues du BottomSheet
        ImageView imgProfile = sheetView.findViewById(R.id.sheet_img_profile);
        TextView tvUsername = sheetView.findViewById(R.id.sheet_tv_username);
        TextView tvCategory = sheetView.findViewById(R.id.sheet_tv_category);
        TextView tvDescription = sheetView.findViewById(R.id.sheet_tv_description);
        ImageView imgIncident = sheetView.findViewById(R.id.sheet_img_incident);
        TextView tvLikes = sheetView.findViewById(R.id.sheet_tv_likes);

        // Remplissage des données
        tvUsername.setText(incident.getNomUtilisateur());
        tvCategory.setText(incident.getNomCategorie());
        tvDescription.setText(incident.getDescription());
        tvLikes.setText(incident.getLikesCount() + " J'aime");

        // Avatar utilisateur
        if (incident.getAuteurPhotoUrl() != null && !incident.getAuteurPhotoUrl().isEmpty()) {
            Glide.with(this).load(incident.getAuteurPhotoUrl()).circleCrop().into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.ic_profile);
        }

        // Photo Incident (Gestion adaptative)
        // Note: Assurez-vous que la méthode hasMedia() existe dans votre modèle Incident,
        // sinon remplacez par : if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty())
        if (incident.hasMedia()) {
            imgIncident.setVisibility(View.VISIBLE);
            Glide.with(this).load(incident.getPhotoUrl()).into(imgIncident);
        } else {
            imgIncident.setVisibility(View.GONE);
        }

        bottomSheetDialog.show();
    }

    private void loadIncidentMarkers() {
        firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (!isAdded()) return;
                allIncidents = incidents;

                // Réappliquer le filtre courant si nécessaire
                int checkedId = chipGroup.getCheckedChipId();
                if (checkedId == R.id.chip_accident) filterMarkers("Accident");
                else if (checkedId == R.id.chip_vol) filterMarkers("Vol");
                else if (checkedId == R.id.chip_incendie) filterMarkers("Incendie");
                else if (checkedId == R.id.chip_panne) filterMarkers("Panne");
                else if (checkedId == R.id.chip_autre) filterMarkers("Autre");
                else updateMapMarkers(allIncidents);
            }

            @Override
            public void onError(Exception e) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Erreur chargement carte", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void filterMarkers(String categoryName) {
        List<Incident> filteredList = new ArrayList<>();
        if (allIncidents != null) {
            for (Incident inc : allIncidents) {
                if (inc.getNomCategorie() != null &&
                        inc.getNomCategorie().toLowerCase().contains(categoryName.toLowerCase())) {
                    filteredList.add(inc);
                }
            }
        }
        updateMapMarkers(filteredList);
    }

    private void updateMapMarkers(List<Incident> incidentsToDisplay) {
        if (googleMap == null || incidentsToDisplay == null || clusterManager == null) return;

        clusterManager.clearItems();

        for (Incident inc : incidentsToDisplay) {
            // Filtrage basique des coordonnées invalides (0,0)
            if (Math.abs(inc.getLatitude()) < 0.0001 && Math.abs(inc.getLongitude()) < 0.0001) {
                continue;
            }
            clusterManager.addItem(inc);
        }
        clusterManager.cluster();
    }

    public void enableUserLocation() {
        if (getContext() == null) return;
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
        }
    }
}
package com.example.safecity.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.example.safecity.utils.FirestoreRepository;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.chip.ChipGroup;
import com.google.maps.android.clustering.ClusterManager; // <--- Import ClusterManager

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirestoreRepository firestoreRepo;
    private ClusterManager<Incident> clusterManager; // <--- Gestionnaire de clusters

    // Variables pour stocker la position cible reçue en argument
    private Double targetLat = null;
    private Double targetLng = null;

    // Champs pour le filtrage
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

        // --- GESTION DES CLICS SUR LES FILTRES (CHIPS) ---
        chipGroup = view.findViewById(R.id.chip_group_filters);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) {
                updateMapMarkers(allIncidents);
            } else if (checkedId == R.id.chip_accident) {
                filterMarkers("Accident");
            } else if (checkedId == R.id.chip_vol) {
                filterMarkers("Vol");
            } else if (checkedId == R.id.chip_travaux) {
                filterMarkers("Travaux");
            } else if (checkedId == R.id.chip_incendie) {
                filterMarkers("Incendie");
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Position initiale
        if (targetLat != null && targetLng != null) {
            LatLng target = new LatLng(targetLat, targetLng);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16));
        } else {
            LatLng defaultLocation = new LatLng(34.6814, -1.9076);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        }

        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        // --- CONFIGURATION DU CLUSTER MANAGER ---
        // 1. Initialiser le ClusterManager avec le contexte et la carte
        clusterManager = new ClusterManager<>(getContext(), googleMap);

        // 2. Déléguer les événements de la carte au ClusterManager
        // C'est lui qui va gérer le zoom et les clics sur les marqueurs maintenant
        googleMap.setOnCameraIdleListener(clusterManager);
        googleMap.setOnMarkerClickListener(clusterManager);

        // (Optionnel) Ajout d'une info-bulle ou action au clic sur un incident individuel
        // clusterManager.setOnClusterItemClickListener(incident -> { ... });

        enableUserLocation();
        loadIncidentMarkers(); // Chargement initial des données
    }

    private void loadIncidentMarkers() {
        firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                allIncidents = incidents;

                // Ré-appliquer le filtre actuel si nécessaire
                int checkedId = chipGroup.getCheckedChipId();
                if (checkedId == R.id.chip_accident) filterMarkers("Accident");
                else if (checkedId == R.id.chip_vol) filterMarkers("Vol");
                else if (checkedId == R.id.chip_travaux) filterMarkers("Travaux");
                else if (checkedId == R.id.chip_incendie) filterMarkers("Incendie");
                else updateMapMarkers(allIncidents);
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
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

    // --- MISE À JOUR VIA CLUSTER MANAGER ---
    private void updateMapMarkers(List<Incident> incidentsToDisplay) {
        if (googleMap == null || incidentsToDisplay == null || clusterManager == null) return;

        // 1. On nettoie les items du ClusterManager (pas googleMap.clear())
        clusterManager.clearItems();

        for (Incident inc : incidentsToDisplay) {
            // Filtrer les coordonnées invalides
            if (Math.abs(inc.getLatitude()) < 0.0001 && Math.abs(inc.getLongitude()) < 0.0001) {
                continue;
            }

            // 2. On ajoute l'incident (qui est un ClusterItem) au manager
            clusterManager.addItem(inc);
        }

        // 3. On force le recalcul des clusters
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

    public void focusOnLocation(double lat, double lng) {
        if (googleMap != null) {
            LatLng pos = new LatLng(lat, lng);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
        } else {
            targetLat = lat;
            targetLng = lng;
        }
    }
}
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
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.utils.LocationHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment gérant la cartographie interactive.
 * Intègre la recherche de lieux, le filtrage par catégorie et le clustering d'incidents.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirestoreRepository firestoreRepo;
    private ClusterManager<Incident> clusterManager;
    private LocationHelper locationHelper;

    private Double targetLat = null;
    private Double targetLng = null;

    private List<Incident> allIncidents = new ArrayList<>();
    private ChipGroup chipGroup;
    private SearchView searchView;
    private FloatingActionButton fabLocation;

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
        // Correction : Le constructeur de LocationHelper attend un seul argument (Activity)
        locationHelper = new LocationHelper(requireActivity());

        // Liaison des vues
        chipGroup = view.findViewById(R.id.chip_group_filters);
        searchView = view.findViewById(R.id.map_search_view);
        fabLocation = view.findViewById(R.id.fab_my_location);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment_view);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupListeners();
    }

    private void setupListeners() {
        // 1. Recherche de lieu via Geocoding
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });

        // 2. Bouton Ma Position
        fabLocation.setOnClickListener(v -> moveToCurrentLocation());

        // 3. Filtrage par Chips
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

    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                searchView.clearFocus();
            } else {
                Toast.makeText(getContext(), "Lieu non trouvé", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erreur de service de recherche", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveToCurrentLocation() {
        // Correction : Utilisation de la méthode getLastKnownLocation de LocationHelper
        locationHelper.getLastKnownLocation(location -> {
            if (location != null && googleMap != null) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15));
            } else {
                Toast.makeText(getContext(), "GPS non disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Décalage des contrôles Google (Zoom, Google Logo) pour ne pas être cachés par nos vues
        this.googleMap.setPadding(0, 320, 0, 0);

        if (targetLat != null && targetLng != null) {
            LatLng target = new LatLng(targetLat, targetLng);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16));
        } else {
            LatLng defaultLocation = new LatLng(34.6814, -1.9076);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        }

        googleMap.getUiSettings().setZoomControlsEnabled(false); // On cache les boutons +/- pour un look plus propre
        googleMap.getUiSettings().setMyLocationButtonEnabled(false); // On utilise notre FAB personnalisé

        // Configuration du Clustering
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

    private void showIncidentBottomSheet(Incident incident) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_map_bottom_sheet, null);
        dialog.setContentView(view);

        ImageView imgProfile = view.findViewById(R.id.sheet_img_profile);
        TextView tvUsername = view.findViewById(R.id.sheet_tv_username);
        TextView tvCategory = view.findViewById(R.id.sheet_tv_category);
        TextView tvDescription = view.findViewById(R.id.sheet_tv_description);
        ImageView imgIncident = view.findViewById(R.id.sheet_img_incident);
        TextView tvLikes = view.findViewById(R.id.sheet_tv_likes);

        tvUsername.setText(incident.getNomUtilisateur());
        tvCategory.setText(incident.getNomCategorie());
        tvDescription.setText(incident.getDescription());

        // Correction : L'id sheet_tv_status n'existe pas dans le layout, on utilise tvLikes pour l'engagement
        if (tvLikes != null) tvLikes.setText(incident.getLikesCount() + " J'aime");

        Glide.with(this).load(incident.getAuteurPhotoUrl()).circleCrop().placeholder(R.drawable.ic_profile).into(imgProfile);

        if (incident.hasMedia()) {
            imgIncident.setVisibility(View.VISIBLE);
            Glide.with(this).load(incident.getPhotoUrl()).into(imgIncident);
        } else {
            imgIncident.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void loadIncidentMarkers() {
        firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (!isAdded()) return;
                allIncidents = incidents;
                updateMapMarkers(allIncidents);
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    private void filterMarkers(String categoryName) {
        List<Incident> filteredList = new ArrayList<>();
        for (Incident inc : allIncidents) {
            if (inc.getNomCategorie() != null && inc.getNomCategorie().equalsIgnoreCase(categoryName)) {
                filteredList.add(inc);
            }
        }
        updateMapMarkers(filteredList);
    }

    private void updateMapMarkers(List<Incident> incidentsToDisplay) {
        if (googleMap == null || clusterManager == null) return;
        clusterManager.clearItems();
        for (Incident inc : incidentsToDisplay) {
            if (Math.abs(inc.getLatitude()) > 0.001) clusterManager.addItem(inc);
        }
        clusterManager.cluster();
    }

    public void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) googleMap.setMyLocationEnabled(true);
        }
    }
}
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
 * Fragment gérant la cartographie interactive de SafeCity.
 * Gère le clustering des incidents, la recherche de lieux et l'affichage des détails.
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
        locationHelper = new LocationHelper(requireContext());

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

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) updateMapMarkers(allIncidents);
            else if (checkedId == R.id.chip_accident) filterMarkers("Accident");
            else if (checkedId == R.id.chip_vol) filterMarkers("Vol");
            else if (checkedId == R.id.chip_incendie) filterMarkers("Incendie");
            else if (checkedId == R.id.chip_panne) filterMarkers("Panne");
            else if (checkedId == R.id.chip_autre) filterMarkers("Autre");
        });
    }

    private void searchLocation(String locationName) {
        if (locationName == null || locationName.isEmpty()) return;

        AppExecutors.getInstance().networkIO().execute(() -> {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try {
                List<Address> addressList = geocoder.getFromLocationName(locationName, 3);

                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (addressList != null && !addressList.isEmpty()) {
                        Address address = addressList.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                        if (googleMap != null && isAdded()) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            searchView.clearFocus();
                        }
                    } else {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Lieu introuvable : " + locationName, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (IOException e) {
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Service de recherche indisponible", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void moveToCurrentLocation() {
        locationHelper.startLocationUpdates(new LocationHelper.LocationListener() {
            @Override
            public void onLocationReceived(Location location) {
                if (location != null && googleMap != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15));
                }
            }
            @Override
            public void onLocationError(String message) {
                if (isAdded()) Toast.makeText(getContext(), "Erreur GPS : " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        this.googleMap.setPadding(0, 180, 0, 0);

        if (targetLat != null && targetLng != null) {
            LatLng target = new LatLng(targetLat, targetLng);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16));
        } else {
            LatLng defaultPos = new LatLng(34.6814, -1.9076);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 12));
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
     * Affiche le détail de l'incident avec synchronisation temps réel.
     */
    private void showIncidentBottomSheet(Incident incident) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_map_bottom_sheet, null);
        dialog.setContentView(view);

        ImageView imgProfile = view.findViewById(R.id.sheet_img_profile);
        TextView tvUsername = view.findViewById(R.id.sheet_tv_username);
        TextView tvCategory = view.findViewById(R.id.sheet_tv_category);
        TextView tvStatus = view.findViewById(R.id.sheet_tv_status);
        TextView tvDescription = view.findViewById(R.id.sheet_tv_description);
        ImageView imgIncident = view.findViewById(R.id.sheet_img_incident);
        View cardMedia = view.findViewById(R.id.card_incident_media);

        // Liaison avec les composants du layout XML
        ImageView imgHeart = view.findViewById(R.id.sheet_img_heart);
        TextView tvLikes = view.findViewById(R.id.sheet_tv_likes);
        TextView tvComments = view.findViewById(R.id.sheet_tv_comments);

        if (tvUsername != null) tvUsername.setText(incident.getNomUtilisateur());
        if (tvCategory != null) tvCategory.setText("🚨 " + incident.getNomCategorie());
        if (tvDescription != null) tvDescription.setText(incident.getDescription());

        if (tvStatus != null) {
            tvStatus.setText(incident.getStatut());
            if ("Traité".equalsIgnoreCase(incident.getStatut())) {
                tvStatus.setBackgroundResource(R.drawable.status_traite_bg);
            }
        }

        // --- GESTION TEMPS RÉEL DU LIKE ET DES COMPTEURS ---
        ListenerRegistration sheetListener = firestoreRepo.getIncidentListener(incident.getId(), new FirestoreRepository.OnIncidentLoadedListener() {
            @Override
            public void onIncidentLoaded(Incident updatedIncident) {
                if (updatedIncident != null && isAdded()) {
                    String myUid = FirebaseAuth.getInstance().getUid();
                    boolean isLikedByMe = updatedIncident.isLikedBy(myUid);

                    // Mise à jour visuelle de la couleur du cœur
                    if (imgHeart != null) {
                        int heartColor = isLikedByMe ? android.R.color.holo_red_dark : android.R.color.black;
                        imgHeart.setColorFilter(ContextCompat.getColor(requireContext(), heartColor));
                    }

                    // Mise à jour des compteurs texte
                    if (tvLikes != null) {
                        tvLikes.setText(updatedIncident.getLikesCount() + " J'aime");
                    }
                    if (tvComments != null) {
                        tvComments.setText(updatedIncident.getCommentsCount() + " comm.");
                    }
                }
            }

            @Override
            public void onError(Exception e) {}
        });

        // Action de clic sur l'icône du cœur
        if (imgHeart != null) {
            imgHeart.setOnClickListener(v -> {
                String myUid = FirebaseAuth.getInstance().getUid();
                if (myUid != null) {
                    firestoreRepo.toggleLike(incident.getId(), myUid);
                }
            });
        }

        if (imgProfile != null) {
            Glide.with(this)
                    .load(incident.getAuteurPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(imgProfile);
        }

        if (imgIncident != null && cardMedia != null) {
            if (incident.hasMedia()) {
                cardMedia.setVisibility(View.VISIBLE);
                imgIncident.setVisibility(View.VISIBLE);
                Glide.with(this).load(incident.getPhotoUrl()).into(imgIncident);
            } else {
                cardMedia.setVisibility(View.GONE);
                imgIncident.setVisibility(View.GONE);
            }
        }

        if (tvComments != null) {
            tvComments.setOnClickListener(v -> {
                dialog.dismiss();
                CommentFragment.newInstance(incident.getId())
                        .show(getParentFragmentManager(), "CommentBottomSheet");
            });
        }

        dialog.setOnDismissListener(d -> {
            if (sheetListener != null) sheetListener.remove();
        });

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
            if (inc.getStatut() != null && !inc.getStatut().equalsIgnoreCase("Traité")) {
                if (Math.abs(inc.getLatitude()) > 0.001) {
                    clusterManager.addItem(inc);
                }
            }
        }
        clusterManager.cluster();
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
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }
}
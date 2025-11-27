package com.example.safecity.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirestoreRepository firestoreRepo;

    // Variables pour stocker la position cible reçue en argument
    private Double targetLat = null;
    private Double targetLng = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. On récupère les arguments passés par MainActivity (s'il y en a)
        if (getArguments() != null) {
            // On vérifie si les clés existent pour éviter les valeurs par défaut (0.0)
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

        // Note: Assurez-vous que l'ID dans fragment_map.xml est bien "map" ou "map_fragment_view"
        // Si vous utilisez <fragment ... android:id="@+id/map" ... /> gardez R.id.map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        // Si votre ID est map_fragment_view, décommentez la ligne ci-dessous :
        // SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment_view);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // 2. Gestion de la position initiale de la caméra
        if (targetLat != null && targetLng != null) {
            // Cas A : On a reçu une demande de focus (clic depuis la liste)
            LatLng target = new LatLng(targetLat, targetLng);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 16)); // Zoom fort
        } else {
            // Cas B : Ouverture normale (Maroc / Oujda)
            LatLng defaultLocation = new LatLng(34.6814, -1.9076);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        }

        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        enableUserLocation();
        loadIncidentMarkers();
    }

    /**
     * Charge les incidents depuis Firestore en temps réel.
     */
    private void loadIncidentMarkers() {
        firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                if (isAdded() && getActivity() != null && googleMap != null) {

                    googleMap.clear();

                    for (Incident inc : incidents) {
                        // Filtrer les coordonnées invalides (0,0)
                        if (Math.abs(inc.getLatitude()) < 0.0001 && Math.abs(inc.getLongitude()) < 0.0001) {
                            continue;
                        }

                        LatLng position = new LatLng(inc.getLatitude(), inc.getLongitude());

                        googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(inc.getNomCategorie() != null ? inc.getNomCategorie() : "Incident")
                                .snippet(inc.getDescription())
                                // Optionnel : icône standard rouge
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Erreur chargement carte", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

    /**
     * METHODE PUBLIQUE appelée par MainActivity pour forcer le focus
     */
    public void focusOnLocation(double lat, double lng) {
        if (googleMap != null) {
            LatLng pos = new LatLng(lat, lng);
            // Animation fluide vers la position
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16));
        } else {
            // Si la carte n'est pas encore prête, on stocke les coords pour onMapReady
            targetLat = lat;
            targetLng = lng;
        }
    }
}
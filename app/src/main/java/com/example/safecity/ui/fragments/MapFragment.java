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
import com.example.safecity.utils.FirestoreRepository; // Nouvel import
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FirestoreRepository firestoreRepo; // Remplacement du DAO

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialisation du repo
        firestoreRepo = new FirestoreRepository();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment_view);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Position par défaut (Ex: Oujda)
        LatLng defaultLocation = new LatLng(34.6814, -1.9076);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Activation de la localisation utilisateur
        enableUserLocation();

        // Chargement des marqueurs depuis Firebase
        loadIncidentMarkers();
    }

    /**
     * Charge les incidents depuis Firestore en temps réel.
     */
    private void loadIncidentMarkers() {
        firestoreRepo.getIncidentsRealtime(new FirestoreRepository.OnDataLoadListener() {
            @Override
            public void onIncidentsLoaded(List<Incident> incidents) {
                // Vérifications de sécurité : Fragment attaché et Map prête
                if (isAdded() && getActivity() != null && googleMap != null) {

                    googleMap.clear(); // Important : On efface les anciens marqueurs avant de remettre les nouveaux

                    for (Incident inc : incidents) {
                        // --- LOGIQUE DE FILTRAGE ---

                        // 1. Si les coordonnées sont exactement (0,0)
                        if (inc.getLatitude() == 0 && inc.getLongitude() == 0) {
                            continue;
                        }

                        // 2. Si les coordonnées sont presque nulles
                        if (Math.abs(inc.getLatitude()) < 0.0001 && Math.abs(inc.getLongitude()) < 0.0001) {
                            continue;
                        }

                        LatLng position = new LatLng(inc.getLatitude(), inc.getLongitude());

                        googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(inc.getNomCategorie() != null ? inc.getNomCategorie() : "Incident")
                                .snippet(inc.getDescription()));
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

    /**
     * Active le point bleu de position de l'utilisateur.
     */
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
     * Centrage de la carte sur une position donnée
     */
    public void centerMapOnLocation(Location location) {
        if (googleMap != null) {
            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
        }
    }
}
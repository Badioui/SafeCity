package com.example.safecity.ui.fragments;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.example.safecity.R;
import com.example.safecity.utils.AppExecutors;
import com.example.safecity.utils.PermissionManager;
import com.example.safecity.dao.IncidentDAO;
import com.example.safecity.model.Incident;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private IncidentDAO incidentDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // Activation de la localisation utilisateur si permissions OK
        enableUserLocation();

        // Chargement des marqueurs
        loadIncidentMarkers();
    }

    /**
     * Charge les incidents depuis la base de données et ajoute des marqueurs sur la carte.
     * Filtre les incidents sans coordonnées précises (0.0, 0.0).
     */
    private void loadIncidentMarkers() {
        // Utilisation de AppExecutors pour le background thread
        AppExecutors.getInstance().diskIO().execute(() -> {
            incidentDAO = new IncidentDAO(requireContext());
            incidentDAO.open();
            List<Incident> incidents = incidentDAO.getAllIncidents();
            incidentDAO.close();

            // Retour sur le thread principal pour l'UI
            AppExecutors.getInstance().mainThread().execute(() -> {
                // Vérifications de sécurité : Fragment attaché et Map prête
                if (isAdded() && getActivity() != null && googleMap != null) {
                    for (Incident inc : incidents) {

                        // --- LOGIQUE DE FILTRAGE RENFORCÉE ---

                        // 1. Si les coordonnées sont exactement (0,0)
                        if (inc.getLatitude() == 0 && inc.getLongitude() == 0) {
                            continue;
                        }

                        // 2. Si les coordonnées sont presque nulles (erreur capteur ou approximation)
                        if (Math.abs(inc.getLatitude()) < 0.0001 && Math.abs(inc.getLongitude()) < 0.0001) {
                            continue;
                        }

                        LatLng position = new LatLng(inc.getLatitude(), inc.getLongitude());

                        googleMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(inc.getStatut()) // Titre (ex: Nouveau, En cours)
                                .snippet(inc.getDescription())); // Description
                    }
                }
            });
        });
    }

    /**
     * Active le point bleu de position de l'utilisateur.
     */
    public void enableUserLocation() {
        if (googleMap != null && PermissionManager.checkAllPermissions(requireContext())) {
            try {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            } catch (SecurityException e) {
                e.printStackTrace();
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
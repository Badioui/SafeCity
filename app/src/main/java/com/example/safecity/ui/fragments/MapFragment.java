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
import com.example.safecity.R;
import com.example.safecity.utils.PermissionManager; // Import B2

// 💡 Le Fragment implémente l'interface de callback.
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;

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

        // Définir une position par défaut (Ex: Paris)
        LatLng defaultLocation = new LatLng(48.8566, 2.3522);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        // --- Logique B2 : Vérifier la permission au chargement ---
        enableUserLocation();

        // TO-DO B3: Charger les marqueurs des incidents.
    }

    /**
     * Tâche B2: Active le point bleu de position de l'utilisateur.
     * Cette méthode est appelée après onMapReady et après l'accord des permissions.
     */
    public void enableUserLocation() {
        // Vérifie si la carte est initialisée ET si nous avons la permission FINE_LOCATION
        if (googleMap != null && PermissionManager.checkAllPermissions(requireContext())) {
            try {
                // Active le bouton et la couche de localisation
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
            } catch (SecurityException e) {
                // Le catch est théoriquement inutile grâce à la vérification de PermissionManager
                e.printStackTrace();
            }
        }
    }

    /**
     * Centrage de la carte sur une position donnée (optionnel pour B2/C2)
     */
    public void centerMapOnLocation(Location location) {
        if (googleMap != null) {
            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
        }
    }
}
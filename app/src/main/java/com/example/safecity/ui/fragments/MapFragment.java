package com.example.safecity.ui.fragments;

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

        // 1. Récupérer l'instance du SupportMapFragment (enfant).
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment_view);

        // 2. Initialisation asynchrone de la carte.
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Appelée lorsque la carte est prête à être utilisée (objet GoogleMap disponible).
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // --- Configuration Initiale de la Carte ---

        // Définir une position par défaut (Ex: Paris)
        LatLng defaultLocation = new LatLng(48.8566, 2.3522);

        // Déplacer la caméra vers cette position avec un niveau de zoom de 12
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        // Optionnel: Activer les contrôles de zoom intégrés.
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        // TO-DO B3: C'est ici que nous chargerons les marqueurs des incidents.
    }

    // Vous pouvez ajouter ici la gestion de l'activation/désactivation de la position de l'utilisateur (B2)
    // une fois que la carte (googleMap) est disponible et que les permissions sont accordées.

    // ... code restant ...
}
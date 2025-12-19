package com.example.safecity.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Utilitaire pour simplifier la récupération de la position GPS de l'utilisateur.
 * Cette version conserve votre code original pour ne pas casser les autres fichiers,
 * tout en ajoutant la compatibilité pour MapFragment.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context; // ✅ Correction du crash (contexte conservé)
    private LocationCallback locationCallback;

    // --- VOTRE CODE ORIGINAL (NE PAS TOUCHER) ---
    public interface LocationListener {
        void onLocationReceived(Location location);
        void onLocationError(String message);
    }

    public LocationHelper(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates(LocationListener listener) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationError("Permission de localisation manquante.");
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.i(TAG, "Dernière position connue trouvée.");
                        listener.onLocationReceived(location);
                    } else {
                        Log.w(TAG, "Dernière position nulle, lancement de la recherche GPS...");
                        requestNewLocationData(listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur getLastLocation: " + e.getMessage());
                    requestNewLocationData(listener);
                });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(LocationListener listener) {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setMinUpdateDistanceMeters(10)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Log.i(TAG, "Nouvelle position GPS reçue.");
                        listener.onLocationReceived(location);
                        stopLocationUpdates();
                        return;
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Recherche satellite active en cours...");
        } catch (Exception e) {
            Log.e(TAG, "Erreur démarrage localisation : " + e.getMessage());
            listener.onLocationError("Erreur système localisation.");
        }
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
            Log.d(TAG, "Mises à jour de localisation arrêtées.");
        }
    }

    // --- AJOUT POUR MAPFRAGMENT (SANS TOUCHER AU RESTE) ---

    public interface OnLocationResultListener {
        void onLocationResult(Location location);
    }

    /**
     * Nouvelle méthode ajoutée pour la compatibilité avec MapFragment.
     */
    @SuppressLint("MissingPermission")
    public void getLastKnownLocation(OnLocationResultListener listener) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationResult(null);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                listener.onLocationResult(location);
            } else {
                // Rabattre sur une requête unique si le cache est vide
                requestSingleUpdate(listener);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestSingleUpdate(OnLocationResultListener listener) {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                listener.onLocationResult(locationResult.getLastLocation());
                fusedLocationClient.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper());
    }
}
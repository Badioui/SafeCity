package com.example.safecity.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationHelper {

    private static final String TAG = "LocationHelper";
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context; // ✅ Correction du crash (contexte conservé)
    private LocationCallback locationCallback;

    public interface LocationListener {
        void onLocationReceived(Location location);
        void onLocationError(String message);
    }

    public LocationHelper(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Démarre la récupération de la localisation.
     * Tente d'abord la dernière position connue (rapide), sinon lance le GPS.
     */
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(LocationListener listener) {
        // Vérification des permissions via le contexte de l'application
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            listener.onLocationError("Permission de localisation manquante.");
            return;
        }

        // 1. Essayer d'abord d'avoir la dernière position connue (Instantané)
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

    /**
     * Lance une nouvelle recherche GPS active (si la dernière position est vide).
     */
    @SuppressLint("MissingPermission")
    private void requestNewLocationData(LocationListener listener) {
        // Configuration de la requête : Haute précision, update toutes les 5s
        // Utiliser BALANCED_POWER_ACCURACY permet d'utiliser le Wifi/Réseau si le GPS capte mal
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setMinUpdateDistanceMeters(10) // Accepter un déplacement de 10m minimum
                .build();


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Log.i(TAG, "Nouvelle position GPS reçue.");
                        listener.onLocationReceived(location);
                        stopLocationUpdates(); // On arrête après avoir reçu une position pour économiser la batterie
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

    /**
     * Arrête le service de localisation
     */
    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
            Log.d(TAG, "Mises à jour de localisation arrêtées.");
        }
    }
}
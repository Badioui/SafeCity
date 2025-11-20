package com.example.safecity;

import android.Manifest;
import android.content.pm.PackageManager;
import android .location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener {
    private static final int PERMS_CALL_ID = 1234;
    private LocationManager lm;
    private GoogleMap googleMap;
    //tache B5 B6
    private NotificationHelper notificationHelper;
    private int notificationCount = 0;
    //tache B7
    private boolean hasAlertedProximity = false;
    //tache C3
    private static final int REQUEST_IMAGE_CAPTURE =1 ;
    //***************************************************
    //CYCLE DE VIE ET INITIALISATION DE LA MAP
    //***************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialisation de la carte (B1)
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (mapFragment != null){
            mapFragment.getMapAsync(this);
        }
         //initialisation du Helper de Notification (B5)
        notificationHelper = new NotificationHelper(this);

        //demande des permissions
        checkPermissions();
    }

    //*****************************************************
    // gestion des permissions
    //*****************************************************

    private void checkPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMS_CALL_ID);
        }
        //tache B5: demande de permission de notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMS_CALL_ID);
            }
        }
        ///tache c2 : ajout de la permission de caméra au runtime
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Corrected super call
        if (requestCode == PERMS_CALL_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permissions de localisation accordées, on démarre le GPS
                startLocationUpdates();
            } else {
                Toast.makeText(this, "les permissions de localisation sont nécessaires.", Toast.LENGTH_LONG).show();
            }
        }
    }

    //****************************************************
    // logique de carte (B1,B4)
    //****************************************************

    @Override
    public void onMapReady(@NonNull GoogleMap map){
        googleMap = map;
        googleMap.setOnMapLongClickListener(this);
        googleMap.setOnMarkerClickListener(this);
        loadMapLogic(); //chargement initial
    }

    private void loadMapLogic(){
        if (googleMap == null) return;
         //marqueur initial (B3)
        LatLng initialPos = new LatLng(43.799345, 6.7254267);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPos, 15));
        googleMap.addMarker(new MarkerOptions()
                .position(initialPos)
                .title("Point de départ/Test"));

        //active le calque de position de user (point bleu)
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            googleMap.setMyLocationEnabled(true);
            startLocationUpdates();
        }
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        // Enregistrement de l'incident (B4 et A3)
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

        Marker newMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Nouveau Signalement")
                .snippet("Type: Test, Date: Maintenant"));

        if (newMarker != null) {
            newMarker.showInfoWindow();
            Toast.makeText(this, "Signalement créé.", Toast.LENGTH_LONG).show();

            // TÂCHE B6 : SIMULATION DE L'ENVOI DE NOTIFICATION
            notificationCount++;
            notificationHelper.sendAlertNotification(
                    "Nouveau Signalement (Tâche B6)",
                    "Un nouveau signalement a été créé à: " + latLng.latitude,
                    notificationCount
            );

            // Tâche C2/C3 : Déclenchement de l'appareil photo
            // Vous ajouterez l'appel à la fonction de prise de photo ici
            // dispatchTakePictureIntent();
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        // Logique de B4 : Affichage des détails ou redirection vers un écran de détail
        // Le snippet est déjà affiché par défaut, ici on peut juste retourner false
        return false;
    }

    // **********************************
    // GESTION GPS (B2)
    // **********************************

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm != null) {
                // Demande des mises à jour GPS (toutes les 5 secondes ou 10 mètres)
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Tâche B7 : Vérification de la proximité à chaque mise à jour GPS
        checkProximity(latitude, longitude);

        // Déplace la carte si la première position est reçue
        if (googleMap != null) {
            LatLng userPos = new LatLng(latitude, longitude);
            // googleMap.animateCamera(CameraUpdateFactory.newLatLng(userPos)); // Optionnel
        }
    }

    // **********************************
    // LOGIQUE DE PROXIMITÉ (B7)
    // **********************************

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0]; // Distance en mètres
    }

    private void checkProximity(double userLat, double userLon) {
        // Incident simulé à New York pour le test B7
        double simulatedIncidentLat = 40.7128;
        double simulatedIncidentLon = -74.0060;
        final double ALERT_RADIUS_METERS = 500.0;

        double distance = calculateDistance(userLat, userLon, simulatedIncidentLat, simulatedIncidentLon);

        if (distance < ALERT_RADIUS_METERS && !hasAlertedProximity) {
            notificationHelper.sendAlertNotification(
                    "ALERTE DE PROXIMITÉ (Tâche B7)",
                    "Vous êtes à " + (int) distance + " mètres d'un incident simulé.",
                    999
            );
            hasAlertedProximity = true;
        } else if (distance >= ALERT_RADIUS_METERS) {
            // Réinitialise l'alerte une fois que l'utilisateur s'éloigne
            hasAlertedProximity = false;
        }
    }
}







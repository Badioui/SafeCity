package com.example.safecity;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

// Imports de vos fragments
import com.example.safecity.ui.fragments.HomeFragment;
import com.example.safecity.ui.fragments.MapFragment;
import com.example.safecity.ui.fragments.SignalementFragment;
import com.example.safecity.ui.fragments.ProfileFragment;
import com.example.safecity.ui.fragments.NotificationsFragment;

// Import du Helper GPS
import com.example.safecity.utils.LocationHelper;

public class MainActivity extends AppCompatActivity implements LocationHelper.LocationListener {

    private BottomNavigationView bottomNav;
    private ImageButton btnSearch;
    private FloatingActionButton btnSos;

    // Variable pour le GPS
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ==================================================================
        // 0. INIT GPS
        // ==================================================================
        locationHelper = new LocationHelper(this);
        checkPermissionsAndStartGPS();

        // --- GESTION CLIC NOTIFICATION (Ouverture à froid) ---
        handleNotificationIntent(getIntent());

        // ==================================================================
        // 1. GESTION DES NOTIFICATIONS
        // ==================================================================
        FirebaseMessaging.getInstance().subscribeToTopic("incidents_all")
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FCM", "Echec abonnement topic", task.getException());
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // ==================================================================
        // 2. BOUTONS FLOTTANTS (SOS)
        // ==================================================================

        // --- A. BOUTON SOS ---
        btnSos = findViewById(R.id.btn_sos);
        btnSos.setOnClickListener(v -> {
            Toast.makeText(this, "Maintenez appuyé pour appeler les secours (19)", Toast.LENGTH_SHORT).show();
        });
        btnSos.setOnLongClickListener(v -> {
            lancerAppelUrgence();
            return true;
        });

        // --- CORRECTION SÉCURITÉ (Problème n°8) ---
        // L'ancien bloc qui gérait le bouton "Stats" ici a été SUPPRIMÉ.
        // La gestion des statistiques et du rôle admin est désormais centralisée
        // et sécurisée dans le HomeFragment via Firestore.

        // ==================================================================
        // 3. GESTION DE L'INTERFACE
        // ==================================================================
        bottomNav = findViewById(R.id.bottom_nav_bar);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.nav_create) {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(this, "Veuillez vous connecter pour signaler un incident", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    return false;
                }
                selectedFragment = new SignalementFragment();
            } else if (itemId == R.id.nav_activity) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .addToBackStack(null)
                        .commit();
            }
            return true;
        });

        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(v -> showSearchDialog());

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    // ==================================================================
    // GESTION DES INTENTS (App déjà ouverte en fond)
    // ==================================================================
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            String latStr = intent.getStringExtra("lat");
            String lngStr = intent.getStringExtra("lng");

            if (latStr != null && lngStr != null) {
                try {
                    double lat = Double.parseDouble(latStr);
                    double lng = Double.parseDouble(lngStr);
                    navigateToMapAndFocus(lat, lng);
                    Toast.makeText(this, "Incident localisé !", Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    Log.e("Notif", "Erreur format GPS", e);
                }
            }
        }
    }

    // ==================================================================
    // MÉTHODES UTILITAIRES
    // ==================================================================

    private void lancerAppelUrgence() {
        Uri number = Uri.parse("tel:19");
        Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Impossible de lancer l'appel.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSearchDialog() {
        final EditText searchInput = new EditText(this);
        searchInput.setHint("Ex: Vol, Accident, Rue X...");
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50; params.rightMargin = 50;
        searchInput.setLayoutParams(params);
        container.addView(searchInput);

        // --- CORRECTION UX (Problème n°9) ---
        // Modification du titre pour informer l'utilisateur de la limite technique
        new AlertDialog.Builder(this)
                .setTitle("Rechercher (50 derniers incidents)")
                .setView(container)
                .setPositiveButton("Chercher", (dialog, which) -> {
                    String query = searchInput.getText().toString().trim();
                    if (!query.isEmpty()) performSearch(query);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void performSearch(String query) {
        bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
        HomeFragment homeFragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("search_query", query);
        homeFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, homeFragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToMapAndFocus(double lat, double lng) {
        bottomNav.getMenu().findItem(R.id.nav_map).setChecked(true);
        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble("focus_lat", lat);
        args.putDouble("focus_lng", lng);
        mapFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, mapFragment)
                .addToBackStack(null)
                .commit();
    }

    // ==================================================================
    // GESTION GPS & PERMISSIONS
    // ==================================================================

    private void checkPermissionsAndStartGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
        } else {
            locationHelper.startLocationUpdates(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationHelper.startLocationUpdates(this);
        }
    }

    @Override
    public void onLocationReceived(Location location) {
        // Sauvegarde de la dernière position connue
        getSharedPreferences("safe_city_prefs", MODE_PRIVATE).edit()
                .putFloat("last_lat", (float) location.getLatitude())
                .putFloat("last_lng", (float) location.getLongitude())
                .apply();
        Log.d("MainActivity", "Position mise à jour : " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onLocationError(String message) {
        Log.e("MainActivity", "Erreur GPS : " + message);
    }
}
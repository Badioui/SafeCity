package com.example.safecity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

// Imports de vos fragments
import com.example.safecity.ui.fragments.HomeFragment;
import com.example.safecity.ui.fragments.MapFragment;
import com.example.safecity.ui.fragments.SignalementFragment;
import com.example.safecity.ui.fragments.ProfileFragment;
import com.example.safecity.ui.fragments.NotificationsFragment; // <--- NOUVEL IMPORT

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ImageButton btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ==================================================================
        // 1. GESTION DES NOTIFICATIONS (ABONNEMENT + PERMISSIONS)
        // ==================================================================

        // A. S'abonner au canal global "incidents_all"
        FirebaseMessaging.getInstance().subscribeToTopic("incidents_all")
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Log.e("FCM", "Echec abonnement topic", task.getException());
                    }
                });

        // B. Demander la permission explicite sur Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // ==================================================================
        // 2. GESTION DE L'INTERFACE (NAVIGATION)
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
                // MODIFICATION : Ouvre le fragment Notifications
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

        // --- GESTION DE LA RECHERCHE ---
        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(v -> showSearchDialog());

        // Charger l'Accueil par défaut au démarrage
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void showSearchDialog() {
        final EditText searchInput = new EditText(this);
        searchInput.setHint("Ex: Vol, Accident, Rue X...");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = 50;
        params.rightMargin = 50;
        searchInput.setLayoutParams(params);
        container.addView(searchInput);

        new AlertDialog.Builder(this)
                .setTitle("Rechercher un incident")
                .setView(container)
                .setPositiveButton("Chercher", (dialog, which) -> {
                    String query = searchInput.getText().toString().trim();
                    if (!query.isEmpty()) {
                        performSearch(query);
                    }
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
}
package com.example.safecity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.safecity.model.Utilisateur;
import com.example.safecity.ui.fragments.HomeFragment;
import com.example.safecity.ui.fragments.MapFragment;
import com.example.safecity.ui.fragments.NotificationsFragment;
import com.example.safecity.ui.fragments.ProfileFragment;
import com.example.safecity.ui.fragments.SignalementFragment;
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.utils.LocationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Activité principale optimisée.
 * Le bouton recherche utilise désormais Material Design 3 et supporte la touche Entrée.
 */
public class MainActivity extends AppCompatActivity implements LocationHelper.LocationListener {

    private BottomNavigationView bottomNav;
    private ImageButton btnSearch;
    private ExtendedFloatingActionButton btnSos;
    private FloatingActionButton fabStats;

    private LocationHelper locationHelper;
    private FirestoreRepository firestoreRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firestoreRepo = new FirestoreRepository();
        locationHelper = new LocationHelper(this);
        checkPermissionsAndStartGPS();

        setupFCM();

        btnSearch = findViewById(R.id.btn_search);
        btnSos = findViewById(R.id.btn_sos);
        fabStats = findViewById(R.id.fab_stats);
        bottomNav = findViewById(R.id.bottom_nav_bar);

        setupFloatingButtons();

        // Liaison du bouton de recherche de la barre supérieure
        btnSearch.setOnClickListener(v -> showSearchDialog());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (itemId == R.id.nav_map) selectedFragment = new MapFragment();
            else if (itemId == R.id.nav_create) {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    redirectToLogin();
                    return false;
                }
                selectedFragment = new SignalementFragment();
            } else if (itemId == R.id.nav_activity) selectedFragment = new NotificationsFragment();
            else if (itemId == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        checkUserRole();
        handleNotificationIntent(getIntent());
    }

    private void setupFCM() {
        FirebaseMessaging.getInstance().subscribeToTopic("incidents_all");
        FirebaseMessaging.getInstance().subscribeToTopic("official_alerts");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void setupFloatingButtons() {
        if (btnSos != null) {
            btnSos.setOnClickListener(v ->
                    Toast.makeText(this, "Maintenez pour appeler les secours (19)", Toast.LENGTH_SHORT).show());

            btnSos.setOnLongClickListener(v -> {
                lancerAppelUrgence();
                return true;
            });
        }

        if (fabStats != null) {
            fabStats.setOnClickListener(v ->
                    Toast.makeText(this, "Accès au tableau de bord Admin", Toast.LENGTH_SHORT).show());
        }
    }

    private void checkUserRole() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && fabStats != null) {
            firestoreRepo.getUser(user.getUid(), new FirestoreRepository.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(Utilisateur utilisateur) {
                    if (utilisateur != null && isRoleAuthorized(utilisateur.getIdRole())) {
                        fabStats.setVisibility(android.view.View.VISIBLE);
                    }
                }
                @Override
                public void onError(Exception e) {}
            });
        }
    }

    private boolean isRoleAuthorized(String role) {
        return "admin".equalsIgnoreCase(role) || "autorite".equalsIgnoreCase(role);
    }

    private void lancerAppelUrgence() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:19"));
        startActivity(callIntent);
    }

    /**
     * Affiche un dialogue de recherche moderne.
     */
    private void showSearchDialog() {
        final EditText input = new EditText(this);
        input.setHint("Ex: Accident, Incendie, Route...");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH); // Définit le bouton "Loupe" sur le clavier

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.topMargin = margin / 2;
        input.setLayoutParams(params);
        container.addView(input);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Rechercher")
                .setMessage("Filtrez les signalements par mot-clé.")
                .setView(container)
                .setPositiveButton("Chercher", (d, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) performSearch(query);
                })
                .setNegativeButton("Annuler", null)
                .create();

        // Gestion du bouton "Chercher" du clavier
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = input.getText().toString().trim();
                if (!query.isEmpty()) performSearch(query);
                dialog.dismiss();
                return true;
            }
            return false;
        });

        dialog.show();
    }

    private void performSearch(String query) {
        if (bottomNav.getSelectedItemId() != R.id.nav_home) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        HomeFragment homeFragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("search_query", query);
        homeFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, homeFragment)
                .commit();
    }

    public void navigateToMapAndFocus(double lat, double lng) {
        bottomNav.setSelectedItemId(R.id.nav_map);
        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble("focus_lat", lat);
        args.putDouble("focus_lng", lng);
        mapFragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, mapFragment)
                .commit();
    }

    private void redirectToLogin() {
        Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("lat")) {
            try {
                double lat = Double.parseDouble(intent.getStringExtra("lat"));
                double lng = Double.parseDouble(intent.getStringExtra("lng"));
                navigateToMapAndFocus(lat, lng);
            } catch (Exception e) {
                Log.e("MainActivity", "Erreur coordonnées notif");
            }
        }
    }

    private void checkPermissionsAndStartGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
        } else {
            locationHelper.startLocationUpdates(this);
        }
    }

    @Override
    public void onLocationReceived(Location location) {
        getSharedPreferences("safe_city_prefs", MODE_PRIVATE).edit()
                .putFloat("last_lat", (float) location.getLatitude())
                .putFloat("last_lng", (float) location.getLongitude()).apply();
    }

    @Override
    public void onLocationError(String message) {
        Log.e("MainActivity", "GPS: " + message);
    }
}
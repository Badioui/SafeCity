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

import com.example.safecity.ui.fragments.HomeFragment;
import com.example.safecity.ui.fragments.MapFragment;
import com.example.safecity.ui.fragments.NotificationsFragment;
import com.example.safecity.ui.fragments.ProfileFragment;
import com.example.safecity.ui.fragments.SignalementFragment;
import com.example.safecity.utils.LocationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements LocationHelper.LocationListener, NotificationsFragment.NotificationNavigationListener {

    private BottomNavigationView bottomNav;
    private ImageButton btnSearch;
    private ExtendedFloatingActionButton btnSos;

    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationHelper = new LocationHelper(this);
        checkPermissionsAndStartGPS();
        setupFCM();

        btnSearch = findViewById(R.id.btn_search);
        btnSos = findViewById(R.id.btn_sos);
        bottomNav = findViewById(R.id.bottom_nav_bar);

        setupFloatingButtons();
        setupBottomNav();

        btnSearch.setOnClickListener(v -> showSearchDialog());

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        subscribeToUserTopic();
        handleNotificationIntent(getIntent());
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.nav_create) {
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    redirectToLogin();
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
                        .commit();
            }
            return true;
        });
    }

    public void navigateToMapAndFocus(double lat, double lng) {
        if (lat == 0 && lng == 0) {
            Toast.makeText(this, "Coordonnées invalides", Toast.LENGTH_SHORT).show();
            return;
        }

        bottomNav.setOnItemSelectedListener(null);
        bottomNav.setSelectedItemId(R.id.nav_map);
        setupBottomNav();

        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putDouble("focus_lat", lat);
        args.putDouble("focus_lng", lng);
        mapFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, mapFragment)
                .commit();
    }

    private void performSearch(String query) {
        bottomNav.setOnItemSelectedListener(null);
        bottomNav.setSelectedItemId(R.id.nav_home);
        setupBottomNav();

        HomeFragment homeFragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("search_query", query);
        homeFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, homeFragment)
                .commit();
    }
    
    @Override
    public void navigateToIncident(String incidentId) {
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        HomeFragment homeFragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("focus_incident_id", incidentId);
        homeFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, homeFragment)
                .addToBackStack(null)
                .commit();
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
    }

    private void subscribeToUserTopic() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userTopic = "user_" + user.getUid();
            FirebaseMessaging.getInstance().subscribeToTopic(userTopic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FCM", "Subscribed to " + userTopic);
                        } else {
                            Log.e("FCM", "Failed to subscribe to " + userTopic);
                        }
                    });
        }
    }

    private void lancerAppelUrgence() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:19"));
        startActivity(callIntent);
    }

    private void showSearchDialog() {
        final EditText input = new EditText(this);
        input.setHint("Ex: Accident, Incendie, Route...");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.topMargin = margin / 2;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Rechercher")
                .setMessage("Filtrez les signalements par mot-clé.")
                .setView(container)
                .setPositiveButton("Chercher", (d, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) performSearch(query);
                })
                .setNegativeButton("Annuler", null)
                .show();
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
        if (intent == null) return;

        if (intent.hasExtra("incidentId")) {
            String incidentId = intent.getStringExtra("incidentId");
            if (incidentId != null && !incidentId.isEmpty()) {
                navigateToIncident(incidentId);
                intent.removeExtra("incidentId");
                return;
            }
        }

        if (intent.hasExtra("lat") && intent.hasExtra("lng")) {
            try {
                double lat = Double.parseDouble(intent.getStringExtra("lat"));
                double lng = Double.parseDouble(intent.getStringExtra("lng"));
                navigateToMapAndFocus(lat, lng);
                intent.removeExtra("lat");
                intent.removeExtra("lng");
            } catch (Exception e) {
                Log.e("MainActivity", "Erreur coordonnées notification pour la carte");
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
        Log.e("MainActivity", "GPS Error: " + message);
    }
}

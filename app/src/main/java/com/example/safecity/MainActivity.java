package com.example.safecity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Imports de vos fragments
import com.example.safecity.ui.fragments.HomeFragment;
import com.example.safecity.ui.fragments.MapFragment;
import com.example.safecity.ui.fragments.SignalementFragment;
import com.example.safecity.ui.fragments.ProfileFragment;
import com.example.safecity.ui.fragments.MyIncidentsFragment;

// Import pour la gestion de l'authentification
import com.example.safecity.utils.AuthManager;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ImageButton btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Gestion de la Navigation du Bas ---
        bottomNav = findViewById(R.id.bottom_nav_bar);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // 🏠 HOME
                selectedFragment = new HomeFragment();

            } else if (itemId == R.id.nav_map) {
                // 🗺️ MAP
                selectedFragment = new MapFragment();

            } else if (itemId == R.id.nav_create) {
                // ➕ CREATE
                // 1. Vérifier si l'utilisateur est connecté (CRUCIAL)
                if (!AuthManager.isLoggedIn(this)) {
                    Toast.makeText(this, "Veuillez vous connecter pour signaler un incident", Toast.LENGTH_SHORT).show();
                    // Rediriger vers Login
                    startActivity(new Intent(this, LoginActivity.class));
                    return false; // Annuler la sélection du menu
                }
                // 2. Si connecté, on ouvre le fragment
                selectedFragment = new SignalementFragment();

            } else if (itemId == R.id.nav_activity) {
                // ❤️ ACTIVITY
                Toast.makeText(this, "Notifications à venir", Toast.LENGTH_SHORT).show();
                return false;

            } else if (itemId == R.id.nav_profile) {
                // 👤 PROFILE
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .addToBackStack(null) // <--- AJOUTÉ : Permet le retour en arrière (popBackStack)
                        .commit();
            }
            return true;
        });

        // --- Gestion du Bouton Recherche (En haut) ---
        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Recherche ouverte 🔍", Toast.LENGTH_SHORT).show();
        });

        // Charger l'Accueil par défaut au démarrage
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
package com.example.safecity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Imports de vos fragments
import com.example.safecity.ui.fragments.HomeFragment;       // Le "Fil d'actu"
import com.example.safecity.ui.fragments.MapFragment;        // La Carte
import com.example.safecity.ui.fragments.SignalementFragment;// Créer (Le +)
import com.example.safecity.ui.fragments.ProfileFragment;    // Profil
import com.example.safecity.ui.fragments.MyIncidentsFragment; // <--- AJOUTER CETTE LIGNE
// import com.example.safecity.ui.fragments.ActivityFragment; // Notifications (à créer)

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
                // 🏠 HOME = Le Fil d'actualité global (Tous les posts)
                selectedFragment = new HomeFragment();

            } else if (itemId == R.id.nav_map) {
                // 🗺️ MAP = La Carte
                selectedFragment = new MapFragment();

            } else if (itemId == R.id.nav_create) {
                // ➕ CREATE = Le Formulaire
                selectedFragment = new SignalementFragment();

            } else if (itemId == R.id.nav_activity) {
                // ❤️ ACTIVITY = Notifications (Placeholder)
                Toast.makeText(this, "Notifications à venir", Toast.LENGTH_SHORT).show();
                return false; // Reste sur la page actuelle

            } else if (itemId == R.id.nav_profile) {
                // 👤 PROFILE = Mon Profil + Mes Incidents
                // Pour l'instant, on utilise MyIncidentsFragment pour voir MES posts
                // Plus tard, on pourra intégrer ça dans un ProfileFragment plus complet
                selectedFragment = new MyIncidentsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
            }
            return true;
        });

        // --- Gestion du Bouton Recherche (En haut) ---
        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(v -> {
            // Idée : Ouvrir un fragment de recherche ou une barre de recherche
            Toast.makeText(MainActivity.this, "Recherche ouverte 🔍", Toast.LENGTH_SHORT).show();
            // Exemple : charger un SearchFragment
        });

        // Charger l'Accueil par défaut au démarrage
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
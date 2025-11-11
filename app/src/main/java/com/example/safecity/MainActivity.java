package com.example.safecity; // Assurez-vous d'utiliser votre package

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.safecity.ui.fragments.MyIncidentsFragment; // À créer
import com.example.safecity.ui.fragments.MapFragment;        // À créer (B1)
import com.example.safecity.ui.fragments.ProfileFragment;    // À créer (C6)
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Utilise le layout modifié de C1
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav_bar);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Pour l'instant, HOME affiche les Signalements personnels (C4)
                selectedFragment = new MyIncidentsFragment();
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment(); // Fragment B1/B3
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment(); // Fragment C6
            } else if (itemId == R.id.nav_create) {
                // Navigue vers l'Activité/Fragment de Création C2
                // Pour l'instant on garde le Fragment de la liste pour la démo
                selectedFragment = new MyIncidentsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Charger l'écran par défaut au démarrage (C4)
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
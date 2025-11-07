package com.example.safecity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.safecity.models.Role;
import com.example.safecity.models.Utilisateur;
import com.example.safecity.models.Incident;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Gestion automatique des marges selon les bordures système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Exemple : création d'un rôle, d'un utilisateur et d'un incident
        Role admin = new Role(1, "Admin");
        Utilisateur user = new Utilisateur(1, "Sam", "sam@mail.com", "hash123", admin, new Date());
        Incident incident = new Incident(1, "url_photo", "Accident route", "Accident",
                33.59, -7.62, new Date(), "Nouveau", user);

        // Affichage dans la console (Logcat)
        System.out.println("Incident créé : " + incident.getDescription());
    }
}

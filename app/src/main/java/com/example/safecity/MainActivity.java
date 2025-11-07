package com.example.safecity;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safecity.dao.IncidentDAO;
import com.example.safecity.dao.UserDAO;
import com.example.safecity.model.Incident;
import com.example.safecity.model.Utilisateur;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private IncidentDAO incidentDAO;
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pas besoin de setContentView si c'est juste du test
        // setContentView(R.layout.activity_main);

        // --- Initialisation DAO ---
        incidentDAO = new IncidentDAO(this);
        userDAO = new UserDAO(this);

        incidentDAO.open();
        userDAO.open();

        testUsers();
        testIncidents();

        userDAO.close();
        incidentDAO.close();
    }

    private void testUsers() {
        Log.i(TAG, "--- TEST USERDAO ---");

        // 1) Création utilisateur
        Utilisateur u = new Utilisateur();
        u.setNom("Alice");
        u.setEmail("alice@example.com");
        u.setIdRole(1); // admin ou user
        long userId = userDAO.insertUser(u, "password123");
        Log.i(TAG, "User inserted with ID: " + userId);

        // 2) Authentification
        Utilisateur auth = userDAO.authenticate("alice@example.com", "password123");
        if (auth != null) {
            Log.i(TAG, "Authentication OK: " + auth.getNom());
        } else {
            Log.e(TAG, "Authentication FAILED");
        }

        // 3) Récupération utilisateur
        Utilisateur user = userDAO.getUserById(userId);
        if (user != null) Log.i(TAG, "User fetched: " + user.getEmail());

        // 4) Changer mot de passe
        boolean changed = userDAO.changePassword(userId, "newpass456");
        Log.i(TAG, "Password changed? " + changed);

        // 5) Compte utilisateurs
        long count = userDAO.countUsers();
        Log.i(TAG, "Total users: " + count);
    }

    private void testIncidents() {
        Log.i(TAG, "--- TEST INCIDENTDAO ---");

        // 1) Création incident
        Incident inc = new Incident();
        inc.setIdUtilisateur(1); // doit correspondre à un user existant
        inc.setDescription("Feu de poubelle");
        inc.setLatitude(34.0219);
        inc.setLongitude(-6.8416);
        inc.setStatut(Incident.STATUT_NOUVEAU);
        long incidentId = incidentDAO.insertIncident(inc);
        Log.i(TAG, "Incident inserted with ID: " + incidentId);

        // 2) Lire incident
        Incident fetched = incidentDAO.getIncidentById(incidentId);
        if (fetched != null) Log.i(TAG, "Fetched incident: " + fetched.getDescription());

        // 3) Update incident
        fetched.setStatut(Incident.STATUT_EN_COURS);
        int updated = incidentDAO.updateIncident(fetched);
        Log.i(TAG, "Incident updated rows: " + updated);

        // 4) Filtrage par statut
        List<Incident> enCours = incidentDAO.getIncidentsByStatut(Incident.STATUT_EN_COURS);
        Log.i(TAG, "Incidents en cours: " + enCours.size());

        // 5) Compte total
        long count = incidentDAO.countIncidents();
        Log.i(TAG, "Total incidents: " + count);

        // 6) Test suppression
        int deleted = incidentDAO.deleteIncident(incidentId);
        Log.i(TAG, "Incident deleted rows: " + deleted);
    }
}


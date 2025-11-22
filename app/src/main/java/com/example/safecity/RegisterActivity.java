package com.example.safecity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.safecity.dao.UserDAO;
import com.example.safecity.model.Utilisateur;
import com.example.safecity.utils.AppExecutors; // Import AppExecutors

public class RegisterActivity extends AppCompatActivity {

    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userDAO = new UserDAO(this);

        EditText etName = findViewById(R.id.et_register_name);
        EditText etEmail = findViewById(R.id.et_register_email);
        EditText etPass = findViewById(R.id.et_register_password);
        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvLogin = findViewById(R.id.tv_go_to_login);

        btnRegister.setOnClickListener(v -> {
            String nom = etName.getText().toString();
            String email = etEmail.getText().toString();
            String pass = etPass.getText().toString();

            if (nom.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Remplissez tout !", Toast.LENGTH_SHORT).show();
                return;
            }

            // Création objet utilisateur (Rôle 3 = Citoyen par défaut)
            Utilisateur user = new Utilisateur();
            user.setNom(nom);
            user.setEmail(email);
            user.setIdRole(3);

            // Insertion en base via AppExecutors (DiskIO)
            AppExecutors.getInstance().diskIO().execute(() -> {
                userDAO.open();
                long id = userDAO.insertUser(user, pass);
                userDAO.close();

                // Mise à jour UI via AppExecutors (MainThread)
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (id > 0) {
                        Toast.makeText(RegisterActivity.this, "Succès ! Connectez-vous.", Toast.LENGTH_SHORT).show();
                        finish(); // Retour au login
                    } else if (id == -2) {
                        Toast.makeText(RegisterActivity.this, "Cet email existe déjà.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Erreur d'inscription.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        tvLogin.setOnClickListener(v -> finish());
    }
}
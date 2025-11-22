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
import com.example.safecity.utils.AuthManager;

public class LoginActivity extends AppCompatActivity {

    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Si déjà connecté, on va direct à l'accueil
        if (AuthManager.isLoggedIn(this)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        userDAO = new UserDAO(this);

        EditText etEmail = findViewById(R.id.et_login_email);
        EditText etPass = findViewById(R.id.et_login_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String pass = etPass.getText().toString();

            // Remplacement de new Thread() par AppExecutors
            AppExecutors.getInstance().diskIO().execute(() -> {
                userDAO.open();
                // Vérification mot de passe
                Utilisateur user = userDAO.authenticate(email, pass);
                userDAO.close();

                // Retour sur le thread principal pour l'UI
                AppExecutors.getInstance().mainThread().execute(() -> {
                    if (user != null) {
                        // SAUVEGARDE DE LA SESSION
                        AuthManager.saveSession(LoginActivity.this, user.getId(), user.getIdRole());
                        Toast.makeText(LoginActivity.this, "Bienvenue " + user.getNom(), Toast.LENGTH_SHORT).show();
                        goToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // Empêche le retour arrière vers le login
    }
}
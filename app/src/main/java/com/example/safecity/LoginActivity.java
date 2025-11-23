package com.example.safecity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Initialisation de Firebase Auth
        auth = FirebaseAuth.getInstance();

        // 2. Vérification auto : Si l'utilisateur est déjà connecté, on file direct à l'accueil
        if (auth.getCurrentUser() != null) {
            goToMain();
            return; // On arrête l'exécution ici
        }

        setContentView(R.layout.activity_login);

        // Liaison UI
        EditText etEmail = findViewById(R.id.et_login_email);
        EditText etPass = findViewById(R.id.et_login_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_go_to_register);

        // Click Connexion
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            // Connexion avec Firebase
            loginUser(email, pass);
        });

        // Redirection vers Inscription
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser(String email, String password) {
        // On affiche un petit message (ou on pourrait mettre une ProgressBar)
        Toast.makeText(this, "Connexion en cours...", Toast.LENGTH_SHORT).show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Succès !
                        FirebaseUser user = auth.getCurrentUser();
                        String nom = (user.getDisplayName() != null) ? user.getDisplayName() : "Utilisateur";

                        Toast.makeText(LoginActivity.this, "Bienvenue " + nom, Toast.LENGTH_SHORT).show();
                        goToMain();
                    } else {
                        // Échec (Mot de passe faux, compte inexistant, pas d'internet...)
                        String error = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                        Toast.makeText(LoginActivity.this, "Erreur : " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        // Ces flags empêchent de revenir au login en faisant "Retour" depuis l'accueil
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
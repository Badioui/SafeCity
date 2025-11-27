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
            return;
        }

        setContentView(R.layout.activity_login);

        // Liaison UI
        EditText etEmail = findViewById(R.id.et_login_email);
        EditText etPass = findViewById(R.id.et_login_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegister = findViewById(R.id.tv_go_to_register);
        TextView tvForgotPass = findViewById(R.id.tv_forgot_password); // <-- AJOUT

        // Click Connexion
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(LoginActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, pass);
        });

        // Redirection vers Inscription
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // --- FONCTIONNALITÉ MOT DE PASSE OUBLIÉ ---
        tvForgotPass.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Entrez votre email ici pour réinitialiser");
                etEmail.requestFocus();
                return;
            }

            // Appel Firebase pour envoyer l'email
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Email de réinitialisation envoyé ! Vérifiez vos spams.", Toast.LENGTH_LONG).show();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                            Toast.makeText(LoginActivity.this, "Erreur : " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void loginUser(String email, String password) {
        Toast.makeText(this, "Connexion en cours...", Toast.LENGTH_SHORT).show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        String nom = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Utilisateur";

                        Toast.makeText(LoginActivity.this, "Bienvenue " + nom, Toast.LENGTH_SHORT).show();
                        goToMain();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Erreur inconnue";
                        Toast.makeText(LoginActivity.this, "Erreur : " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
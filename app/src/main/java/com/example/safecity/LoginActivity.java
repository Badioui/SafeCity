package com.example.safecity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Gère l'authentification des utilisateurs.
 * Optimisé pour le layout Material Design avec gestion des erreurs en temps réel
 * et vérification automatique de la session au démarrage.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister, tvForgot;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Initialisation de Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. VÉRIFICATION AUTO : Si l'utilisateur est déjà connecté, on va direct à l'accueil
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        // 3. Liaison des vues (IDs synchronisés avec le layout Material)
        tilEmail = findViewById(R.id.til_login_email);
        tilPassword = findViewById(R.id.til_login_password);
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_go_to_register);
        tvForgot = findViewById(R.id.tv_forgot_password);

        // 4. Écouteurs de clics
        btnLogin.setOnClickListener(v -> performLogin());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgot.setOnClickListener(v -> handleForgotPassword());
    }

    /**
     * Tente de connecter l'utilisateur avec validation des entrées.
     */
    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Reset des erreurs visuelles
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Validations
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Veuillez entrer un email valide");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Le mot de passe est requis");
            return;
        }

        // Action de connexion
        btnLogin.setEnabled(false);
        Toast.makeText(this, "Connexion en cours...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    String nom = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Utilisateur";

                    Toast.makeText(this, "Bienvenue " + nom, Toast.LENGTH_SHORT).show();
                    goToMain();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Échec : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Gère la récupération du mot de passe via Firebase.
     */
    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Entrez votre email ici pour réinitialiser");
            etEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Lien de réinitialisation envoyé ! Vérifiez vos spams.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Redirige vers l'activité principale en nettoyant la pile d'activités.
     */
    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
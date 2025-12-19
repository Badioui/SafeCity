package com.example.safecity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safecity.model.Utilisateur;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Logique d'inscription optimisée pour le layout Material Design.
 * Gère la validation des champs, la création Auth et la synchronisation Firestore.
 */
public class RegisterActivity extends AppCompatActivity {

    // Conteneurs pour les erreurs visuelles
    private TextInputLayout tilName, tilEmail, tilPassword;
    // Champs de saisie
    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Liaison des vues avec les IDs du XML amélioré
        tilName = findViewById(R.id.til_register_name);
        tilEmail = findViewById(R.id.til_register_email);
        tilPassword = findViewById(R.id.til_register_password);

        etName = findViewById(R.id.et_register_name);
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);

        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        // Listeners
        btnRegister.setOnClickListener(v -> attemptRegistration());
        tvGoToLogin.setOnClickListener(v -> finish()); // Retourne simplement à l'écran précédent (Login)
    }

    /**
     * Valide les champs et lance la procédure Firebase.
     */
    private void attemptRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Réinitialisation des erreurs
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Validation locale
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Le nom est requis");
            isValid = false;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Veuillez entrer un email valide");
            isValid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tilPassword.setError("Le mot de passe doit contenir au moins 6 caractères");
            isValid = false;
        }

        if (!isValid) return;

        // Désactivation du bouton pour éviter les envois multiples
        btnRegister.setEnabled(false);
        Toast.makeText(this, "Création de votre profil citoyen...", Toast.LENGTH_SHORT).show();

        // 1. Création du compte dans Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // 2. Mise à jour du profil (nom d'affichage)
                        updateAuthProfile(user, name);
                    }
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Échec : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateAuthProfile(FirebaseUser user, String name) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            // 3. Une fois le profil Auth mis à jour, on enregistre dans Firestore
            saveUserToFirestore(user, name);
        });
    }

    /**
     * Initialise les données de l'utilisateur dans la base Firestore.
     */
    private void saveUserToFirestore(FirebaseUser user, String name) {
        Utilisateur newUser = new Utilisateur();

        // Correction : Utilisation de setId conformément au modèle Utilisateur (@DocumentId)
        newUser.setId(user.getUid());

        newUser.setNom(name);
        newUser.setEmail(user.getEmail());
        newUser.setIdRole("citoyen"); // Rôle par défaut
        newUser.setScore(0);

        // Correction : Suppression de setGrade car le grade est une méthode calculée
        // getGrade() dans votre modèle basée sur le score.

        newUser.setDateCreation(String.valueOf(System.currentTimeMillis()));

        // Enregistrement dans la collection "utilisateurs"
        db.collection("utilisateurs").document(user.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Bienvenue " + name + " !", Toast.LENGTH_SHORT).show();
                    // Redirection vers l'accueil en vidant la pile d'activités
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Erreur lors de la création du profil : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
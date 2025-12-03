package com.example.safecity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safecity.model.Utilisateur;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPass;
    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Initialisation Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Liaison UI
        etName = findViewById(R.id.et_register_name);
        etEmail = findViewById(R.id.et_register_email);
        etPass = findViewById(R.id.et_register_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_go_to_login);

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String nom = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPass.getText().toString().trim();

        if (TextUtils.isEmpty(nom) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Veuillez tout remplir", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit faire 6 caractères min.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Désactiver le bouton pour éviter les doubles clics
        btnRegister.setEnabled(false);

        // 3. Création dans Firebase Auth
        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        // 4. Mise à jour du profil (Nom) dans Auth
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(nom)
                                .build();

                        if (firebaseUser != null) {
                            firebaseUser.updateProfile(profileUpdates);

                            // 5. Sauvegarder les infos sup. dans Firestore
                            saveUserToFirestore(firebaseUser.getUid(), nom, email);
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String nom, String email) {
        // On crée un objet Utilisateur pour Firestore
        Utilisateur newUser = new Utilisateur();
        newUser.setNom(nom);
        newUser.setEmail(email);
        newUser.setIdRole("citoyen");

        // --- CORRECTION : AJOUT DE LA DATE DE CRÉATION ---
        // On utilise le timestamp système actuel converti en String
        // Cela permet de savoir quand l'utilisateur s'est inscrit
        newUser.setDateCreation(String.valueOf(System.currentTimeMillis()));

        db.collection("users").document(uid)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Compte créé avec succès !", Toast.LENGTH_SHORT).show();
                    // Rediriger vers MainActivity (ou Login)
                    startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, "Compte Auth créé mais erreur sauvegarde Firestore : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
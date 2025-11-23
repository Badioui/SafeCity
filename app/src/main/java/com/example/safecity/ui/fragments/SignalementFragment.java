package com.example.safecity.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.safecity.R;
import com.example.safecity.dao.ReferenceDAO;
import com.example.safecity.model.Categorie;
import com.example.safecity.model.Incident;
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.utils.AppExecutors;
import com.example.safecity.utils.ImageUtils;
import com.example.safecity.utils.LocationHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SignalementFragment extends Fragment implements LocationHelper.LocationListener {

    // Clé pour passer l'argument (ID de l'incident à modifier) - STRING maintenant
    private static final String ARG_INCIDENT_ID = "arg_incident_id";

    // Constantes
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_PICK = 2;
    private static final int PERM_CODE_CAMERA = 100;
    private static final int PERM_CODE_LOCATION = 101;
    private static final int PERM_CODE_GALLERY = 102;

    // UI Components
    private TextInputEditText etDescription;
    private Spinner spinnerType;
    private Button btnSubmit;
    private ImageButton btnCapturePhoto;
    private ImageView imgPhotoPreview;
    private TextView tvGpsLocation;
    private TextView tvHeader;
    private CheckBox cbNoGps;

    // Data & Logic
    // REMPLACEMENT : IncidentDAO supprimé, on utilise FirestoreRepository
    private FirestoreRepository firestoreRepo;
    private ReferenceDAO referenceDAO; // On garde ça pour les catégories (si toujours en local)

    private LocationHelper locationHelper;
    private Location lastKnownLocation;
    private String currentPhotoPath;
    private String finalPhotoPath;

    // Mode Édition variables
    private String editingIncidentId = null; // String pour Firestore
    private Incident incidentToEdit;

    // Méthode statique mise à jour pour accepter un String ID
    public static SignalementFragment newInstance(String incidentId) {
        SignalementFragment fragment = new SignalementFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INCIDENT_ID, incidentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signalement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialisation des vues
        etDescription = view.findViewById(R.id.et_description);
        spinnerType = view.findViewById(R.id.spinner_type_incident);
        btnSubmit = view.findViewById(R.id.btn_submit_incident);
        btnCapturePhoto = view.findViewById(R.id.btn_capture_photo);
        imgPhotoPreview = view.findViewById(R.id.img_photo_preview);
        tvGpsLocation = view.findViewById(R.id.tv_gps_location);
        tvHeader = view.findViewById(R.id.tv_header);
        cbNoGps = view.findViewById(R.id.cb_no_gps);

        // 2. Initialisation des Repositories
        firestoreRepo = new FirestoreRepository();
        referenceDAO = new ReferenceDAO(getContext());

        // 3. Vérifier si on est en mode édition
        if (getArguments() != null) {
            editingIncidentId = getArguments().getString(ARG_INCIDENT_ID, null);
        }

        // 4. Charger les catégories et initialiser l'UI
        loadCategoriesAndInit();

        // 5. Listeners
        btnCapturePhoto.setOnClickListener(v -> showImageSourceDialog());
        btnSubmit.setOnClickListener(v -> saveIncident());

        cbNoGps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (locationHelper != null) locationHelper.stopLocationUpdates();
                lastKnownLocation = null;
                tvGpsLocation.setText("Aucune position précise (Ville/Pays uniquement)");
            } else {
                checkLocationPermissionAndStart();
            }
        });
    }

    private void loadCategoriesAndInit() {
        // Chargement des catégories (toujours en SQLite local pour cet exemple)
        AppExecutors.getInstance().diskIO().execute(() -> {
            referenceDAO.open();
            List<Categorie> cats = referenceDAO.getAllCategories();
            referenceDAO.close();

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (!isAdded() || getActivity() == null) return;

                if (cats != null && !cats.isEmpty()) {
                    ArrayAdapter<Categorie> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, cats);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerType.setAdapter(adapter);
                }

                // Logique d'édition avec Firestore
                if (editingIncidentId != null) {
                    // TODO: Si vous voulez implémenter l'édition complète, il faudrait faire un appel
                    // firestoreRepo.getIncidentById(editingIncidentId, callback...) ici.
                    // Pour l'instant, on se concentre sur la création.
                    tvHeader.setText("Modifier le signalement (Indisponible temporairement)");
                } else {
                    checkLocationPermissionAndStart();
                }
            });
        });
    }

    /**
     * Sauvegarde l'incident dans Firestore.
     */
    private void saveIncident() {
        String desc = etDescription.getText().toString().trim();
        if (desc.isEmpty()) {
            etDescription.setError("Description requise");
            return;
        }

        // Gestion Position
        double lat = 0.0;
        double lon = 0.0;

        if (!cbNoGps.isChecked()) {
            if (lastKnownLocation == null) {
                Toast.makeText(getContext(), "Attente du GPS...", Toast.LENGTH_SHORT).show();
                return;
            }
            lat = lastKnownLocation.getLatitude();
            lon = lastKnownLocation.getLongitude();
        }

        // Création de l'objet Incident
        Incident incident = (incidentToEdit != null) ? incidentToEdit : new Incident();

        incident.setDescription(desc);
        incident.setLatitude(lat);
        incident.setLongitude(lon);
        incident.setPhotoUrl(finalPhotoPath);

        // Gestion Catégorie
        Categorie cat = (Categorie) spinnerType.getSelectedItem();
        if (cat != null) {
            // Conversion ID long -> String pour Firestore
            incident.setIdCategorie(String.valueOf(cat.getId()));
            incident.setNomCategorie(cat.getNomCategorie());
        }

        // Gestion Utilisateur (Firebase Auth)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            incident.setIdUtilisateur(currentUser.getUid()); // ID String unique Firebase
            incident.setNomUtilisateur(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Utilisateur");
        } else {
            Toast.makeText(getContext(), "Vous devez être connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dates et Statut (si nouvel incident)
        if (editingIncidentId == null) {
            incident.setStatut(Incident.STATUT_NOUVEAU);
            incident.setDateSignalement(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
        }

        // --- ENVOI VERS FIREBASE ---
        btnSubmit.setEnabled(false); // Eviter double clic

        // Utilisation du repo Firestore comme demandé
        firestoreRepo.addIncident(incident, new FirestoreRepository.OnFirestoreTaskComplete() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Incident signalé et synchronisé !", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack(); // Fermer le fragment
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // =========================================================
    // GESTION DU GPS (inchangé)
    // =========================================================

    private void checkLocationPermissionAndStart() {
        if (cbNoGps.isChecked()) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_CODE_LOCATION);
        } else {
            startGps();
        }
    }

    private void startGps() {
        tvGpsLocation.setText("Recherche position...");
        locationHelper = new LocationHelper(getContext());
        locationHelper.startLocationUpdates(this);
    }

    @Override
    public void onLocationReceived(Location location) {
        if (cbNoGps.isChecked()) return;
        this.lastKnownLocation = location;
        if (getContext() != null) {
            tvGpsLocation.setText(String.format(Locale.getDefault(), "📍 GPS : %.4f, %.4f", location.getLatitude(), location.getLongitude()));
            btnSubmit.setEnabled(true);
        }
    }

    @Override
    public void onLocationError(String message) {
        if (getContext() != null && !cbNoGps.isChecked()) {
            tvGpsLocation.setText("⚠️ " + message);
        }
    }

    // =========================================================
    // GESTION DE LA PHOTO (inchangé)
    // =========================================================

    private void showImageSourceDialog() {
        String[] options = {"Prendre une photo", "Choisir dans la galerie"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Ajouter une photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermission();
                    else checkGalleryPermission();
                }).show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERM_CODE_CAMERA);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void checkGalleryPermission() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (Build.VERSION.SDK_INT >= 33) permission = "android.permission.READ_MEDIA_IMAGES";

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, PERM_CODE_GALLERY);
        } else {
            dispatchGalleryIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERM_CODE_CAMERA) dispatchTakePictureIntent();
            if (requestCode == PERM_CODE_GALLERY) dispatchGalleryIntent();
            if (requestCode == PERM_CODE_LOCATION) startGps();
        } else {
            Toast.makeText(getContext(), "Permission refusée", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(), "com.example.safecity.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void dispatchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_PICK);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            imgPhotoPreview.setVisibility(View.VISIBLE);
            try {
                File processedFile = null;
                if (requestCode == REQUEST_IMAGE_CAPTURE) {
                    File rawFile = new File(currentPhotoPath);
                    processedFile = ImageUtils.compressImage(rawFile);
                } else if (requestCode == REQUEST_GALLERY_PICK && data != null) {
                    Uri selectedImageUri = data.getData();
                    processedFile = ImageUtils.compressUri(getContext(), selectedImageUri);
                }

                if (processedFile != null) {
                    finalPhotoPath = processedFile.getAbsolutePath();
                    imgPhotoPreview.setImageTintList(null);
                    imgPhotoPreview.setPadding(0, 0, 0, 0);
                    Glide.with(this).load(finalPhotoPath).centerCrop().into(imgPhotoPreview);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Erreur image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopLocationUpdates();
        }
    }
}
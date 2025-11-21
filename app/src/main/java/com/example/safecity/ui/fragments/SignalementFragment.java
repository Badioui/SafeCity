package com.example.safecity.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.example.safecity.dao.IncidentDAO;
import com.example.safecity.dao.ReferenceDAO;
import com.example.safecity.model.Categorie;
import com.example.safecity.model.Incident;
import com.example.safecity.utils.AuthManager;
import com.example.safecity.utils.ImageUtils;
import com.example.safecity.utils.LocationHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SignalementFragment extends Fragment implements LocationHelper.LocationListener {

    // Clé pour passer l'argument
    private static final String ARG_INCIDENT_ID = "arg_incident_id";

    // Constantes
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_PICK = 2;
    private static final int PERM_CODE_CAMERA = 100;
    private static final int PERM_CODE_LOCATION = 101;
    private static final int PERM_CODE_GALLERY = 102;

    // UI
    private TextInputEditText etDescription;
    private Spinner spinnerType;
    private Button btnSubmit;
    private ImageButton btnCapturePhoto;
    private ImageView imgPhotoPreview;
    private TextView tvGpsLocation;
    private TextView tvHeader; // Pour changer le titre

    // Data
    private IncidentDAO incidentDAO;
    private ReferenceDAO referenceDAO;
    private LocationHelper locationHelper;
    private Location lastKnownLocation;
    private String currentPhotoPath;
    private String finalPhotoPath;

    // Mode Édition
    private long editingIncidentId = -1; // -1 signifie mode création
    private Incident incidentToEdit;

    // Méthode statique pour créer le fragment avec un ID (facilite l'appel)
    public static SignalementFragment newInstance(long incidentId) {
        SignalementFragment fragment = new SignalementFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_INCIDENT_ID, incidentId);
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

        // Init UI
        etDescription = view.findViewById(R.id.et_description);
        spinnerType = view.findViewById(R.id.spinner_type_incident);
        btnSubmit = view.findViewById(R.id.btn_submit_incident);
        btnCapturePhoto = view.findViewById(R.id.btn_capture_photo);
        imgPhotoPreview = view.findViewById(R.id.img_photo_preview);
        tvGpsLocation = view.findViewById(R.id.tv_gps_location);
        tvHeader = view.findViewById(R.id.tv_header);

        incidentDAO = new IncidentDAO(getContext());
        referenceDAO = new ReferenceDAO(getContext());

        // Vérifier si on est en mode édition (argument passé ?)
        if (getArguments() != null) {
            editingIncidentId = getArguments().getLong(ARG_INCIDENT_ID, -1);
        }

        // Charger les catégories puis (si édition) pré-remplir
        loadCategoriesAndInit();

        btnCapturePhoto.setOnClickListener(v -> showImageSourceDialog());
        btnSubmit.setOnClickListener(v -> saveIncident()); // Méthode unifiée save/update
    }

    private void loadCategoriesAndInit() {
        new Thread(() -> {
            referenceDAO.open();
            List<Categorie> cats = referenceDAO.getAllCategories();
            referenceDAO.close();

            // Si mode édition, charger l'incident
            if (editingIncidentId != -1) {
                incidentDAO.open();
                incidentToEdit = incidentDAO.getIncidentById(editingIncidentId); // Supposons que cette méthode existe ou getAll et filtrer
                incidentDAO.close();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // 1. Configurer le Spinner
                    if (cats != null && !cats.isEmpty()) {
                        ArrayAdapter<Categorie> adapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item, cats);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerType.setAdapter(adapter);
                    }

                    // 2. Si Édition : Pré-remplir l'interface
                    if (incidentToEdit != null) {
                        setupEditMode(cats);
                    } else {
                        // Sinon mode création : Lancer GPS
                        checkLocationPermissionAndStart();
                    }
                });
            }
        }).start();
    }

    private void setupEditMode(List<Categorie> categories) {
        // Changer les titres
        tvHeader.setText("Modifier le signalement");
        btnSubmit.setText("Mettre à jour");

        // Remplir description
        etDescription.setText(incidentToEdit.getDescription());

        // Sélectionner la bonne catégorie dans le spinner
        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == incidentToEdit.getIdCategorie()) {
                    spinnerType.setSelection(i);
                    break;
                }
            }
        }

        // Afficher la photo existante
        if (incidentToEdit.getPhotoUrl() != null) {
            finalPhotoPath = incidentToEdit.getPhotoUrl();
            imgPhotoPreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(finalPhotoPath).centerCrop().into(imgPhotoPreview);
        }

        // Afficher la position existante (et la garder)
        lastKnownLocation = new Location("saved");
        lastKnownLocation.setLatitude(incidentToEdit.getLatitude());
        lastKnownLocation.setLongitude(incidentToEdit.getLongitude());
        tvGpsLocation.setText("📍 Position enregistrée : " + incidentToEdit.getLatitude() + ", " + incidentToEdit.getLongitude());

        btnSubmit.setEnabled(true);
    }

    private void saveIncident() {
        String desc = etDescription.getText().toString().trim();
        if (desc.isEmpty()) { etDescription.setError("Requis"); return; }

        // Si on crée, on a besoin du GPS. Si on édite, on a déjà lastKnownLocation (restauré)
        if (lastKnownLocation == null) {
            Toast.makeText(getContext(), "Position GPS manquante", Toast.LENGTH_SHORT).show();
            return;
        }

        Incident incident = (incidentToEdit != null) ? incidentToEdit : new Incident();
        incident.setDescription(desc);
        incident.setLatitude(lastKnownLocation.getLatitude());
        incident.setLongitude(lastKnownLocation.getLongitude());
        incident.setPhotoUrl(finalPhotoPath);

        Categorie cat = (Categorie) spinnerType.getSelectedItem();
        if (cat != null) incident.setIdCategorie(cat.getId());

        if (incidentToEdit == null) {
            incident.setStatut("Nouveau");
            incident.setIdUtilisateur(AuthManager.getCurrentUserId(getContext()));
        }

        new Thread(() -> {
            incidentDAO.open();
            long res;
            if (editingIncidentId != -1) {
                res = incidentDAO.updateIncident(incident); // UPDATE (C5)
            } else {
                res = incidentDAO.insertIncident(incident); // INSERT (C2)
            }
            incidentDAO.close();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (res > 0) {
                        Toast.makeText(getContext(), editingIncidentId != -1 ? "Modifié avec succès !" : "Envoyé !", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Erreur...", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    // ... (Le reste du code : GPS, Photo, Permissions reste identique à la version précédente) ...

    // --- GPS ---
    private void checkLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_CODE_LOCATION);
        } else {
            startGps();
        }
    }

    private void startGps() {
        if (editingIncidentId != -1) return; // Pas de GPS auto en mode édition (on garde l'ancien sauf si forcé)
        tvGpsLocation.setText("Recherche position...");
        locationHelper = new LocationHelper(getContext());
        locationHelper.startLocationUpdates(this);
    }

    // --- PHOTO (Reste inchangé, voir code précédent) ---
    private void showImageSourceDialog() { /* ... Code Photo ... */
        String[] options = {"Prendre une photo", "Choisir dans la galerie"};
        new AlertDialog.Builder(getContext())
                .setTitle("Ajouter une photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        checkGalleryPermission();
                    }
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
        if (Build.VERSION.SDK_INT >= 33) {
            permission = "android.permission.READ_MEDIA_IMAGES";
        }
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
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) { }
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
                    Glide.with(this).load(finalPhotoPath).centerCrop().into(imgPhotoPreview);
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @Override
    public void onLocationReceived(Location location) {
        lastKnownLocation = location;
        tvGpsLocation.setText("📍 Position : " + location.getLatitude() + ", " + location.getLongitude());
        btnSubmit.setEnabled(true);
    }

    @Override
    public void onLocationError(String message) {
        tvGpsLocation.setText("⚠️ " + message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) locationHelper.stopLocationUpdates();
    }
}
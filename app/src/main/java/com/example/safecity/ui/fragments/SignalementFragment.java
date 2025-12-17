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
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

// Remplacement de AlertDialog par MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import com.bumptech.glide.Glide;
import com.example.safecity.R;
import com.example.safecity.model.Categorie;
import com.example.safecity.model.Incident;
import com.example.safecity.utils.AppExecutors;
import com.example.safecity.utils.FirestoreRepository;
import com.example.safecity.utils.ImageUtils;
import com.example.safecity.utils.LocationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SignalementFragment extends Fragment implements LocationHelper.LocationListener {

    private static final String ARG_INCIDENT_ID = "arg_incident_id";

    // Constantes
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_PICK = 2;
    private static final int PERM_CODE_CAMERA = 100;
    private static final int PERM_CODE_LOCATION = 101;
    private static final int PERM_CODE_GALLERY = 102;

    private TextInputEditText etDescription;
    private Spinner spinnerType;
    private Button btnSubmit;
    private ImageButton btnCapturePhoto;
    private ImageView imgPhotoPreview;
    private TextView tvGpsLocation, tvHeader;
    private CheckBox cbNoGps;

    private FirestoreRepository firestoreRepo;
    private LocationHelper locationHelper;
    private Location lastKnownLocation;
    private String currentPhotoPath;
    private String finalPhotoPath;

    // Variables pour le mode ÉDITION
    private String editingIncidentId = null;
    private Incident incidentToEdit;

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

        etDescription = view.findViewById(R.id.et_description);
        spinnerType = view.findViewById(R.id.spinner_type_incident);
        btnSubmit = view.findViewById(R.id.btn_submit_incident);
        btnCapturePhoto = view.findViewById(R.id.btn_capture_photo);
        imgPhotoPreview = view.findViewById(R.id.img_photo_preview);
        tvGpsLocation = view.findViewById(R.id.tv_gps_location);
        tvHeader = view.findViewById(R.id.tv_header);
        cbNoGps = view.findViewById(R.id.cb_no_gps);

        firestoreRepo = new FirestoreRepository();

        if (getArguments() != null) {
            editingIncidentId = getArguments().getString(ARG_INCIDENT_ID, null);
        }

        loadCategoriesAndInit();

        btnCapturePhoto.setOnClickListener(v -> showImageSourceDialog());
        btnSubmit.setOnClickListener(v -> prepareAndSaveIncident());

        cbNoGps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (locationHelper != null) locationHelper.stopLocationUpdates();
                if (incidentToEdit == null) lastKnownLocation = null;
                tvGpsLocation.setText("Position manuelle ou conservée");
            } else {
                checkLocationPermissionAndStart();
            }
        });
    }

    // --- 1. CHARGEMENT ET INIT ---

    private void loadCategoriesAndInit() {
        firestoreRepo.getCategories(new FirestoreRepository.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(List<Categorie> cats) {
                if (!isAdded() || getActivity() == null) return;

                if (cats != null && !cats.isEmpty()) {
                    ArrayAdapter<Categorie> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, cats);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerType.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "Aucune catégorie trouvée", Toast.LENGTH_SHORT).show();
                }

                if (editingIncidentId != null) {
                    tvHeader.setText("Modifier le signalement");
                    btnSubmit.setText("Mettre à jour");
                    loadIncidentData(editingIncidentId);
                } else {
                    checkLocationPermissionAndStart();
                }
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    // --- 2. CHARGEMENT DES DONNÉES (MODE ÉDITION) ---

    private void loadIncidentData(String id) {
        firestoreRepo.getIncident(id, new FirestoreRepository.OnIncidentLoadedListener() {
            @Override
            public void onIncidentLoaded(Incident incident) {
                if (!isAdded()) return;

                incidentToEdit = incident;
                etDescription.setText(incident.getDescription());

                if (incident.getNomCategorie() != null && spinnerType.getAdapter() != null) {
                    for (int i = 0; i < spinnerType.getAdapter().getCount(); i++) {
                        Categorie cat = (Categorie) spinnerType.getAdapter().getItem(i);
                        if (cat.getNomCategorie().equals(incident.getNomCategorie())) {
                            spinnerType.setSelection(i);
                            break;
                        }
                    }
                }

                if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
                    imgPhotoPreview.setVisibility(View.VISIBLE);
                    Glide.with(requireContext()).load(incident.getPhotoUrl()).into(imgPhotoPreview);
                }

                cbNoGps.setChecked(true);
                tvGpsLocation.setText("Position originale conservée");

                lastKnownLocation = new Location("Firestore");
                lastKnownLocation.setLatitude(incident.getLatitude());
                lastKnownLocation.setLongitude(incident.getLongitude());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Erreur chargement : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- 3. SAUVEGARDE ---

    private void prepareAndSaveIncident() {
        String desc = etDescription.getText().toString().trim();
        if (desc.isEmpty()) { etDescription.setError("Requis"); return; }

        Incident incident = (incidentToEdit != null) ? incidentToEdit : new Incident();
        incident.setDescription(desc);

        if (!cbNoGps.isChecked() && lastKnownLocation != null) {
            incident.setLatitude(lastKnownLocation.getLatitude());
            incident.setLongitude(lastKnownLocation.getLongitude());
        }

        Categorie cat = (Categorie) spinnerType.getSelectedItem();
        if (cat != null) {
            incident.setIdCategorie(cat.getId());
            incident.setNomCategorie(cat.getNomCategorie());
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (editingIncidentId == null) {
                incident.setIdUtilisateur(currentUser.getUid());
                incident.setNomUtilisateur(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Anonyme");
            }
        } else {
            Toast.makeText(getContext(), "Connectez-vous", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingIncidentId == null) {
            incident.setStatut(Incident.STATUT_NOUVEAU);
            incident.setDateSignalement(new Date());
        }

        btnSubmit.setEnabled(false);
        Toast.makeText(getContext(), "Envoi en cours...", Toast.LENGTH_SHORT).show();

        if (finalPhotoPath != null) {
            uploadImageAndSave(incident);
        } else {
            if (editingIncidentId == null) incident.setPhotoUrl("");
            saveToFirestore(incident);
        }
    }

    private void uploadImageAndSave(Incident incident) {
        if (getContext() == null) return;
        Uri fileUri = Uri.fromFile(new File(finalPhotoPath));
        String fileName = "incident_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("incident_images/" + fileName);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        incident.setPhotoUrl(uri.toString());
                        saveToFirestore(incident);
                        deleteTempFile(finalPhotoPath);
                    });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), "Erreur upload image", Toast.LENGTH_SHORT).show();
                        deleteTempFile(finalPhotoPath);
                    }
                });
    }

    private void deleteTempFile(String path) {
        if (path == null) return;
        try {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveToFirestore(Incident incident) {
        if (editingIncidentId != null) {
            firestoreRepo.updateIncidentDetails(incident, new FirestoreRepository.OnFirestoreTaskComplete() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Modification enregistrée !", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                }
                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), "Erreur maj: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            firestoreRepo.addIncident(incident, new FirestoreRepository.OnFirestoreTaskComplete() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) firestoreRepo.incrementUserScore(user.getUid(), 10);

                        Toast.makeText(getContext(), "Envoyé ! (+10 pts)", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                }
                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // --- LOGIQUE PHOTO ---

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Toast.makeText(getContext(), "Traitement de l'image...", Toast.LENGTH_SHORT).show();
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    File processedFile = null;
                    if (requestCode == REQUEST_IMAGE_CAPTURE) {
                        File rawFile = new File(currentPhotoPath);
                        processedFile = ImageUtils.compressImage(rawFile);
                        if (rawFile.exists()) {
                            rawFile.delete();
                        }
                    } else if (requestCode == REQUEST_GALLERY_PICK && data != null) {
                        Uri selectedImageUri = data.getData();
                        processedFile = ImageUtils.compressUri(getContext(), selectedImageUri);
                    }
                    if (processedFile != null) {
                        finalPhotoPath = processedFile.getAbsolutePath();
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            if (isAdded()) {
                                imgPhotoPreview.setVisibility(View.VISIBLE);
                                Glide.with(this).load(finalPhotoPath).centerCrop().into(imgPhotoPreview);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    // --- GPS & Permissions ---

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
        if (getContext() != null && !cbNoGps.isChecked()) tvGpsLocation.setText("⚠️ " + message);
    }

    // CORRECTION : Utilisation de MaterialAlertDialogBuilder avec bouton Annuler
    private void showImageSourceDialog() {
        String[] options = {"Prendre une photo", "Choisir dans la galerie"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ajouter une photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermission();
                    else checkGalleryPermission();
                })
                .setNegativeButton("Annuler", null) // Ajout du bouton pour fermer
                .show();
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
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try { photoFile = createImageFile(); } catch (IOException ex) { ex.printStackTrace(); }
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
    public void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) locationHelper.stopLocationUpdates();
    }
}
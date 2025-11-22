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
import com.example.safecity.dao.IncidentDAO;
import com.example.safecity.dao.ReferenceDAO;
import com.example.safecity.model.Categorie;
import com.example.safecity.model.Incident;
import com.example.safecity.utils.AppExecutors;
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

    // Clé pour passer l'argument (ID de l'incident à modifier)
    private static final String ARG_INCIDENT_ID = "arg_incident_id";

    // Constantes pour les permissions et intents
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
    private IncidentDAO incidentDAO;
    private ReferenceDAO referenceDAO;
    private LocationHelper locationHelper;
    private Location lastKnownLocation;
    private String currentPhotoPath; // Chemin temporaire (caméra)
    private String finalPhotoPath;   // Chemin final à sauvegarder en BDD

    // Mode Édition variables
    private long editingIncidentId = -1; // -1 signifie "Mode Création"
    private Incident incidentToEdit;

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

        // 1. Initialisation des vues
        etDescription = view.findViewById(R.id.et_description);
        spinnerType = view.findViewById(R.id.spinner_type_incident);
        btnSubmit = view.findViewById(R.id.btn_submit_incident);
        btnCapturePhoto = view.findViewById(R.id.btn_capture_photo);
        imgPhotoPreview = view.findViewById(R.id.img_photo_preview);
        tvGpsLocation = view.findViewById(R.id.tv_gps_location);
        tvHeader = view.findViewById(R.id.tv_header);
        cbNoGps = view.findViewById(R.id.cb_no_gps);

        // 2. Initialisation des DAO
        incidentDAO = new IncidentDAO(getContext());
        referenceDAO = new ReferenceDAO(getContext());

        // 3. Vérifier si on est en mode édition
        if (getArguments() != null) {
            editingIncidentId = getArguments().getLong(ARG_INCIDENT_ID, -1);
        }

        // 4. Charger les catégories et initialiser l'UI
        loadCategoriesAndInit();

        // 5. Listeners
        btnCapturePhoto.setOnClickListener(v -> showImageSourceDialog());
        btnSubmit.setOnClickListener(v -> saveIncident());

        // Listener pour la CheckBox GPS
        cbNoGps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // L'utilisateur ne veut pas de GPS
                if (locationHelper != null) locationHelper.stopLocationUpdates();
                lastKnownLocation = null; // On vide la position
                tvGpsLocation.setText("Aucune position précise (Ville/Pays uniquement)");
            } else {
                // L'utilisateur réactive le GPS
                checkLocationPermissionAndStart();
            }
        });
    }

    private void loadCategoriesAndInit() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            referenceDAO.open();
            List<Categorie> cats = referenceDAO.getAllCategories();
            referenceDAO.close();

            if (editingIncidentId != -1) {
                incidentDAO.open();
                incidentToEdit = incidentDAO.getIncidentById(editingIncidentId);
                incidentDAO.close();
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isAdded() && getActivity() != null) {
                    if (cats != null && !cats.isEmpty()) {
                        ArrayAdapter<Categorie> adapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item, cats);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerType.setAdapter(adapter);
                    }

                    if (incidentToEdit != null) {
                        setupEditMode(cats);
                    } else {
                        checkLocationPermissionAndStart();
                    }
                }
            });
        });
    }

    private void setupEditMode(List<Categorie> categories) {
        tvHeader.setText("Modifier le signalement");
        btnSubmit.setText("Mettre à jour");
        etDescription.setText(incidentToEdit.getDescription());

        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == incidentToEdit.getIdCategorie()) {
                    spinnerType.setSelection(i);
                    break;
                }
            }
        }

        if (incidentToEdit.getPhotoUrl() != null) {
            finalPhotoPath = incidentToEdit.getPhotoUrl();
            imgPhotoPreview.setVisibility(View.VISIBLE);

            // CORRECTION TINT : On enlève le filtre gris
            imgPhotoPreview.setImageTintList(null);

            imgPhotoPreview.setPadding(0, 0, 0, 0);
            Glide.with(this).load(finalPhotoPath).centerCrop().into(imgPhotoPreview);
        }

        // Gestion GPS en mode édition
        if (incidentToEdit.getLatitude() == 0 && incidentToEdit.getLongitude() == 0) {
            // C'était un incident sans GPS
            cbNoGps.setChecked(true);
            tvGpsLocation.setText("Aucune position précise (Ville/Pays uniquement)");
        } else {
            lastKnownLocation = new Location("saved");
            lastKnownLocation.setLatitude(incidentToEdit.getLatitude());
            lastKnownLocation.setLongitude(incidentToEdit.getLongitude());
            tvGpsLocation.setText("📍 Position enregistrée : " + incidentToEdit.getLatitude() + ", " + incidentToEdit.getLongitude());
        }

        btnSubmit.setEnabled(true);
    }

    /**
     * Sauvegarde (Insert) ou Met à jour (Update) l'incident.
     */
    private void saveIncident() {
        String desc = etDescription.getText().toString().trim();
        if (desc.isEmpty()) {
            etDescription.setError("Description requise");
            return;
        }

        // Gestion Position : Vague vs Précise
        double lat = 0.0;
        double lon = 0.0;

        if (!cbNoGps.isChecked()) {
            if (lastKnownLocation == null) {
                Toast.makeText(getContext(), "Attente du GPS... ou cochez 'Pas de position précise'", Toast.LENGTH_SHORT).show();
                return;
            }
            lat = lastKnownLocation.getLatitude();
            lon = lastKnownLocation.getLongitude();
        }

        Incident incident = (incidentToEdit != null) ? incidentToEdit : new Incident();

        incident.setDescription(desc);
        incident.setLatitude(lat);
        incident.setLongitude(lon);
        incident.setPhotoUrl(finalPhotoPath);

        Categorie cat = (Categorie) spinnerType.getSelectedItem();
        if (cat != null) {
            incident.setIdCategorie(cat.getId());
        }

        if (incidentToEdit == null) {
            incident.setStatut("Nouveau");
            incident.setDateSignalement(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));

            // --- VERIFICATION CRUCIALE : On assigne l'ID de l'utilisateur connecté ---
            long userId = AuthManager.getCurrentUserId(getContext());
            incident.setIdUtilisateur(userId);

            // Debug optionnel :
            // System.out.println("Sauvegarde incident pour User ID : " + userId);
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            incidentDAO.open();
            long res;
            if (editingIncidentId != -1) {
                res = incidentDAO.updateIncident(incident);
            } else {
                res = incidentDAO.insertIncident(incident);
            }
            incidentDAO.close();

            AppExecutors.getInstance().mainThread().execute(() -> {
                if (isAdded() && getActivity() != null) {
                    if (res > 0) {
                        Toast.makeText(getContext(), editingIncidentId != -1 ? "Incident modifié !" : "Incident signalé !", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    // =========================================================
    // GESTION DU GPS
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
        if (editingIncidentId != -1 && incidentToEdit != null) {
            if (lastKnownLocation != null) return;
        }

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
    // GESTION DE LA PHOTO
    // =========================================================

    private void showImageSourceDialog() {
        String[] options = {"Prendre une photo", "Choisir dans la galerie"};
        new AlertDialog.Builder(requireContext())
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

                    // CORRECTION TINT : On enlève le filtre gris
                    imgPhotoPreview.setImageTintList(null);

                    imgPhotoPreview.setPadding(0, 0, 0, 0);
                    Glide.with(this).load(finalPhotoPath).centerCrop().into(imgPhotoPreview);
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Erreur lors du traitement de l'image", Toast.LENGTH_SHORT).show();
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
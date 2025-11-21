package com.example.safecity.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionManager {

    // Code de requête unique pour gérer toutes les permissions demandées
    public static final int REQUEST_CODE_ALL = 100;

    // Liste des permissions critiques pour SafeCity
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * Vérifie si l'application a déjà toutes les permissions critiques.
     */
    public static boolean checkAllPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Déclenche la boîte de dialogue de demande de permissions à l'utilisateur.
     */
    public static void requestAllPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE_ALL);
    }
}
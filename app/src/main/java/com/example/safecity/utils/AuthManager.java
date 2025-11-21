package com.example.safecity.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {

    private static final String PREF_NAME = "SafeCitySession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE_ID = "role_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Instance de SharedPreferences
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Sauvegarde la session utilisateur après une connexion réussie.
     */
    public static void saveSession(Context context, long userId, long roleId) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putLong(KEY_USER_ID, userId);
        editor.putLong(KEY_ROLE_ID, roleId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Récupère l'ID de l'utilisateur actuellement connecté.
     * Retourne -1 si aucun utilisateur n'est connecté.
     */
    public static long getCurrentUserId(Context context) {
        return getPreferences(context).getLong(KEY_USER_ID, -1);
    }

    /**
     * Récupère l'ID du rôle de l'utilisateur (1=Admin, 2=Autorité, 3=Citoyen).
     * Retourne -1 si non trouvé.
     */
    public static long getCurrentUserRole(Context context) {
        return getPreferences(context).getLong(KEY_ROLE_ID, -1);
    }

    /**
     * Vérifie si un utilisateur est déjà connecté.
     */
    public static boolean isLoggedIn(Context context) {
        return getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Déconnecte l'utilisateur (efface la session).
     */
    public static void logout(Context context) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.clear();
        editor.apply();
    }
}
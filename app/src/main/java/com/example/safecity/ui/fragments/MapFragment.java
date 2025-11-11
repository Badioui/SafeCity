/**
 * ATTENTION: FICHIER DE TEST / PLACEHOLDER TEMPORAIRE (Tâche B1)
 * ⚠️ Ce fichier NE CONTIENT PAS la logique finale d'intégration de Google Maps.
 * Il sert UNIQUEMENT de conteneur pour résoudre les erreurs de compilation
 * (Cannot find symbol) causées par l'utilisation dans MainActivity.java.
 * * Ce fragment sera remplacé par la logique complète de B1 (Configuration API Key)
 * et B3 (Affichage des marqueurs/incidents).
 */
package com.example.safecity.ui.fragments;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.safecity.R;

// Fragment temporaire pour résoudre l'erreur de compilation
public class MapFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Remplacer par le layout de la carte (B1)
        return inflater.inflate(R.layout.fragment_map, container, false);
    }
}

package com.example.safecity;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.safecity.model.Incident;

/**
 * Bloc A7 : Tests Unitaires
 * Vérifie que la logique du modèle Incident fonctionne correctement.
 */

public class ExampleUnitTest {
    @Test
    public void incident_creation_isCorrect() {
        // 1. Préparer un incident de test
        Incident incident = new Incident();
        incident.setDescription("Lampe cassée");
        incident.setLatitude(34.68);
        incident.setLongitude(-1.90);

        // 2. Vérifier que les getters renvoient bien ce qu'on a mis
        assertEquals("Lampe cassée", incident.getDescription());
        assertEquals(34.68, incident.getLatitude(), 0.001); // Le 0.001 est la marge d'erreur acceptée pour les double
    }

    @Test
    public void status_logic_isCorrect() {
        Incident incident = new Incident();

        // Cas A : Statut "Nouveau" -> Ne doit pas être considéré comme traité
        incident.setStatut(Incident.STATUT_NOUVEAU);
        assertFalse("Un incident Nouveau ne doit pas être Traité", incident.isTraite());

        // Cas B : Statut "Traité" -> Doit être considéré comme traité
        incident.setStatut(Incident.STATUT_TRAITE);
        assertTrue("Un incident Traité doit être reconnu", incident.isTraite());

        // Cas C : Test de robustesse (insensible à la casse)
        // Si par erreur on écrit "traité" en minuscule dans la base, l'app doit quand même comprendre
        incident.setStatut("traité");
        assertTrue("La vérification doit ignorer les majuscules/minuscules", incident.isTraite());
    }
}
/**
 * Version V2 pour Firebase Functions SDK récent
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

// Initialisation de l'application Firebase Admin
// Cette fonction permet d'accéder aux services Firebase tels que Firestore et Messaging.
admin.initializeApp();

/**
 * [Fonction existante]
 * Déclenchée lors de la création d'un nouveau document dans la collection 'incidents'.
 * Envoie une notification push à tous les utilisateurs abonnés au topic 'incidents_all'.
 */
exports.sendNewIncidentNotification = onDocumentCreated("incidents/{incidentId}", async (event) => {

    // 1. Récupération des données du snapshot
    const snapshot = event.data;
    if (!snapshot) {
        console.log("Pas de données associées à l'événement");
        return;
    }

    const incident = snapshot.data();

    // Valeurs par défaut
    const type = incident.nomCategorie || "Incident";
    const desc = incident.description || "Nouvel incident signalé.";

    // Conversion en String pour FCM
    const lat = String(incident.latitude);
    const lng = String(incident.longitude);

    console.log(`Nouvel incident détecté (V2) : ${type}`);

    // 2. Construction du message
    const message = {
        notification: {
            title: `⚠️ Nouveau : ${type}`,
            body: `${desc} - Soyez prudents !`
        },
        data: {
            lat: lat,
            lng: lng,
            incidentId: event.params.incidentId // Accès aux paramètres via event.params
        },
        android: {
            notification: {
                clickAction: "MainActivity",
                channelId: "safecity_alerts_channel"
            }
        },
        topic: "incidents_all"
    };

    // 3. Envoi via Firebase Cloud Messaging (FCM)
    try {
        const response = await admin.messaging().send(message);
        console.log('Notification envoyée avec succès ! ID:', response);
    } catch (error) {
        console.error('Erreur lors de l\'envoi:', error);
    }
});

/**
 * [Nouvelle Fonction - Étape 3]
 * Déclenchée lors de la création d'un nouveau document dans la collection 'official_alerts'.
 * Envoie une Alerte Officielle à tous les utilisateurs abonnés au topic 'official_alerts'.
 */
exports.sendOfficialAlertNotification = onDocumentCreated("official_alerts/{alertId}", async (event) => {
   
    // 1. Récupération des données de l'alerte
    const snapshot = event.data;
    if (!snapshot) {
        console.log("Pas de données d'alerte");
        return;
    }

    const alert = snapshot.data();
    const title = alert.titre || "Alerte Officielle";
    const body = alert.message || "Message important des autorités.";

    console.log(`Nouvelle alerte officielle : ${title}`);

    // 2. Construction du message FCM
    const message = {
        notification: {
            title: `🚨 ${title}`, // Ajout d'une icône d'urgence
            body: body
        },
        android: {
            notification: {
                clickAction: "MainActivity",
                channelId: "safecity_alerts_channel",
                priority: "high" // Priorité haute pour les alertes critiques
            }
        },
        topic: "official_alerts" // Topic spécifique pour les alertes officielles
    };

    // 3. Envoi via Firebase Cloud Messaging (FCM)
    try {
        await admin.messaging().send(message);
        console.log('Alerte envoyée avec succès !');
    } catch (error) {
        console.error('Erreur envoi alerte:', error);
    }
});
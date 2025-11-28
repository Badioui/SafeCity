/**
 * Version V2 pour Firebase Functions SDK récent
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNewIncidentNotification = onDocumentCreated("incidents/{incidentId}", async (event) => {

    // 1. En V2, on récupère le snapshot via event.data
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

    // 3. Envoi
    try {
        const response = await admin.messaging().send(message);
        console.log('Notification envoyée avec succès ! ID:', response);
    } catch (error) {
        console.error('Erreur lors de l\'envoi:', error);
    }
});

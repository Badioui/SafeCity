// On importe les fonctions nécessaires avec la nouvelle syntaxe v2
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

// Initialisation de l'app admin
admin.initializeApp();

// Définition de la fonction avec la syntaxe v2 "onDocumentCreated"
exports.sendPushNotification = onDocumentCreated("notifications/{notificationId}", async (event) => {
  // Récupérer les données du document qui vient d'être créé
  const snapshot = event.data;
  if (!snapshot) {
    console.log("Aucune donnée associée à l'événement, arrêt.");
    return;
  }
  const notificationData = snapshot.data();
  const userId = notificationData.idDestinataire;

  // Vérifier si l'ID du destinataire est présent
  if (!userId) {
    return console.log("idDestinataire manquant, notification ignorée.");
  }

  // Créer le message de la notification
  const payload = {
    notification: {
      title: notificationData.titre,
      body: notificationData.message,
    },
    data: {
      // Ajouter des données supplémentaires pour l'app
      idIncidentSource: notificationData.idIncidentSource || "",
    },
  };

  // Envoyer la notification au topic de l'utilisateur
  try {
    const response = await admin
      .messaging()
      .sendToTopic(`user_${userId}`, payload);
    console.log("Notification envoyée avec succès:", response);
  } catch (error) {
    console.error("Erreur lors de l'envoi de la notification:", error);
  }
});
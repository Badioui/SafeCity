/**
 * ATTENTION: FICHIER DE TEST / ARTEFACT D'INTÉGRATION EN COURS (Tâche C4/C5)
 * ⚠️ Ce code d'adaptateur ne gère pas encore tous les cas d'usage finaux :
 * 1. La logique des boutons Modifier/Supprimer (C5) est vide.
 * 2. La gestion de la couleur de statut est simplifiée pour la compilation.
 * 3. Le chargement d'images n'est pas implémenté.
 * * Ce fichier est destiné à être complété pour l'affichage final des données.
 */
package com.example.safecity.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.R;
import com.example.safecity.model.Incident; // Assurez-vous que l'import est correct
import java.util.List;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    // Retirer 'final' pour permettre la mise à jour via updateList()
    private List<Incident> incidentList;
    private final OnIncidentActionListener listener;

    // Interface de Callback pour gérer toutes les actions sur un incident (C4, C5)
    public interface OnIncidentActionListener {
        void onMapClick(Incident incident);
        void onEditClick(Incident incident);
        void onDeleteClick(Incident incident);
    }

    public IncidentAdapter(List<Incident> incidentList, OnIncidentActionListener listener) {
        this.incidentList = incidentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IncidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                // CORRECTION: Utiliser le layout unique
                .inflate(R.layout.item_signalement, parent, false);
        return new IncidentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidentViewHolder holder, int position) {
        Incident incident = incidentList.get(position);
        Context context = holder.itemView.getContext();

        // 1. Liaison des données
        // Assurez-vous que Incident.java a une méthode getUserName() ou utilisez la logique suivante
        String usernameDisplay = "Citoyen #" + incident.getIdUtilisateur();

        holder.tvUsername.setText(usernameDisplay);
        holder.tvDescription.setText(incident.getDescription());
        holder.tvStatus.setText(incident.getStatut().toUpperCase());

        // 2. Gestion des actions (Map, Edit, Delete)

        holder.btnOpenMap.setOnClickListener(v -> {
            if (listener != null) listener.onMapClick(incident);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(incident);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(incident);
        });

        // 3. Gestion de la couleur de statut (Version compilable)
        updateStatusColorCompilable(holder.tvStatus, incident.getStatut(), context);
    }

    @Override
    public int getItemCount() {
        return incidentList.size();
    }

    public void updateList(List<Incident> newList) {
        // CORRECTION: Assignation de la nouvelle liste
        this.incidentList = newList;
        notifyDataSetChanged();
    }

    // Fonction de statut simplifiée pour la compilation (utilise R.drawable.status_new_bg)
    private void updateStatusColorCompilable(TextView statusView, String statut, Context context) {
        if (statut.equals(Incident.STATUT_NOUVEAU)) {
            // Utilise le drawable NOUVEAU (qui devrait exister)
            statusView.setBackgroundResource(R.drawable.status_new_bg);
            statusView.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            // Utilise un background générique pour éviter les erreurs des drawables manquants
            statusView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSecondary));
            statusView.setTextColor(ContextCompat.getColor(context, R.color.white));
        }
        // NOTE: Les autres statuts (EN COURS, TRAITÉ) doivent être gérés ici lorsque les drawables sont créés.
    }

    // CLASSE VIEWHOLDER CORRIGÉE
    public static class IncidentViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvUsername;
        public final TextView tvCategoryDate;
        public final TextView tvStatus;
        public final ImageView imgIncidentPhoto;
        public final TextView tvDescription;
        public final ImageButton btnOpenMap;
        public final ImageButton btnEdit;
        public final ImageButton btnDelete;

        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            // Liaison des IDs de item_signalement.xml
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCategoryDate = itemView.findViewById(R.id.tv_category_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            imgIncidentPhoto = itemView.findViewById(R.id.img_incident_photo);
            tvDescription = itemView.findViewById(R.id.tv_description);

            // Boutons d'action
            btnOpenMap = itemView.findViewById(R.id.btn_open_map);
            btnEdit = itemView.findViewById(R.id.btn_edit_incident);
            btnDelete = itemView.findViewById(R.id.btn_delete_incident);
        }
    }
}
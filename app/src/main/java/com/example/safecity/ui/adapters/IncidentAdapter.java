package com.example.safecity.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.safecity.R;
import com.example.safecity.model.Incident;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private Context context;
    private List<Incident> incidentList;
    private OnIncidentActionListener actionListener;

    // 1. Interface pour les actions (Clic sur Carte, Modifier, Supprimer)
    public interface OnIncidentActionListener {
        void onMapClick(Incident incident);
        void onEditClick(Incident incident);
        void onDeleteClick(Incident incident);
    }

    // 2. Constructeur complet (Pour MyIncidentsFragment)
    public IncidentAdapter(Context context, List<Incident> incidentList, OnIncidentActionListener listener) {
        this.context = context;
        this.incidentList = incidentList;
        this.actionListener = listener;
    }

    // 3. Constructeur simple (Pour HomeFragment)
    public IncidentAdapter(Context context, List<Incident> incidentList) {
        this(context, incidentList, null);
    }

    @NonNull
    @Override
    public IncidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_signalement, parent, false);
        return new IncidentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidentViewHolder holder, int position) {
        Incident incident = incidentList.get(position);

        // --- AFFICHAGE DES TEXTES ---
        holder.tvDescription.setText(incident.getDescription());
        holder.tvStatus.setText(incident.getStatut());

        // A. Catégorie
        String catName = incident.getNomCategorie();
        if (catName == null || catName.isEmpty()) {
            catName = "Non classé";
        }

        // --- FORMATAGE DE LA DATE ---
        String dateAffichee = "Date inconnue";
        // On suppose ici que incident.getDateSignalement() retourne un objet java.util.Date
        if (incident.getDateSignalement() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            dateAffichee = sdf.format(incident.getDateSignalement());
        }

        // Affichage "Catégorie • Date"
        holder.tvCategory.setText(catName + " • " + dateAffichee);

        // B. Nom de l'utilisateur
        // On utilise getNomUtilisateur() ou getUserName() selon votre modèle.
        // Ici je garde la logique précédente mais vérifiez le nom du getter dans Incident.java.
        String userName = incident.getNomUtilisateur(); // ou incident.getUserName()
        if (userName != null && !userName.isEmpty()) {
            holder.tvUsername.setText(userName);
        } else {
            holder.tvUsername.setText("Citoyen #" + incident.getIdUtilisateur());
        }

        // --- GESTION IMAGE (Glide) ---
        if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
            holder.imgPhoto.setVisibility(View.VISIBLE);

            // IMPORTANT : On retire le filtre couleur (tint) pour voir la vraie photo
            holder.imgPhoto.setImageTintList(null);
            holder.imgPhoto.setPadding(0, 0, 0, 0);

            Glide.with(context)
                    .load(incident.getPhotoUrl())
                    .transform(new CenterCrop(), new RoundedCorners(16))
                    .placeholder(R.drawable.ic_incident_placeholder)
                    .into(holder.imgPhoto);
        } else {
            // Image par défaut si pas de photo
            holder.imgPhoto.setImageResource(R.drawable.ic_incident_placeholder);
        }

        // --- GESTION DES BOUTONS (Visibilité selon le contexte) ---
        if (actionListener != null) {
            // Mode "Mes Incidents" : Boutons visibles
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnMap.setOnClickListener(v -> actionListener.onMapClick(incident));
            holder.btnEdit.setOnClickListener(v -> actionListener.onEditClick(incident));
            holder.btnDelete.setOnClickListener(v -> actionListener.onDeleteClick(incident));
        } else {
            // Mode "Fil d'actu global" : Boutons cachés
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);

            // Optionnel : On peut quand même laisser le bouton MAP actif
            holder.btnMap.setOnClickListener(v -> {
                // Action simple ou vide, ou ouvrir la map globale
            });
        }
    }

    @Override
    public int getItemCount() {
        return incidentList != null ? incidentList.size() : 0;
    }

    public void updateData(List<Incident> newList) {
        this.incidentList = newList;
        notifyDataSetChanged();
    }

    public static class IncidentViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvCategory, tvStatus, tvUsername;
        ImageView imgPhoto;
        ImageButton btnMap, btnEdit, btnDelete;

        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvCategory = itemView.findViewById(R.id.tv_category_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvUsername = itemView.findViewById(R.id.tv_username);
            imgPhoto = itemView.findViewById(R.id.img_incident_photo);

            btnMap = itemView.findViewById(R.id.btn_open_map);
            btnEdit = itemView.findViewById(R.id.btn_edit_incident);
            btnDelete = itemView.findViewById(R.id.btn_delete_incident);
        }
    }
}
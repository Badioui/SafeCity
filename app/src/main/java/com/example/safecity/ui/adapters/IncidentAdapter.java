package com.example.safecity.ui.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.safecity.MainActivity;
import com.example.safecity.R;
import com.example.safecity.model.Incident;

import java.util.List;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private Context context;
    private List<Incident> incidentList;
    private OnIncidentActionListener actionListener;

    // Champs pour la gestion des droits
    private String currentUserId;
    private String currentUserRole; // "admin", "autorite", "citoyen"

    public interface OnIncidentActionListener {
        void onMapClick(Incident incident);
        void onEditClick(Incident incident);
        void onDeleteClick(Incident incident);
        void onValidateClick(Incident incident); // NOUVEAU : Action de validation
    }

    public IncidentAdapter(Context context, List<Incident> incidentList, OnIncidentActionListener listener) {
        this.context = context;
        this.incidentList = incidentList;
        this.actionListener = listener;
    }

    public IncidentAdapter(Context context, List<Incident> incidentList) {
        this(context, incidentList, null);
    }

    // Méthode pour définir l'utilisateur actuel et ses droits
    public void setCurrentUser(String userId, String role) {
        this.currentUserId = userId;
        this.currentUserRole = role;
        notifyDataSetChanged(); // Rafraîchir l'affichage des boutons
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

        // --- 1. AFFICHAGE DES TEXTES ET IMAGES ---
        holder.tvDescription.setText(incident.getDescription());
        holder.tvStatus.setText(incident.getStatut());

        String catName = incident.getNomCategorie();
        if (catName == null || catName.isEmpty()) catName = "Non classé";

        // Gestion de la date relative (ex: "Il y a 5 min")
        String dateAffichee = "Date inconnue";
        if (incident.getDateSignalement() != null) {
            long now = System.currentTimeMillis();
            long time = incident.getDateSignalement().getTime();
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    time, now, DateUtils.MINUTE_IN_MILLIS);
            dateAffichee = relativeTime.toString();
        }
        holder.tvCategory.setText(catName + " • " + dateAffichee);

        // Nom utilisateur
        String userName = incident.getNomUtilisateur();
        holder.tvUsername.setText((userName != null && !userName.isEmpty()) ? userName : "Citoyen");

        // Chargement Image avec Glide
        if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            holder.imgPhoto.setImageTintList(null);
            holder.imgPhoto.setPadding(0, 0, 0, 0);
            Glide.with(context)
                    .load(incident.getPhotoUrl())
                    .transform(new CenterCrop(), new RoundedCorners(16))
                    .placeholder(R.drawable.ic_incident_placeholder)
                    .into(holder.imgPhoto);
        } else {
            holder.imgPhoto.setImageResource(R.drawable.ic_incident_placeholder);
        }

        // --- 2. LOGIQUE DES BOUTONS SELON LES RÔLES ---

        boolean isOwner = incident.getIdUtilisateur() != null && incident.getIdUtilisateur().equals(currentUserId);
        boolean isAdmin = "admin".equalsIgnoreCase(currentUserRole);
        boolean isAuthority = "autorite".equalsIgnoreCase(currentUserRole);

        // Vérification si l'incident est déjà traité
        boolean isTraite = "Traité".equalsIgnoreCase(incident.getStatut());

        // A. Boutons MODIFIER / SUPPRIMER : Visibles pour Créateur OU Admin
        if (isOwner || isAdmin) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnEdit.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onEditClick(incident);
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDeleteClick(incident);
            });
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        // B. Bouton VALIDER : Visible pour Autorité OU Admin (si pas déjà traité)
        if ((isAuthority || isAdmin) && !isTraite) {
            holder.btnValidate.setVisibility(View.VISIBLE);
            holder.btnValidate.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onValidateClick(incident);
            });
        } else {
            holder.btnValidate.setVisibility(View.GONE);
        }

        // C. Bouton MAP : Visible pour tout le monde
        holder.btnMap.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).navigateToMapAndFocus(incident.getLatitude(), incident.getLongitude());
            } else {
                Toast.makeText(context, "Lat: " + incident.getLatitude() + ", Lon: " + incident.getLongitude(), Toast.LENGTH_SHORT).show();
            }
        });
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
        ImageButton btnMap, btnEdit, btnDelete, btnValidate; // Ajout btnValidate

        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvCategory = itemView.findViewById(R.id.tv_category_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvUsername = itemView.findViewById(R.id.tv_username);
            imgPhoto = itemView.findViewById(R.id.img_incident_photo);

            // Boutons
            btnMap = itemView.findViewById(R.id.btn_open_map);
            btnEdit = itemView.findViewById(R.id.btn_edit_incident);
            btnDelete = itemView.findViewById(R.id.btn_delete_incident);
            btnValidate = itemView.findViewById(R.id.btn_validate_incident); // Liaison
        }
    }
}
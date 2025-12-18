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
import androidx.core.content.ContextCompat; // Pour les couleurs
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.google.firebase.firestore.FieldValue; // Pour increment/arrayUnion
import com.google.firebase.firestore.FirebaseFirestore; // Pour la database

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
        void onValidateClick(Incident incident);
        void onImageClick(Incident incident);
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
        notifyDataSetChanged(); // Rafraîchir l'affichage
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

        // --- 1. INFO UTILISATEUR & AVATAR ---
        String userName = incident.getNomUtilisateur();
        holder.tvUsername.setText((userName != null && !userName.isEmpty()) ? userName : "Citoyen");

        // CHARGEMENT AVATAR AUTEUR (Nouveau V2.5)
        if (incident.getAuteurPhotoUrl() != null && !incident.getAuteurPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(incident.getAuteurPhotoUrl())
                    .circleCrop() // Arrondir l'avatar
                    .placeholder(R.drawable.ic_profile)
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_profile);
        }

        // --- 2. TEMPS RELATIF ---
        String timeAgo = "Date inconnue";
        if (incident.getDateSignalement() != null) {
            long now = System.currentTimeMillis();
            long time = incident.getDateSignalement().getTime();
            timeAgo = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString();
        }

        String categorie = (incident.getNomCategorie() != null) ? incident.getNomCategorie() : "Incident";
        holder.tvCategoryDate.setText(categorie + " • " + timeAgo);

        // --- 3. CONTENU ---
        holder.tvDescription.setText(incident.getDescription());
        holder.tvStatus.setText(incident.getStatut());

        // --- 4. MEDIA ADAPTATIF ---
        if (incident.hasMedia()) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(incident.getPhotoUrl()) // Priorité photo (la vidéo aura son propre player plus tard)
                    .fitCenter()
                    .placeholder(R.drawable.ic_incident_placeholder)
                    .into(holder.imgPhoto);

            holder.imgPhoto.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onImageClick(incident);
            });
        } else {
            holder.imgPhoto.setVisibility(View.GONE);
            holder.imgPhoto.setOnClickListener(null);
        }

        // --- 5. LOGIQUE DES BOUTONS (Droits) ---
        boolean isOwner = (incident.getIdUtilisateur() != null && incident.getIdUtilisateur().equals(currentUserId));
        boolean isAdmin = "admin".equalsIgnoreCase(currentUserRole);
        boolean isAuthority = "autorite".equalsIgnoreCase(currentUserRole);
        boolean isTraite = "Traité".equalsIgnoreCase(incident.getStatut());

        // Visibilité Edit/Delete
        if (isOwner || isAdmin) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> { if (actionListener != null) actionListener.onEditClick(incident); });
            holder.btnDelete.setOnClickListener(v -> { if (actionListener != null) actionListener.onDeleteClick(incident); });
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Visibilité Valider
        if ((isAuthority || isAdmin) && !isTraite) {
            holder.btnValidate.setVisibility(View.VISIBLE);
            holder.btnValidate.setOnClickListener(v -> { if (actionListener != null) actionListener.onValidateClick(incident); });
        } else {
            holder.btnValidate.setVisibility(View.GONE);
        }

        // Map
        holder.btnMap.setOnClickListener(v -> { if (actionListener != null) actionListener.onMapClick(incident); });

        // --- 6. GESTION DES LIKES (Nouveau V2.5) ---

        // Initialisation de la couleur du bouton
        if (incident.isLikedBy(currentUserId)) {
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light));
        } else {
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
        }

        // Action Click Like
        holder.btnLike.setOnClickListener(v -> toggleLike(incident, holder));
    }

    /**
     * Gère l'ajout ou le retrait d'un like dans Firestore et met à jour l'UI locale.
     */
    private void toggleLike(Incident incident, IncidentViewHolder holder) {
        if (currentUserId == null) {
            Toast.makeText(context, "Vous devez être connecté pour aimer.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        boolean isLiked = incident.isLikedBy(currentUserId);

        if (isLiked) {
            // RETIRER LE LIKE
            // 1. Optimistic UI (Mise à jour visuelle immédiate)
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));

            // 2. Mise à jour Firestore
            db.collection("incidents").document(incident.getId())
                    .update("likesCount", FieldValue.increment(-1),
                            "likedBy", FieldValue.arrayRemove(currentUserId))
                    .addOnFailureListener(e -> {
                        // Rollback UI en cas d'erreur
                        holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light));
                        Toast.makeText(context, "Erreur connexion", Toast.LENGTH_SHORT).show();
                    });

        } else {
            // AJOUTER LE LIKE
            // 1. Optimistic UI
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light));

            // 2. Mise à jour Firestore
            db.collection("incidents").document(incident.getId())
                    .update("likesCount", FieldValue.increment(1),
                            "likedBy", FieldValue.arrayUnion(currentUserId))
                    .addOnFailureListener(e -> {
                        // Rollback UI
                        holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
                        Toast.makeText(context, "Erreur connexion", Toast.LENGTH_SHORT).show();
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

    // --- VIEWHOLDER ---
    public static class IncidentViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvCategoryDate, tvStatus, tvUsername;
        ImageView imgPhoto, imgProfile;
        ImageButton btnMap, btnEdit, btnDelete, btnValidate, btnLike;

        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCategoryDate = itemView.findViewById(R.id.tv_category_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDescription = itemView.findViewById(R.id.tv_description);
            imgPhoto = itemView.findViewById(R.id.img_incident_photo);

            btnMap = itemView.findViewById(R.id.btn_open_map);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnEdit = itemView.findViewById(R.id.btn_edit_incident);
            btnDelete = itemView.findViewById(R.id.btn_delete_incident);
            btnValidate = itemView.findViewById(R.id.btn_validate_incident);
        }
    }
}
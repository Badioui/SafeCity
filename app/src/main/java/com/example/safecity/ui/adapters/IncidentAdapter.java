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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.safecity.R;
import com.example.safecity.model.Incident;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Adaptateur Premium pour le flux de signalements.
 * Synchronisé avec item_signalement.xml et gère les droits admin/auteur.
 */
public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private final Context context;
    private List<Incident> incidentList;
    private final OnIncidentActionListener actionListener;

    private String currentUserId;
    private String currentUserRole;

    public interface OnIncidentActionListener {
        void onMapClick(Incident incident);
        void onEditClick(Incident incident);
        void onDeleteClick(Incident incident);
        void onValidateClick(Incident incident);
        void onImageClick(Incident incident);
        void onCommentClick(Incident incident);
    }

    public IncidentAdapter(Context context, List<Incident> incidentList, OnIncidentActionListener listener) {
        this.context = context;
        this.incidentList = incidentList;
        this.actionListener = listener;
    }

    public void setCurrentUser(String userId, String role) {
        this.currentUserId = userId;
        this.currentUserRole = role;
        notifyDataSetChanged();
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

        // --- 1. IDENTITÉ AUTEUR ---
        holder.tvUsername.setText(incident.getNomUtilisateur() != null ? incident.getNomUtilisateur() : "Citoyen");

        // Cache intelligent pour l'avatar (évite de charger d'anciennes photos)
        Glide.with(context)
                .load(incident.getAuteurPhotoUrl())
                .placeholder(R.drawable.ic_profile)
                .circleCrop()
                .signature(new ObjectKey(incident.getAuteurPhotoUrl() != null ? incident.getAuteurPhotoUrl() : "default"))
                .into(holder.imgProfile);

        // --- 2. CATEGORIE & TEMPS ---
        String timeAgo = incident.getDateSignalement() != null ?
                DateUtils.getRelativeTimeSpanString(incident.getDateSignalement().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                : "Récemment";

        holder.tvCategoryDate.setText((incident.getNomCategorie() != null ? incident.getNomCategorie() : "Incident") + " • " + timeAgo);

        // --- 3. CONTENU & STATUT ---
        holder.tvDescription.setText(incident.getDescription());
        holder.tvStatus.setText(incident.getStatut());

        // Logique visuelle du badge de statut
        if (incident.isTraite()) {
            holder.tvStatus.setBackgroundResource(R.drawable.status_traite_bg);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.status_new_bg);
        }

        // --- 4. MÉDIA (Photo de l'incident) ---
        if (incident.hasMedia()) {
            holder.cardMediaContainer.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(incident.getPhotoUrl())
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_incident_placeholder)
                    .into(holder.imgPhoto);

            holder.imgPhoto.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onImageClick(incident);
            });
        } else {
            holder.cardMediaContainer.setVisibility(View.GONE);
        }

        // --- 5. LOGIQUE DES DROITS (Visibilité des boutons) ---
        boolean isOwner = incident.getIdUtilisateur() != null && incident.getIdUtilisateur().equals(currentUserId);
        boolean isAdmin = "admin".equalsIgnoreCase(currentUserRole);
        boolean isAuthority = "autorite".equalsIgnoreCase(currentUserRole);

        // Modifier/Supprimer (Auteur ou Admin)
        int editVisibility = (isOwner || isAdmin) ? View.VISIBLE : View.GONE;
        holder.btnEdit.setVisibility(editVisibility);
        holder.btnDelete.setVisibility(editVisibility);

        // Valider (Autorité ou Admin et seulement si Nouveau)
        holder.btnValidate.setVisibility(((isAuthority || isAdmin) && !incident.isTraite()) ? View.VISIBLE : View.GONE);

        // Click Listeners Admin
        holder.btnEdit.setOnClickListener(v -> { if (actionListener != null) actionListener.onEditClick(incident); });
        holder.btnDelete.setOnClickListener(v -> { if (actionListener != null) actionListener.onDeleteClick(incident); });
        holder.btnValidate.setOnClickListener(v -> { if (actionListener != null) actionListener.onValidateClick(incident); });

        // --- 6. ACTIONS SOCIALES & CARTE ---
        holder.tvLikesCount.setText(String.valueOf(incident.getLikesCount()));
        holder.tvCommentsCount.setText(String.valueOf(incident.getCommentsCount()));

        // État du bouton Like
        if (incident.isLikedBy(currentUserId)) {
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light));
        } else {
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
        }

        // Listeners
        holder.btnLike.setOnClickListener(v -> toggleLike(incident, holder));
        holder.btnComment.setOnClickListener(v -> { if (actionListener != null) actionListener.onCommentClick(incident); });
        holder.btnMap.setOnClickListener(v -> { if (actionListener != null) actionListener.onMapClick(incident); });
    }

    private void toggleLike(Incident incident, IncidentViewHolder holder) {
        if (currentUserId == null) {
            Toast.makeText(context, "Connectez-vous pour aimer.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        boolean isLiked = incident.isLikedBy(currentUserId);

        // UI Optimiste
        if (isLiked) {
            incident.getLikedBy().remove(currentUserId);
            incident.setLikesCount(Math.max(0, incident.getLikesCount() - 1));
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
        } else {
            incident.getLikedBy().add(currentUserId);
            incident.setLikesCount(incident.getLikesCount() + 1);
            holder.btnLike.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light));
        }
        holder.tvLikesCount.setText(String.valueOf(incident.getLikesCount()));

        // Sync Firestore
        db.collection("incidents").document(incident.getId())
                .update("likesCount", FieldValue.increment(isLiked ? -1 : 1),
                        "likedBy", isLiked ? FieldValue.arrayRemove(currentUserId) : FieldValue.arrayUnion(currentUserId))
                .addOnFailureListener(e -> notifyDataSetChanged()); // Rollback en cas d'erreur
    }

    @Override
    public int getItemCount() {
        return incidentList != null ? incidentList.size() : 0;
    }

    public void updateData(List<Incident> newList) {
        this.incidentList = newList;
        notifyDataSetChanged();
    }

    static class IncidentViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvCategoryDate, tvStatus, tvUsername;
        TextView tvLikesCount, tvCommentsCount;
        ImageView imgPhoto, imgProfile;
        View cardMediaContainer;
        ImageButton btnMap, btnEdit, btnDelete, btnValidate, btnLike, btnComment;

        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_profile);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCategoryDate = itemView.findViewById(R.id.tv_category_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDescription = itemView.findViewById(R.id.tv_description);
            imgPhoto = itemView.findViewById(R.id.img_incident_photo);
            cardMediaContainer = itemView.findViewById(R.id.card_media_container);

            tvLikesCount = itemView.findViewById(R.id.tv_likes_count);
            tvCommentsCount = itemView.findViewById(R.id.tv_comments_count);

            btnMap = itemView.findViewById(R.id.btn_open_map);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnEdit = itemView.findViewById(R.id.btn_edit_incident);
            btnDelete = itemView.findViewById(R.id.btn_delete_incident);
            btnValidate = itemView.findViewById(R.id.btn_validate_incident);
        }
    }
}
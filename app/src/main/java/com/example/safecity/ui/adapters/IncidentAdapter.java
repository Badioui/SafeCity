package com.example.safecity.ui.adapters;

import android.content.Context;
import android.text.format.DateUtils; // Import pour "Il y a X min"
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
import com.example.safecity.MainActivity; // Import pour le casting
import com.example.safecity.R;
import com.example.safecity.model.Incident;

import java.util.List;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private Context context;
    private List<Incident> incidentList;
    private OnIncidentActionListener actionListener;

    public interface OnIncidentActionListener {
        void onMapClick(Incident incident); // Gardé pour compatibilité, mais on va utiliser le clic direct
        void onEditClick(Incident incident);
        void onDeleteClick(Incident incident);
    }

    public IncidentAdapter(Context context, List<Incident> incidentList, OnIncidentActionListener listener) {
        this.context = context;
        this.incidentList = incidentList;
        this.actionListener = listener;
    }

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

        // Textes
        holder.tvDescription.setText(incident.getDescription());
        holder.tvStatus.setText(incident.getStatut());

        String catName = incident.getNomCategorie();
        if (catName == null || catName.isEmpty()) catName = "Non classé";

        // --- 1. AMÉLIORATION DATE (Il y a X min) ---
        String dateAffichee = "Date inconnue";
        if (incident.getDateSignalement() != null) {
            long now = System.currentTimeMillis();
            long time = incident.getDateSignalement().getTime();

            // Affiche "Il y a 5 min", "Hier", etc.
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    time, now, DateUtils.MINUTE_IN_MILLIS);

            dateAffichee = relativeTime.toString();
        }
        holder.tvCategory.setText(catName + " • " + dateAffichee);

        // Utilisateur
        String userName = incident.getNomUtilisateur();
        holder.tvUsername.setText((userName != null && !userName.isEmpty()) ? userName : "Citoyen");

        // Image
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

        // --- 2. ACTIVATION BOUTON MAP POUR TOUS ---
        // Ce bouton doit marcher partout (Home et Profil)
        holder.btnMap.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                // On appelle la méthode qu'on va créer dans MainActivity
                ((MainActivity) context).navigateToMapAndFocus(incident.getLatitude(), incident.getLongitude());
            } else {
                Toast.makeText(context, "Lat: " + incident.getLatitude() + ", Lon: " + incident.getLongitude(), Toast.LENGTH_SHORT).show();
            }
        });

        // --- BOUTONS ÉDITION (Seulement pour Profil) ---
        if (actionListener != null) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> actionListener.onEditClick(incident));
            holder.btnDelete.setOnClickListener(v -> actionListener.onDeleteClick(incident));
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
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
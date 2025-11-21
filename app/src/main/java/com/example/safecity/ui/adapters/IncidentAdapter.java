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

import java.util.List;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private Context context;
    private List<Incident> incidentList;
    private OnIncidentActionListener actionListener; // Le fameux Listener

    // ✅ 1. DÉFINITION DE L'INTERFACE (Indispensable pour MyIncidentsFragment)
    public interface OnIncidentActionListener {
        void onMapClick(Incident incident);
        void onEditClick(Incident incident);
        void onDeleteClick(Incident incident);
    }

    // ✅ 2. CONSTRUCTEUR COMPLET (Pour MyIncidentsFragment - avec actions)
    public IncidentAdapter(Context context, List<Incident> incidentList, OnIncidentActionListener listener) {
        this.context = context;
        this.incidentList = incidentList;
        this.actionListener = listener;
    }

    // ✅ 3. CONSTRUCTEUR SIMPLE (Pour HomeFragment - sans actions)
    public IncidentAdapter(Context context, List<Incident> incidentList) {
        this(context, incidentList, null); // Appelle l'autre constructeur avec null
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
        holder.tvCategory.setText("Catégorie " + incident.getIdCategorie());
        holder.tvUsername.setText("Utilisateur #" + incident.getIdUtilisateur());

        // Image
        if (incident.getPhotoUrl() != null && !incident.getPhotoUrl().isEmpty()) {
            holder.imgPhoto.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(incident.getPhotoUrl())
                    .transform(new CenterCrop(), new RoundedCorners(16))
                    .placeholder(R.drawable.ic_incident_placeholder)
                    .into(holder.imgPhoto);
        } else {
            holder.imgPhoto.setImageResource(R.drawable.ic_incident_placeholder);
        }

        // Gestion de l'affichage des boutons (Selon si on a un listener ou pas)
        if (actionListener != null) {
            // Mode "Mes Incidents" : On active les boutons
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);

            holder.btnMap.setOnClickListener(v -> actionListener.onMapClick(incident));
            holder.btnEdit.setOnClickListener(v -> actionListener.onEditClick(incident));
            holder.btnDelete.setOnClickListener(v -> actionListener.onDeleteClick(incident));
        } else {
            // Mode "Home" : On cache les boutons d'édition (on garde juste la carte)
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);

            // Optionnel : Bouton carte actif même dans Home
            holder.btnMap.setOnClickListener(v -> {
                // Logique simple ou vide
            });
        }
    }

    @Override
    public int getItemCount() {
        return incidentList != null ? incidentList.size() : 0;
    }

    // Méthode unique pour mettre à jour la liste (utilisée partout)
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
            tvUsername = itemView.findViewById(R.id.tv_username); // Assurez-vous d'avoir cet ID dans le XML
            imgPhoto = itemView.findViewById(R.id.img_incident_photo);

            btnMap = itemView.findViewById(R.id.btn_open_map);
            btnEdit = itemView.findViewById(R.id.btn_edit_incident);
            btnDelete = itemView.findViewById(R.id.btn_delete_incident);
        }
    }
}
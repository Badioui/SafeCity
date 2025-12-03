package com.example.safecity.ui.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.safecity.R;
import com.example.safecity.model.NotificationApp;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    private Context context;
    private List<NotificationApp> notifList;

    public NotificationAdapter(Context context, List<NotificationApp> notifList) {
        this.context = context;
        this.notifList = notifList;
    }

    // --- AJOUT DE LA MÉTHODE MANQUANTE ---
    public void updateData(List<NotificationApp> newList) {
        this.notifList = newList;
        notifyDataSetChanged(); // Rafraîchit la vue avec les nouvelles données
    }
    // -------------------------------------

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        NotificationApp notif = notifList.get(position);
        holder.tvTitle.setText(notif.getTitre());
        holder.tvMessage.setText(notif.getMessage());

        if (notif.getDate() != null) {
            CharSequence time = DateUtils.getRelativeTimeSpanString(
                    notif.getDate().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvDate.setText(time);
        }

        // Changer l'icône selon le type (optionnel)
        if ("alerte".equals(notif.getType())) {
            holder.imgIcon.setImageResource(R.drawable.ic_info);
        } else {
            // Icône par défaut si besoin
            holder.imgIcon.setImageResource(R.drawable.ic_notifications);
        }
    }

    @Override
    public int getItemCount() {
        return notifList != null ? notifList.size() : 0;
    }

    public static class NotifViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate;
        ImageView imgIcon;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvDate = itemView.findViewById(R.id.tv_notif_date);
            imgIcon = itemView.findViewById(R.id.img_notif_icon);
        }
    }
}
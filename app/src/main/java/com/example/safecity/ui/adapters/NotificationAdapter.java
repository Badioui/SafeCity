package com.example.safecity.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.R;
import com.example.safecity.model.NotificationApp;

import java.util.List;

/**
 * Adaptateur pour les notifications.
 * Gère les différents types (alerte, validation, message) avec des couleurs distinctes.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    private final Context context;
    private List<NotificationApp> notifList;

    public NotificationAdapter(Context context, List<NotificationApp> notifList) {
        this.context = context;
        this.notifList = notifList;
    }

    public void updateData(List<NotificationApp> newList) {
        this.notifList = newList;
        notifyDataSetChanged();
    }

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

        // Date relative
        if (notif.getDate() != null) {
            CharSequence time = DateUtils.getRelativeTimeSpanString(
                    notif.getDate().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvDate.setText(time);
        }

        // --- LOGIQUE PREMIUM : Personnalisation par type ---
        String type = notif.getType() != null ? notif.getType().toLowerCase() : "default";

        if (type.contains("alerte")) {
            holder.imgIcon.setImageResource(R.drawable.ic_info);
            holder.cardIconContainer.setCardBackgroundColor(Color.parseColor("#FFF1F0")); // Rouge très clair
            holder.imgIcon.setColorFilter(Color.parseColor("#F44336")); // Rouge
        } else if (type.contains("validation") || type.contains("traité")) {
            holder.imgIcon.setImageResource(R.drawable.ic_check);
            holder.cardIconContainer.setCardBackgroundColor(Color.parseColor("#F6FFED")); // Vert très clair
            holder.imgIcon.setColorFilter(Color.parseColor("#52C41A")); // Vert
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_notifications);
            holder.cardIconContainer.setCardBackgroundColor(Color.parseColor("#F0F7FF")); // Bleu très clair
            holder.imgIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
        }

        // Gestion de l'indicateur de non-lu (si ton modèle NotificationApp a un champ isRead)
        // holder.viewUnread.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return notifList != null ? notifList.size() : 0;
    }

    public static class NotifViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate;
        ImageView imgIcon;
        com.google.android.material.card.MaterialCardView cardIconContainer;
        View viewUnread;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvDate = itemView.findViewById(R.id.tv_notif_date);
            imgIcon = itemView.findViewById(R.id.img_notif_icon);
            cardIconContainer = itemView.findViewById(R.id.card_icon_container);
            viewUnread = itemView.findViewById(R.id.view_unread_indicator);
        }
    }
}
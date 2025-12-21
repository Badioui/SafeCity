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
import androidx.recyclerview.widget.RecyclerView;

import com.example.safecity.R;
import com.example.safecity.model.NotificationApp;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationApp notification);
    }

    private final Context context;
    private List<NotificationApp> notifList;
    private final OnNotificationClickListener clickListener;

    public NotificationAdapter(Context context, List<NotificationApp> notifList, OnNotificationClickListener listener) {
        this.context = context;
        this.notifList = notifList;
        this.clickListener = listener;
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
        holder.bind(notif, clickListener);
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

        public void bind(final NotificationApp notif, final OnNotificationClickListener listener) {
            tvTitle.setText(notif.getTitre());
            tvMessage.setText(notif.getMessage());

            if (notif.getDate() != null) {
                CharSequence time = DateUtils.getRelativeTimeSpanString(
                        notif.getDate().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);
                tvDate.setText(time);
            }

            String type = notif.getType() != null ? notif.getType().toLowerCase() : "default";
            switch (type) {
                case "like":
                    imgIcon.setImageResource(R.drawable.ic_heart);
                    cardIconContainer.setCardBackgroundColor(Color.parseColor("#FFF0F6"));
                    imgIcon.setColorFilter(Color.parseColor("#FF4D4F"));
                    break;
                case "comment":
                    imgIcon.setImageResource(R.drawable.ic_send);
                    cardIconContainer.setCardBackgroundColor(Color.parseColor("#E6F7FF"));
                    imgIcon.setColorFilter(Color.parseColor("#1890FF"));
                    break;
                case "alerte":
                case "alerte_officielle":
                    imgIcon.setImageResource(R.drawable.ic_info);
                    cardIconContainer.setCardBackgroundColor(Color.parseColor("#FFF1F0"));
                    imgIcon.setColorFilter(Color.parseColor("#F5222D"));
                    break;
                case "validation":
                case "traité":
                    imgIcon.setImageResource(R.drawable.ic_check);
                    cardIconContainer.setCardBackgroundColor(Color.parseColor("#F6FFED"));
                    imgIcon.setColorFilter(Color.parseColor("#52C41A"));
                    break;
                default:
                    imgIcon.setImageResource(R.drawable.ic_notifications);
                    cardIconContainer.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                    imgIcon.setColorFilter(Color.parseColor("#8C8C8C"));
                    break;
            }

            viewUnread.setVisibility(notif.isLu() ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notif);
                }
            });
        }
    }
}

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

import com.bumptech.glide.Glide;
import com.example.safecity.R;
import com.example.safecity.model.Comment;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentList;

    public CommentAdapter(Context context, List<Comment> commentList) {
        this.context = context;
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.tvUsername.setText(comment.getNomUtilisateur());
        holder.tvText.setText(comment.getTexte());

        // Avatar
        Glide.with(context)
                .load(comment.getAuteurPhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(holder.imgProfile);

        // Date relative
        if (comment.getDatePublication() != null) {
            long time = comment.getDatePublication().getTime();
            long now = System.currentTimeMillis();
            holder.tvDate.setText(DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS));
        } else {
            holder.tvDate.setText("À l'instant");
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public void updateData(List<Comment> newList) {
        this.commentList = newList;
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView tvUsername, tvText, tvDate;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_comment_profile);
            tvUsername = itemView.findViewById(R.id.tv_comment_username);
            tvText = itemView.findViewById(R.id.tv_comment_text);
            tvDate = itemView.findViewById(R.id.tv_comment_date);
        }
    }
}
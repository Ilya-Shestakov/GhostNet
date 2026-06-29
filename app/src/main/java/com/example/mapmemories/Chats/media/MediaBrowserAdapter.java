package com.example.mapmemories.Chats.media;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mapmemories.Chats.ChatMessage;
import com.example.mapmemories.R;
import com.github.chrisbanes.photoview.PhotoView;
import java.util.List;

public class MediaBrowserAdapter extends RecyclerView.Adapter<MediaBrowserAdapter.ViewHolder> {

    private final List<ChatMessage> images;
    private final Runnable onClick;

    public MediaBrowserAdapter(List<ChatMessage> images, Runnable onClick) {
        this.images = images;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_browser_image, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = images.get(position);
        String path = (msg.getRemoteUrl() != null && !msg.getRemoteUrl().isEmpty()) ? msg.getRemoteUrl() : msg.getImageUrl();

        // Уникальное имя для анимации
        androidx.core.view.ViewCompat.setTransitionName(holder.photoView, "photo_" + msg.getMessageId());

        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(path)
                .into(holder.photoView);

        holder.photoView.setOnPhotoTapListener((view, x, y) -> onClick.run());

        holder.photoView.setOnMatrixChangeListener(rect -> {
            holder.photoView.getParent().requestDisallowInterceptTouchEvent(holder.photoView.getScale() > 1.0f);
        });
    }


    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }
}
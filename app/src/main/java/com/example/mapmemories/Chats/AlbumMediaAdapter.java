package com.example.mapmemories.Chats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mapmemories.R;

import java.util.List;

public class AlbumMediaAdapter extends RecyclerView.Adapter<AlbumMediaAdapter.ViewHolder> {
    private Context context;
    private List<String> urls;
    private List<String> types;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(String url, int position);
    }

    public AlbumMediaAdapter(Context context, List<String> urls, List<String> types, OnItemClickListener listener) {
        this.context = context;
        this.urls = urls;
        this.types = types;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album_thumb, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = urls.get(position);
        String type = types.size() > position ? types.get(position) : "image";
        Glide.with(context)
                .load(url)
                .centerCrop()
                .override(200, 200)
                .into(holder.imageView);
        holder.videoIcon.setVisibility("video".equals(type) ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(url, position);
        });
    }

    @Override public int getItemCount() { return urls.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView videoIcon;
        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.albumThumb);
            videoIcon = itemView.findViewById(R.id.albumVideoIcon);
        }
    }
}
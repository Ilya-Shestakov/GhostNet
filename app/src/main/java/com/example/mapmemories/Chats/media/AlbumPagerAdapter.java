package com.example.mapmemories.Chats.media;

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

public class AlbumPagerAdapter extends RecyclerView.Adapter<AlbumPagerAdapter.PagerViewHolder> {
    private Context context;
    private List<String> urls;
    private OnAlbumClickListener clickListener;

    public interface OnAlbumClickListener {
        void onClick(View v, int position);
        void onLongClick(View v, int position);
    }

    public AlbumPagerAdapter(Context context, List<String> urls, OnAlbumClickListener clickListener) {
        this.context = context;
        this.urls = urls;
        this.clickListener = clickListener;
    }

    @Override
    public void onBindViewHolder(@NonNull PagerViewHolder holder, int position) {
        int realIdx = position % urls.size();
        Glide.with(context).load(urls.get(realIdx)).centerCrop().into(holder.imageView);

        // Вешаем клики прямо на ImageView
        holder.imageView.setOnClickListener(v -> clickListener.onClick(v, realIdx));
        holder.imageView.setOnLongClickListener(v -> {
            clickListener.onLongClick(v, realIdx);
            return true;
        });
    }

    public static final int INFINITE_SIZE = 10000;

    @NonNull
    @Override
    public PagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Используем вашу готовую разметку из проекта
        View view = LayoutInflater.from(context).inflate(R.layout.item_album_page, parent, false);
        return new PagerViewHolder(view);
    }

    @Override
    public int getItemCount() {
        if (urls == null || urls.isEmpty()) return 0;
        return urls.size() > 1 ? INFINITE_SIZE : urls.size();
    }

    static class PagerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        PagerViewHolder(@NonNull View itemView) {
            super(itemView);
            // Убедитесь, что в item_album_page.xml ID именно такой
            imageView = itemView.findViewById(R.id.albumImage);

            // Если ID другой, поправьте его здесь.
            // Если вы создавали ImageView программно, то: imageView = (ImageView) itemView;
        }
    }
}
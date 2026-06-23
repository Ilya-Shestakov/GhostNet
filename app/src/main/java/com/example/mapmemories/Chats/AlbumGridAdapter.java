package com.example.mapmemories.Chats;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class AlbumGridAdapter extends RecyclerView.Adapter<AlbumGridAdapter.GridViewHolder> {
    private Context context;
    private List<String> urls;
    private OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(int position); }

    public AlbumGridAdapter(Context context, List<String> urls, OnItemClickListener listener) {
        this.context = context;
        this.urls = urls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(context);
        // Вычисляем размер ячейки (примерно 1/3 ширины контейнера)
        int size = parent.getWidth() / 3;
        if (size <= 0) size = (int) (90 * context.getResources().getDisplayMetrics().density);

        iv.setLayoutParams(new ViewGroup.LayoutParams(size, size));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setPadding(2, 2, 2, 2);
        return new GridViewHolder(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Glide.with(context).load(urls.get(position)).centerCrop().into((ImageView) holder.itemView);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() { return urls.size(); }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        GridViewHolder(@NonNull View itemView) { super(itemView); }
    }
}
package com.example.mapmemories.Chats.media;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class AlbumThumbAdapter extends RecyclerView.Adapter<AlbumThumbAdapter.ThumbViewHolder> {
    private Context context;
    private List<String> fullUrls;
    private List<String> displayUrls = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener { void onItemClick(int originalPos); }

    public AlbumThumbAdapter(Context context, List<String> fullUrls, OnItemClickListener listener) {
        this.context = context;
        this.fullUrls = fullUrls;
        this.listener = listener;
        updateRotation(0); // Инициализация
    }

    public void updateRotation(int currentMainIndex) {
        int size = fullUrls.size();
        if (size <= 1) return;

        displayUrls.clear();

        // Начинаем всегда с "одного шага назад" от текущей большой фотки
        // Это и есть та фотка, которая при свайпе вперед должна оказаться крайней левой
        int startPoint = (currentMainIndex - 1 + size) % size;

        // Проходим по кругу ровно столько раз, сколько всего фоток
        for (int i = 0; i < size; i++) {
            int targetIdx = (startPoint + i) % size;

            // Если этот индекс НЕ является текущей большой фоткой — добавляем в ленту
            if (targetIdx != currentMainIndex) {
                displayUrls.add(fullUrls.get(targetIdx));
            }
        }

        // ВАЖНО: При такой логике в displayUrls ВСЕГДА будет ровно (size - 1) элементов
        // и они всегда будут стоять в правильном "вращающемся" порядке
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThumbViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(context);
        int size = (int) (50 * context.getResources().getDisplayMetrics().density);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(size, size);
        lp.setMargins(6, 0, 6, 0);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setClipToOutline(true);
        iv.setBackgroundResource(com.example.mapmemories.R.drawable.bg_media_thumb);
        return new ThumbViewHolder(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull ThumbViewHolder holder, int position) {
        String url = displayUrls.get(position);
        Glide.with(context).load(url).centerCrop().into((ImageView) holder.itemView);
        holder.itemView.setOnClickListener(v -> {
            int idx = fullUrls.indexOf(url);
            if (idx != -1) listener.onItemClick(idx);
        });
    }

    @Override
    public int getItemCount() { return displayUrls.size(); }
    static class ThumbViewHolder extends RecyclerView.ViewHolder {
        ThumbViewHolder(@NonNull View itemView) { super(itemView); }
    }
}
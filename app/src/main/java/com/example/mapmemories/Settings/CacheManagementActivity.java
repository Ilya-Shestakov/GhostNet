package com.example.mapmemories.Settings;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.mapmemories.R;
import com.example.mapmemories.database.AppDatabase;
import com.example.mapmemories.systemHelpers.MediaCacheManager;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.DecimalFormat;

public class CacheManagementActivity extends AppCompatActivity {

    private TextView tvMessagesSize, tvOfflinePostsSize, tvMediaCacheSize;
    private ProgressBar progressMessages, progressOfflinePosts, progressMediaCache;

    // Максимальные размеры для расчёта прогресса (в байтах)
    private static final long MAX_MESSAGES_BYTES = 10 * 1024 * 1024;   // 10 MB
    private static final long MAX_OFFLINE_POSTS_BYTES = 50 * 1024 * 1024; // 50 MB
    private static final long MAX_MEDIA_CACHE_BYTES = 100 * 1024 * 1024;  // 100 MB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache_management);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        tvMessagesSize = findViewById(R.id.tvMessagesSize);
        tvOfflinePostsSize = findViewById(R.id.tvOfflinePostsSize);
        tvMediaCacheSize = findViewById(R.id.tvMediaCacheSize);

        progressMessages = findViewById(R.id.progressMessages);
        progressOfflinePosts = findViewById(R.id.progressOfflinePosts);
        progressMediaCache = findViewById(R.id.progressMediaCache);

        MaterialButton btnClearMessages = findViewById(R.id.btnClearMessages);
        MaterialButton btnClearOfflinePosts = findViewById(R.id.btnClearOfflinePosts);
        MaterialButton btnClearMediaCache = findViewById(R.id.btnClearMediaCache);
        MaterialButton btnClearAll = findViewById(R.id.btnClearAll);

        loadSizes();

        btnClearMessages.setOnClickListener(v -> clearMessages());
        btnClearOfflinePosts.setOnClickListener(v -> clearOfflinePosts());
        btnClearMediaCache.setOnClickListener(v -> clearMediaCache());
        btnClearAll.setOnClickListener(v -> clearAll());
    }

    private void loadSizes() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            int messageCount = db.localMessageDao().getMessageCountAll();
            long messagesSizeBytes = messageCount * 512L;

            int offlinePostCount = db.offlinePostDao().getAllPostsSync().size();
            long offlinePostsSizeBytes = offlinePostCount * 1024L;

            long mediaCacheBytes = getFolderSize(new File(getFilesDir(), "ghost_media"));
            long glideCacheBytes = getFolderSize(new File(getCacheDir(), "image_manager_disk_cache"));
            long totalMediaBytes = mediaCacheBytes + glideCacheBytes;

            runOnUiThread(() -> {
                tvMessagesSize.setText(formatSize(messagesSizeBytes));
                updateProgress(progressMessages, messagesSizeBytes, MAX_MESSAGES_BYTES);

                tvOfflinePostsSize.setText(formatSize(offlinePostsSizeBytes));
                updateProgress(progressOfflinePosts, offlinePostsSizeBytes, MAX_OFFLINE_POSTS_BYTES);

                tvMediaCacheSize.setText(formatSize(totalMediaBytes));
                updateProgress(progressMediaCache, totalMediaBytes, MAX_MEDIA_CACHE_BYTES);
            });
        }).start();
    }

    private long getFolderSize(File dir) {
        long size = 0;
        if (dir != null && dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.isFile()) size += f.length();
                else size += getFolderSize(f);
            }
        }
        return size;
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void updateProgress(ProgressBar bar, long current, long max) {
        int percent = (int) ((current * 100) / max);
        percent = Math.min(percent, 100); // не больше 100%
        bar.setProgress(percent);

        // Цвет: зелёный (0%) -> жёлтый (50%) -> красный (100%)
        float fraction = percent / 100f;
        int color;
        if (fraction < 0.5f) {
            // зелёный -> жёлтый
            int green = Color.rgb(76, 175, 80);   // #4CAF50
            int yellow = Color.rgb(255, 235, 59);  // #FFEB3B
            color = blendColors(green, yellow, fraction * 2);
        } else {
            // жёлтый -> красный
            int yellow = Color.rgb(255, 235, 59);
            int red = Color.rgb(244, 67, 54);      // #F44336
            color = blendColors(yellow, red, (fraction - 0.5f) * 2);
        }
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private int blendColors(int color1, int color2, float ratio) {
        int r = (int) (Color.red(color1) + (Color.red(color2) - Color.red(color1)) * ratio);
        int g = (int) (Color.green(color1) + (Color.green(color2) - Color.green(color1)) * ratio);
        int b = (int) (Color.blue(color1) + (Color.blue(color2) - Color.blue(color1)) * ratio);
        return Color.rgb(r, g, b);
    }

    private void clearMessages() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            db.localMessageDao().deleteAllMessages();
            runOnUiThread(() -> {
                Toast.makeText(this, "Сообщения удалены", Toast.LENGTH_SHORT).show();
                loadSizes();
            });
        }).start();
    }

    private void clearOfflinePosts() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            db.offlinePostDao().deleteAll();
            runOnUiThread(() -> {
                Toast.makeText(this, "Офлайн-посты удалены", Toast.LENGTH_SHORT).show();
                loadSizes();
            });
        }).start();
    }

    private void clearMediaCache() {
        MediaCacheManager.clearCache(this);
        new Thread(() -> {
            Glide.get(this).clearDiskCache();
            runOnUiThread(() -> {
                Toast.makeText(this, "Медиа-кэш очищен", Toast.LENGTH_SHORT).show();
                loadSizes();
            });
        }).start();
    }

    private void clearAll() {
        clearMessages();
        clearOfflinePosts();
        clearMediaCache();
        Toast.makeText(this, "Все данные удалены", Toast.LENGTH_SHORT).show();
        loadSizes();
    }
}
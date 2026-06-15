package com.example.mapmemories.Chats;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.mapmemories.R;
import com.example.mapmemories.database.AppDatabase;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class MediaBrowserActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvCounter;
    private View topBar, browserRoot, topBarBg;
    private List<ChatMessage> imageMessages;
    private String chatId;
    private int currentPos;
    private String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

    private float startY, startX;
    private boolean isDragging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_browser);

        imageMessages = (List<ChatMessage>) getIntent().getSerializableExtra("images");
        currentPos = getIntent().getIntExtra("position", 0);
        chatId = getIntent().getStringExtra("chatId");

        viewPager = findViewById(R.id.viewPager);
        tvCounter = findViewById(R.id.tvCounter);
        topBar = findViewById(R.id.topBar);
        topBarBg = findViewById(R.id.topBarBg);
        browserRoot = findViewById(R.id.browserRoot);

        MediaBrowserAdapter adapter = new MediaBrowserAdapter(imageMessages, this::toggleUi);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPos, false);
        updateCounter(currentPos);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPos = position;
                updateCounter(position);
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDownload).setOnClickListener(v -> downloadImage());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareImage());
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            ChatMessage msg = imageMessages.get(currentPos);
            showPremiumDeleteDialog(msg, msg.getSenderId().equals(currentUserId));
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getPointerCount() > 1) {
            isDragging = false;
            viewPager.setUserInputEnabled(false);
            return super.dispatchTouchEvent(ev);
        }

        PhotoView currentPhoto = getCurrentPhotoView();
        if (currentPhoto == null || currentPhoto.getScale() > 1.0f) return super.dispatchTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getRawY();
                startX = ev.getRawX();
                isDragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = ev.getRawY() - startY;
                float deltaX = ev.getRawX() - startX;

                if (!isDragging && Math.abs(deltaY) > 40 && Math.abs(deltaY) > Math.abs(deltaX)) {
                    isDragging = true;
                }

                if (isDragging) {
                    viewPager.setTranslationY(deltaY);
                    float alpha = 1f - (Math.abs(deltaY) / 1500f);
                    browserRoot.setBackgroundColor(Color.argb((int) (Math.max(0, alpha) * 255), 0, 0, 0));
                    topBar.setAlpha(1f - (Math.abs(deltaY) / 500f));
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    if (Math.abs(viewPager.getTranslationY()) > 350) {
                        // КРАСИВОЕ ЗАКРЫТИЕ
                        supportFinishAfterTransition();
                    } else {
                        viewPager.animate().translationY(0).setDuration(250)
                                .setInterpolator(new android.view.animation.OvershootInterpolator(1.0f)).start();
                        browserRoot.setBackgroundColor(Color.BLACK);
                        topBar.animate().alpha(1f).setDuration(250).start();
                    }
                    isDragging = false;
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private PhotoView getCurrentPhotoView() {
        View view = ((ViewGroup) viewPager.getChildAt(0)).getChildAt(0);
        if (view != null) return view.findViewById(R.id.photoView);
        return null;
    }




    private void showPremiumDeleteDialog(ChatMessage message, boolean isMine) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activityRootView.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.MIRROR));
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#66000000"));

        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setCardBackgroundColor(Color.parseColor("#1C1C1E"));
        cardView.setRadius(48f);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams((int) (getResources().getDisplayMetrics().widthPixels * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER;
        cardView.setLayoutParams(cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(64, 64, 64, 48);

        TextView title = new TextView(this);
        title.setText("Удалить фото?");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        cardContent.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Это действие нельзя отменить.");
        desc.setTextColor(Color.GRAY);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 16, 0, 48);
        cardContent.addView(desc);

        final boolean[] deleteForEveryone = {false};
        if (isMine) {
            LinearLayout toggleRow = new LinearLayout(this);
            toggleRow.setOrientation(LinearLayout.HORIZONTAL);
            toggleRow.setGravity(Gravity.CENTER_VERTICAL);
            toggleRow.setPadding(32, 24, 32, 24);
            GradientDrawable toggleBg = new GradientDrawable();
            toggleBg.setCornerRadius(16f);
            toggleBg.setColor(Color.parseColor("#1AFFFFFF"));
            toggleRow.setBackground(toggleBg);

            ImageView tickIcon = new ImageView(this);
            tickIcon.setImageResource(R.drawable.ic_check);
            tickIcon.setColorFilter(Color.GRAY);
            tickIcon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));

            TextView toggleText = new TextView(this);
            toggleText.setText("Удалить также у собеседника");
            toggleText.setTextColor(Color.WHITE);
            toggleText.setPadding(24, 0, 0, 0);

            toggleRow.addView(tickIcon);
            toggleRow.addView(toggleText);
            cardContent.addView(toggleRow);

            toggleRow.setOnClickListener(v -> {
                deleteForEveryone[0] = !deleteForEveryone[0];
                tickIcon.setColorFilter(deleteForEveryone[0] ? Color.parseColor("#FF5252") : Color.GRAY);
                toggleBg.setColor(deleteForEveryone[0] ? Color.parseColor("#33FF5252") : Color.parseColor("#1AFFFFFF"));
            });
        }

        LinearLayout btnContainer = new LinearLayout(this);
        btnContainer.setPadding(0, 48, 0, 0);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Отмена");
        btnCancel.setTextColor(Color.WHITE);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        btnCancel.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) activityRootView.setRenderEffect(null);
            dialog.dismiss();
        });

        TextView btnDelete = new TextView(this);
        btnDelete.setText("Удалить");
        btnDelete.setTextColor(Color.parseColor("#FF5252"));
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setTypeface(null, android.graphics.Typeface.BOLD);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        btnDelete.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) activityRootView.setRenderEffect(null);
            dialog.dismiss();
            performDelete(message, deleteForEveryone[0]);
        });

        btnContainer.addView(btnCancel);
        btnContainer.addView(btnDelete);
        cardContent.addView(btnContainer);
        cardView.addView(cardContent);
        root.addView(cardView);
        dialog.setContentView(root);
        dialog.show();
    }

    private void performDelete(ChatMessage msg, boolean forEveryone) {
        if (forEveryone) {
            FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages").child(msg.getMessageId()).removeValue();
        } else {
            FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages").child(msg.getMessageId()).child("deletedBy").setValue(currentUserId);
        }
        new Thread(() -> {
            AppDatabase.getDatabase(this).localMessageDao().deleteById(msg.getMessageId());
            runOnUiThread(() -> {
                imageMessages.remove(currentPos);
                if (imageMessages.isEmpty()) finish();
                else {
                    viewPager.getAdapter().notifyDataSetChanged();
                    updateCounter(viewPager.getCurrentItem());
                }
            });
        }).start();
    }

    private void updateCounter(int position) {
        tvCounter.setText((position + 1) + " из " + imageMessages.size());
    }

    private void toggleUi() {
        float alpha = topBar.getAlpha() == 1f ? 0f : 1f;
        topBar.animate().alpha(alpha).setDuration(200).start();
        topBarBg.animate().alpha(alpha).setDuration(200).start();
    }

    private void downloadImage() {
        ChatMessage msg = imageMessages.get(currentPos);
        String url = (msg.getRemoteUrl() != null) ? msg.getRemoteUrl() : msg.getImageUrl();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "GhostNet_" + System.currentTimeMillis() + ".jpg");
        ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
        Toast.makeText(this, "Загрузка началась", Toast.LENGTH_SHORT).show();
    }

    private void shareImage() {
        ChatMessage msg = imageMessages.get(currentPos);
        String url = (msg.getRemoteUrl() != null) ? msg.getRemoteUrl() : msg.getImageUrl();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Фото из GhostNet: " + url);
        startActivity(Intent.createChooser(intent, "Поделиться"));
    }
}
package com.example.mapmemories.Chats;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.WindowManager;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.mapmemories.R;
import com.example.mapmemories.database.AppDatabase;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.database.FirebaseDatabase;

public class SecretMediaViewerActivity extends AppCompatActivity {

    private String messageId, chatId, imageUrl;
    private int timerSeconds;
    private boolean isOneTime;
    private TextView tvCountdown, tvSenderName, tvMessageTime, tvCaption;
    private View topBar, topGradient, bottomGradient;
    private PhotoView pvSecret;
    private boolean isUiVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_secret_media_viewer);

        imageUrl = getIntent().getStringExtra("imageUrl");
        timerSeconds = getIntent().getIntExtra("timer", 0);
        isOneTime = getIntent().getBooleanExtra("isOneTime", false);
        messageId = getIntent().getStringExtra("messageId");
        chatId = getIntent().getStringExtra("chatId");

        pvSecret = findViewById(R.id.pvSecret);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvSenderName = findViewById(R.id.tvSenderName);
        tvMessageTime = findViewById(R.id.tvMessageTime);
        tvCaption = findViewById(R.id.tvCaption);
        topBar = findViewById(R.id.topBar);
        topGradient = findViewById(R.id.topGradient);
        bottomGradient = findViewById(R.id.bottomGradient);

        Glide.with(this).load(imageUrl).into(pvSecret);

        pvSecret.setOnClickListener(v -> toggleUi());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (timerSeconds > 0) {
            startDestructionTimer();
        }

        String sender = getIntent().getStringExtra("senderName");
        String time = getIntent().getStringExtra("time");
        String caption = getIntent().getStringExtra("caption");

        if (sender != null) tvSenderName.setText(sender);
        if (time != null) tvMessageTime.setText(time);
        if (caption != null && !caption.isEmpty()) tvCaption.setText(caption);
        else tvCaption.setVisibility(View.GONE);
    }

    private void toggleUi() {
        isUiVisible = !isUiVisible;
        float alpha = isUiVisible ? 1f : 0f;
        topBar.animate().alpha(alpha).setDuration(200).start();
        topGradient.animate().alpha(alpha).setDuration(200).start();
        bottomGradient.animate().alpha(alpha).setDuration(200).start();
        tvCaption.animate().alpha(alpha).setDuration(200).start();
        if (!isUiVisible) hideSystemUI(); else showSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void startDestructionTimer() {
        tvCountdown.setVisibility(View.VISIBLE);
        new CountDownTimer(timerSeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                destroyMessageAndExit();
            }
        }.start();
    }

    private void destroyMessageAndExit() {
        FirebaseDatabase.getInstance().getReference("chats")
                .child(chatId).child("messages").child(messageId).removeValue();

        new Thread(() -> {
            AppDatabase.getDatabase(this).localMessageDao().deleteById(messageId);
            runOnUiThread(this::finish);
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (isOneTime || timerSeconds > 0) {
            destroyMessageAndExit();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isOneTime || timerSeconds > 0) {
            destroyMessageAndExit();
        }
    }
}
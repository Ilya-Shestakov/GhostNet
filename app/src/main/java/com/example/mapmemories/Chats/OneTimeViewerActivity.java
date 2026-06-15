package com.example.mapmemories.Chats;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.mapmemories.R;
import com.google.firebase.database.FirebaseDatabase;

public class OneTimeViewerActivity extends AppCompatActivity {

    private String messageId, chatId;
    private boolean isDeleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_one_time_viewer);

        String url = getIntent().getStringExtra("imageUrl");
        messageId = getIntent().getStringExtra("messageId");
        chatId = getIntent().getStringExtra("chatId");

        ImageView iv = findViewById(R.id.pvOneTime);
        Glide.with(this).load(url).into(iv);
    }

    private void deleteAndExit() {
        if (isDeleted) return;
        isDeleted = true;

        if (chatId != null && messageId != null) {
            FirebaseDatabase.getInstance().getReference("chats")
                    .child(chatId).child("messages").child(messageId).removeValue();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        deleteAndExit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        deleteAndExit();
    }
}
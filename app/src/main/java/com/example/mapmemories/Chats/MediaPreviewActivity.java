package com.example.mapmemories.Chats;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.mapmemories.R;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MediaPreviewActivity extends AppCompatActivity {

    private Uri imageUri;
    private boolean isOneTime = false;
    //private TextView tvModeDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_preview);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            getWindow().getAttributes().setBlurBehindRadius(40);
        }

        imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        isOneTime = getIntent().getBooleanExtra("isOneTime", false);

        PhotoView pvPreview = findViewById(R.id.pvPreview);

        SwitchMaterial swOneTime = findViewById(R.id.swOneTime);
        swOneTime.setChecked(isOneTime);
        updateText(isOneTime);

        FloatingActionButton btnDone = findViewById(R.id.btnDone);

        Glide.with(this).load(imageUri).into(pvPreview);

        swOneTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isOneTime = isChecked;
            updateText(isOneTime);
        });

        btnDone.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("imageUri", imageUri.toString());
            result.putExtra("isOneTime", isOneTime);
            result.putExtra("timerValue", 0);
            setResult(RESULT_OK, result);
            supportFinishAfterTransition();
        });
    }

    private void updateText(boolean oneTime) {
        if (oneTime) {
            //tvModeDesc.setText("Разовый просмотр (сообщение удалится)");
        } else {
            //tvModeDesc.setText("Обычное сообщение");
        }
    }

    @Override
    public void onBackPressed() {
        supportFinishAfterTransition();
    }
}
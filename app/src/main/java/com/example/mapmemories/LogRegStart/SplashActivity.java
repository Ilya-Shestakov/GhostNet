package com.example.mapmemories.LogRegStart;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapmemories.R;
import com.example.mapmemories.systemHelpers.VibratorHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ImageView logoImageView;
    private ImageView rippleCircle1, rippleCircle2;
    private TextView appNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logoImageView = findViewById(R.id.logoImageView);
        rippleCircle1 = findViewById(R.id.rippleCircle1);
        rippleCircle2 = findViewById(R.id.rippleCircle2);
        appNameTextView = findViewById(R.id.appNameTextView);

        logoImageView.setTranslationY(-2500f);

        appNameTextView.setAlpha(0f);
        appNameTextView.setTranslationY(100f);

        startDropAnimation();
    }

    private void startDropAnimation() {
        logoImageView.animate()
                .translationY(0f)
                .setDuration(1200)
                .setStartDelay(300)
                .setInterpolator(new BounceInterpolator())
                .start();


        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            VibratorHelper.vibrate(this, 70);

            playShockwaveAnimation();

        }, 850);


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            VibratorHelper.vibrate(this, 20);

            appNameTextView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator())
                    .start();

        }, 1400);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2500);
    }

    private void playShockwaveAnimation() {

        rippleCircle1.setAlpha(0.7f);
        rippleCircle1.setScaleX(1f);
        rippleCircle1.setScaleY(1f);

        rippleCircle1.animate()
                .scaleX(3.5f)
                .scaleY(3.5f)
                .alpha(0f)
                .setDuration(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        rippleCircle2.setAlpha(0.5f);
        rippleCircle2.setScaleX(1f);
        rippleCircle2.setScaleY(1f);

        rippleCircle2.animate()
                .scaleX(4.5f)
                .scaleY(4.5f)
                .alpha(0f)
                .setDuration(800)
                .setStartDelay(100)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
}
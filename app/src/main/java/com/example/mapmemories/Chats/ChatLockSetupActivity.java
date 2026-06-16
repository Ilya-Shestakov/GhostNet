package com.example.mapmemories.Chats;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mapmemories.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

public class ChatLockSetupActivity extends AppCompatActivity {

    private View[] dots = new View[6];
    private StringBuilder passwordBuilder = new StringBuilder();
    private String firstPassword = null;
    private boolean isConfirming = false;
    private MaterialButton btnSave;
    private TextView tvPasswordTitle, tvPasswordSubtitle;
    private RadioButton radioBiometric, radioPassword;
    private boolean biometricAvailable = false;
    private String selectedMethod = "password"; // "password" или "biometric"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_lock_setup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        tvPasswordTitle = findViewById(R.id.tvPasswordTitle);
        tvPasswordSubtitle = findViewById(R.id.tvPasswordSubtitle);
        btnSave = findViewById(R.id.btnSave);
        radioBiometric = findViewById(R.id.radioBiometric);
        radioPassword = findViewById(R.id.radioPassword);
        MaterialCardView cardBiometric = findViewById(R.id.cardBiometric);
        MaterialCardView cardPassword = findViewById(R.id.cardPassword);

        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);
        dots[4] = findViewById(R.id.dot5);
        dots[5] = findViewById(R.id.dot6);

        BiometricManager bm = BiometricManager.from(this);
        biometricAvailable = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;

        if (!biometricAvailable) {
            cardBiometric.setAlpha(0.5f);
            radioBiometric.setEnabled(false);
        }

        radioPassword.setChecked(true);
        selectedMethod = "password";

        radioBiometric.setOnClickListener(v -> {
            if (!biometricAvailable) return;
            radioBiometric.setChecked(true);
            radioBiometric.setChecked(true);
            radioPassword.setChecked(false);
            selectedMethod = "biometric";
        });

        radioPassword.setOnClickListener(v -> {
            radioPassword.setChecked(true);
            radioBiometric.setChecked(false);
            selectedMethod = "password";
        });

        setupKeyboard();
        btnSave.setOnClickListener(v -> savePasswordAndFinish());
    }

    private void setupKeyboard() {
        GridLayout grid = findViewById(R.id.keyboardGrid);
        String[] keys = {"1","2","3","4","5","6","7","8","9","","0","⌫"};

        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            TextView btn = new TextView(this);
            btn.setText(key);
            btn.setTextSize(24f);
            btn.setTextColor(getColor(R.color.text_primary));
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundResource(R.drawable.key_bg);
            btn.setClickable(true);
            btn.setFocusable(true);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(80);
            params.height = dpToPx(80);
            params.setMargins(12, 12, 12, 12);
            params.rowSpec = GridLayout.spec(i / 3);
            params.columnSpec = GridLayout.spec(i % 3);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                animateKeyPress(v);
                onKeyPressed(key);
            });

            grid.addView(btn);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void animateKeyPress(View view) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f);
        scaleUpX.setDuration(100);
        scaleUpY.setDuration(100);
        set.play(scaleDownX).with(scaleDownY);
        set.play(scaleUpX).with(scaleUpY).after(scaleDownX);
        set.start();
    }

    private void onKeyPressed(String key) {
        if (key.equals("⌫")) {
            if (passwordBuilder.length() > 0) {
                dots[passwordBuilder.length() - 1].setBackgroundResource(R.drawable.dot_inactive);
                passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);
            }
            return;
        }
        if (key.isEmpty()) return;
        if (passwordBuilder.length() >= 6) return;

        passwordBuilder.append(key);
        dots[passwordBuilder.length() - 1].setBackgroundResource(R.drawable.dot_active);

        if (passwordBuilder.length() == 6) {
            if (!isConfirming) {
                firstPassword = passwordBuilder.toString();
                passwordBuilder.setLength(0);
                isConfirming = true;
                tvPasswordTitle.setText("Повторите пароль");
                tvPasswordSubtitle.setText("Введите пароль ещё раз");
                clearDots();
            } else {
                if (passwordBuilder.toString().equals(firstPassword)) {
                    tvPasswordTitle.setText("Пароль принят");
                    tvPasswordSubtitle.setText("Нажмите «Сохранить»");
                    btnSave.setVisibility(View.VISIBLE);
                    disableKeyboard();
                } else {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                    firstPassword = null;
                    isConfirming = false;
                    passwordBuilder.setLength(0);
                    clearDots();
                    tvPasswordTitle.setText("Придумайте пароль");
                    tvPasswordSubtitle.setText("Введите 6 цифр");
                }
            }
        }
    }

    private void clearDots() {
        for (View dot : dots) dot.setBackgroundResource(R.drawable.dot_inactive);
    }

    private void disableKeyboard() {
        GridLayout grid = findViewById(R.id.keyboardGrid);
        for (int i = 0; i < grid.getChildCount(); i++) {
            grid.getChildAt(i).setEnabled(false);
        }
    }

    private void savePasswordAndFinish() {
        // Если выбрана биометрия, сначала аутентифицируемся, потом сохраняем
        if (selectedMethod.equals("biometric")) {
            authenticateAndSave();
        } else {
            savePasswordHash(false);
            finishWithSuccess();
        }
    }

    private void authenticateAndSave() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                savePasswordHash(true);
                finishWithSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(ChatLockSetupActivity.this, "Не удалось подтвердить личность", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Toast.makeText(ChatLockSetupActivity.this, "Ошибка: " + errString, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Подтверждение биометрии")
                .setSubtitle("Используйте отпечаток пальца или лицо")
                .setNegativeButtonText("Отмена")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void savePasswordHash(boolean useBiometric) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(firstPassword.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String passwordHash = hexString.toString();

            SharedPreferences prefs = getSharedPreferences("chat_lock", MODE_PRIVATE);
            prefs.edit()
                    .putString("password_hash", passwordHash)
                    .putBoolean("use_biometric", useBiometric)
                    .apply();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void finishWithSuccess() {
        Toast.makeText(this, "Настройки блокировки сохранены", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
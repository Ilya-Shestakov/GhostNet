package com.example.mapmemories.LogRegStart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mapmemories.Info.SecurityInfoActivity;
import com.example.mapmemories.Lenta.MainActivity;
import com.example.mapmemories.R;
import com.example.mapmemories.systemHelpers.CryptoHelper;
import com.example.mapmemories.systemHelpers.LocalAccount;
import com.example.mapmemories.systemHelpers.MultiAccountManager;
import com.example.mapmemories.systemHelpers.VibratorHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private Button loginButton;
    private TextView registerButton, forgotPasswordTextView;
    private ProgressBar progressBar;
    private RelativeLayout overlay;
    private ImageView questionSecurity;
    private FirebaseAuth mAuth;

    private LinearLayout loginFieldsContainer, qrContainer;
    private ImageView ivQrCode;
    private Button btnSwitchMode;
    private boolean isQrMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        checkCurrentUser();

        initViews();
        setupClickListeners();
    }

    private void checkCurrentUser() {
        if (!getIntent().getBooleanExtra("is_adding_account", false)) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        }
    }

    private void initViews() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);

        if (emailInputLayout != null) emailEditText = (TextInputEditText) emailInputLayout.getEditText();
        if (passwordInputLayout != null) passwordEditText = (TextInputEditText) passwordInputLayout.getEditText();

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        progressBar = findViewById(R.id.progressBar);
        overlay = findViewById(R.id.overlay);

        loginFieldsContainer = findViewById(R.id.loginFieldsContainer);
        qrContainer = findViewById(R.id.qrContainer);
        ivQrCode = findViewById(R.id.ivQrCode);
        btnSwitchMode = findViewById(R.id.btnSwitchMode);

        questionSecurity = findViewById(R.id.questionSecurity);

        showLoading(false);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        forgotPasswordTextView.setOnClickListener(v -> showForgotPasswordDialog());

        btnSwitchMode.setOnClickListener(v -> toggleLoginMode());

        questionSecurity.setOnClickListener(v -> {
            startActivity(new Intent(this, SecurityInfoActivity.class));
        });
    }

    private void attemptLogin() {
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        boolean hasError = false;

        if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
            emailInputLayout.setError("Введите корректный email");
            VibratorHelper.vibrate(this, 50);
            hasError = true;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordInputLayout.setError("Пароль должен быть минимум 6 символов");
            VibratorHelper.vibrate(this, 50);
            hasError = true;
        }

        if (!hasError) {
            showLoading(true);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            if (user != null) {
                                String uid = user.getUid();

                                if (CryptoHelper.getLocalPublicKey(uid) == null) {

                                    try {
                                        String newPublicKey = CryptoHelper.generateKeyPair(uid);

                                        FirebaseDatabase.getInstance().getReference("users")
                                                .child(uid).child("publicKey").setValue(newPublicKey);

                                        Toast.makeText(this, "Созданы новые ключи безопасности для этого устройства", Toast.LENGTH_LONG).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                String deviceId = CryptoHelper.getDeviceId(this);
                                FirebaseDatabase.getInstance().getReference("users").child(uid).child("currentDeviceId").setValue(deviceId);

                                saveAccountAndProceed(user, email, password);
                            }
                        } else {
                            VibratorHelper.vibrate(this, 100);
                            Toast.makeText(LoginActivity.this, "Ошибка входа", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showForgotPasswordDialog() {
        VibratorHelper.vibrate(this, 30);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Введите ваш Email");

        String currentEmail = emailEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(currentEmail)) {
            input.setText(currentEmail);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(input);

        new MaterialAlertDialogBuilder(this, R.style.Theme_MapMemories)
                .setTitle("Восстановление пароля")
                .setMessage("Мы отправим ссылку для сброса пароля на вашу почту.")
                .setView(layout)
                .setPositiveButton("Отправить", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
                        Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendResetEmail(email);
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void sendResetEmail(String email) {
        showLoading(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Инструкция отправлена на " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Ошибка отправки. Проверьте адрес.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toggleLoginMode() {
        isQrMode = !isQrMode;

        android.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) loginFieldsContainer.getParent(),
                new android.transition.AutoTransition().setDuration(300));

        if (isQrMode) {
            loginFieldsContainer.setVisibility(View.GONE);
            qrContainer.setVisibility(View.VISIBLE);
            btnSwitchMode.setText("Войти по почте");

            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnSwitchMode.getLayoutParams();
            params.topToBottom = R.id.qrContainer;
            btnSwitchMode.setLayoutParams(params);

            generateLoginQr();
        } else {
            qrContainer.setVisibility(View.GONE);
            loginFieldsContainer.setVisibility(View.VISIBLE);
            btnSwitchMode.setText("Войти по QR-коду");

            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnSwitchMode.getLayoutParams();
            params.topToBottom = R.id.loginFieldsContainer;
            btnSwitchMode.setLayoutParams(params);
        }
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void showLoading(boolean isLoading) {
        int visibility = isLoading ? View.VISIBLE : View.GONE;
        progressBar.setVisibility(visibility);
        overlay.setVisibility(visibility);
        loginButton.setEnabled(!isLoading);
        registerButton.setEnabled(!isLoading);
        forgotPasswordTextView.setEnabled(!isLoading);
    }

    private void handleE2EELogin(FirebaseUser user, String email, String password) {
        String uid = user.getUid();

        if (CryptoHelper.getLocalPublicKey(uid) == null) {
            showE2EEWarningDialog(user, email, password);
        } else {
            saveAccountAndProceed(user, email, password);
        }
    }

    private java.security.PrivateKey tempPrivateKey;

    private void generateLoginQr() {
        try {
            java.security.KeyPair tempPair = CryptoHelper.generateTemporaryKeyPair();
            tempPrivateKey = tempPair.getPrivate();
            String tempPublicKey = Base64.encodeToString(tempPair.getPublic().getEncoded(), Base64.NO_WRAP);

            String ip = CryptoHelper.getLocalIpAddress();
            String qrData = "GHOSTNET_MIGRATE:" + ip + ":8888:" + tempPublicKey;

            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(qrData, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivQrCode.setImageBitmap(bmp);
            startSocketServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startSocketServer() {
        new Thread(() -> {
            try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(8888)) {
                java.net.Socket clientSocket = serverSocket.accept();
                java.io.DataInputStream dis = new java.io.DataInputStream(clientSocket.getInputStream());

                String encryptedAuth = dis.readUTF();
                String encryptedMessages = dis.readUTF();

                String decryptedAuth = CryptoHelper.decryptWithPrivateKey(encryptedAuth, tempPrivateKey);
                String[] authParts = decryptedAuth.split("\\|"); // email|password|uid

                runOnUiThread(() -> finalizeMigration(authParts, encryptedMessages));
                clientSocket.close();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void finalizeMigration(String[] auth, String messagesJson) {
        mAuth.signInWithEmailAndPassword(auth[0], auth[1]).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = task.getResult().getUser();
                if (user != null) {
                    new Thread(() -> {
                        try {
                            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<com.example.mapmemories.database.LocalMessage>>(){}.getType();
                            java.util.List<com.example.mapmemories.database.LocalMessage> list = new com.google.gson.Gson().fromJson(messagesJson, type);

                            com.example.mapmemories.database.AppDatabase.getDatabase(this).localMessageDao().insertMessages(list);

                            runOnUiThread(() -> {
                                String deviceId = CryptoHelper.getDeviceId(this);
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(user.getUid()).child("currentDeviceId").setValue(deviceId);

                                saveAccountAndProceed(user, auth[0], auth[1]);
                                Toast.makeText(this, "Миграция завершена! Восстановлено " + list.size() + " сообщений", Toast.LENGTH_LONG).show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        });
    }


    private void handleImportedData(String json) {
        Toast.makeText(this, "Данные получены! Начинаю импорт...", Toast.LENGTH_LONG).show();
    }

    private void showE2EEWarningDialog(FirebaseUser user, String email, String password) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.Theme_MapMemories)
                .setTitle("Новое устройство")
                .setMessage("Поскольку GhostNet использует сквозное шифрование, ваши старые сообщения на этом устройстве прочитать не получится. \n\nВы согласны создать новые ключи безопасности и начать с чистого листа?")
                .setCancelable(false) // Нельзя закрыть тыком мимо
                .setPositiveButton("Согласен", (dialog, which) -> {
                    try {
                        String newPublicKey = CryptoHelper.generateKeyPair(user.getUid());

                        FirebaseDatabase.getInstance().getReference("users")
                                .child(user.getUid()).child("publicKey").setValue(newPublicKey);

                        saveAccountAndProceed(user, email, password);

                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка безопасности", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    dialog.dismiss();
                })
                .show();
    }

    private void saveAccountAndProceed(FirebaseUser user, String email, String password) {
        String uid = user.getUid();
        String deviceId = CryptoHelper.getDeviceId(this);

        FirebaseDatabase.getInstance().getReference("users")
                .child(uid)
                .child("currentDeviceId")
                .setValue(deviceId);

        LocalAccount newAcc = new LocalAccount(
                uid, email, password,
                user.getDisplayName() != null ? user.getDisplayName() : "Пользователь",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""
        );
        new MultiAccountManager(this).addAccount(newAcc);

        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
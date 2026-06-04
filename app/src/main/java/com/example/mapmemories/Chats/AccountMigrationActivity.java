package com.example.mapmemories.Chats;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.mapmemories.R;
import com.example.mapmemories.systemHelpers.CryptoHelper;
import com.example.mapmemories.systemHelpers.VibratorHelper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;

public class AccountMigrationActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Без камеры сканирование невозможно", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_migration);

        previewView = findViewById(R.id.previewView);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Ошибка запуска камеры", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        BarcodeScanner scanner = BarcodeScanning.getClient();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            @SuppressWarnings("UnsafeOptInUsageError")
            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                scanner.process(inputImage)
                        .addOnSuccessListener(barcodes -> {
                            for (Barcode barcode : barcodes) {
                                String rawValue = barcode.getRawValue();
                                if (rawValue != null && rawValue.startsWith("GHOSTNET_MIGRATE:")) {
                                    handleQrScanned(rawValue);
                                }
                            }
                        })
                        .addOnCompleteListener(task -> image.close());
            } else {
                image.close();
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private boolean isProcessed = false;

    private void handleQrScanned(String data) {
        if (isProcessed) return;
        isProcessed = true;

        String[] parts = data.split(":");
        String targetIp = parts[1];
        String targetPublicKey = parts[3];

        String currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

        com.example.mapmemories.systemHelpers.MultiAccountManager accountManager =
                new com.example.mapmemories.systemHelpers.MultiAccountManager(this);

        com.example.mapmemories.systemHelpers.LocalAccount currentAcc = null;
        for (com.example.mapmemories.systemHelpers.LocalAccount acc : accountManager.getAccounts()) {
            if (acc.uid.equals(currentUid)) {
                currentAcc = acc;
                break;
            }
        }

        if (currentAcc == null) {
            isProcessed = false;
            return;
        }

        final com.example.mapmemories.systemHelpers.LocalAccount finalAcc = currentAcc;

        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("chats")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        java.util.List<com.example.mapmemories.database.LocalMessage> exportList = new java.util.ArrayList<>();
                        for (com.google.firebase.database.DataSnapshot chatSnap : snapshot.getChildren()) {
                            if (chatSnap.getKey() != null && chatSnap.getKey().contains(currentUid)) {
                                for (com.google.firebase.database.DataSnapshot msgSnap : chatSnap.child("messages").getChildren()) {
                                    com.example.mapmemories.Chats.ChatMessage cloudMsg = msgSnap.getValue(com.example.mapmemories.Chats.ChatMessage.class);
                                    if (cloudMsg != null) {
                                        com.example.mapmemories.database.LocalMessage local = new com.example.mapmemories.database.LocalMessage();
                                        local.messageId = cloudMsg.getMessageId();
                                        local.chatId = chatSnap.getKey();
                                        local.senderId = cloudMsg.getSenderId();
                                        local.receiverId = cloudMsg.getReceiverId();
                                        local.timestamp = cloudMsg.getTimestamp();
                                        local.type = cloudMsg.getType();
                                        local.text = CryptoHelper.decrypt(cloudMsg.getTextSender() != null ? cloudMsg.getTextSender() : cloudMsg.getText());
                                        exportList.add(local);
                                    }
                                }
                            }
                        }
                        sendMigrationData(targetIp, targetPublicKey, exportList, finalAcc);
                    }
                    @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
    }

    private void sendMigrationData(String ip, String pubKey, java.util.List<com.example.mapmemories.database.LocalMessage> list, com.example.mapmemories.systemHelpers.LocalAccount acc) {
        new Thread(() -> {
            try {
                java.net.Socket socket = new java.net.Socket(ip, 8888);
                java.io.DataOutputStream dos = new java.io.DataOutputStream(socket.getOutputStream());

                String authData = acc.email + "|" + acc.password + "|" + acc.uid;
                String encryptedAuth = CryptoHelper.encryptWithPublicKey(authData, pubKey);

                dos.writeUTF(encryptedAuth);
                dos.writeUTF(new com.google.gson.Gson().toJson(list));
                dos.flush();
                socket.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Данные переданы!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> isProcessed = false);
            }
        }).start();
    }

    private void sendDataToNewDevice(String targetIp, String jsonData) {
        new Thread(() -> {
            try {
                java.net.Socket socket = new java.net.Socket(targetIp, 8888);
                java.io.DataOutputStream dos = new java.io.DataOutputStream(socket.getOutputStream());

                byte[] data = jsonData.getBytes("UTF-8");
                dos.writeInt(data.length);
                dos.write(data);
                dos.flush();

                socket.close();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Перенос завершен!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка связи: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void exportMessagesFromFirebase(String targetIp) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        com.google.firebase.database.DatabaseReference chatsRef = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("chats");

        chatsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                java.util.List<com.example.mapmemories.Chats.ChatMessage> exportList = new java.util.ArrayList<>();

                for (com.google.firebase.database.DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    if (chatSnapshot.getKey() != null && chatSnapshot.getKey().contains(uid)) {
                        for (com.google.firebase.database.DataSnapshot msgSnap : chatSnapshot.child("messages").getChildren()) {
                            com.example.mapmemories.Chats.ChatMessage msg = msgSnap.getValue(com.example.mapmemories.Chats.ChatMessage.class);
                            if (msg != null) {
                                String decrypted = CryptoHelper.decrypt(msg.getTextSender() != null ? msg.getTextSender() : msg.getText());
                                msg.setText(decrypted);
                                msg.setTextSender(null);
                                exportList.add(msg);
                            }
                        }
                    }
                }

                String jsonData = new com.google.gson.Gson().toJson(exportList);
                sendDataToNewDevice(targetIp, jsonData);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(AccountMigrationActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
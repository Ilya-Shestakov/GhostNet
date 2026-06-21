package com.example.mapmemories.systemHelpers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.mapmemories.Chats.ChatMessage;
import com.example.mapmemories.database.AppDatabase;
import com.example.mapmemories.database.LocalMessage;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumUploadWorker extends Worker {

    public AlbumUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String caption = getInputData().getString("caption");
        String chatId = getInputData().getString("chatId");
        String senderId = getInputData().getString("senderId");
        String receiverId = getInputData().getString("receiverId");
        String targetPubKey = getInputData().getString("targetPubKey");
        String myPubKey = getInputData().getString("myPubKey");
        String[] uriStrings = getInputData().getStringArray("uris");
        String tempMessageId = getInputData().getString("tempId");
        int selfDestructTime = getInputData().getInt("selfDestructTime", 0);

        if (uriStrings == null || uriStrings.length == 0) return Result.failure();

        // Инициализация Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dvbjhturp");
        config.put("api_key", "149561293632228");
        config.put("api_secret", "U8ZmnwKrLwBxmLbBPMM5CxvEYdU");
        Cloudinary cloudinary = new Cloudinary(config);

        List<String> uploadedUrls = new ArrayList<>();
        List<String> types = new ArrayList<>();

        for (String uriStr : uriStrings) {
            try {
                Uri uri = Uri.parse(uriStr);
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(uri);
                String mimeType = getApplicationContext().getContentResolver().getType(uri);
                boolean isVideo = mimeType != null && mimeType.startsWith("video/");
                String resourceType = isVideo ? "video" : "image";
                Map uploadResult = cloudinary.uploader().upload(inputStream,
                        ObjectUtils.asMap("resource_type", resourceType));
                String secureUrl = (String) uploadResult.get("secure_url");
                uploadedUrls.add(secureUrl);
                types.add(isVideo ? "video" : "image");
            } catch (Exception e) {
                Log.e("AlbumUploadWorker", "Upload failed for " + uriStr, e);
                // Можно продолжить или прервать — здесь продолжаем
            }
        }

        if (uploadedUrls.isEmpty()) return Result.failure();

        // Шифруем caption
        String encCaption = null;
        String encCaptionSender = null;
        if (caption != null && !caption.isEmpty()) {
            encCaption = CryptoHelper.encryptForRecipient(caption, targetPubKey);
            encCaptionSender = CryptoHelper.encryptForRecipient(caption, myPubKey);
        }

        // Отправляем сообщение в Firebase
        DatabaseReference chatRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(chatId).child("messages");
        String finalId = chatRef.push().getKey();
        if (finalId != null) {
            ChatMessage albumMsg = new ChatMessage(senderId, receiverId, null, System.currentTimeMillis(), "album");
            albumMsg.setMessageId(finalId);
            albumMsg.setMediaUrls(uploadedUrls);
            albumMsg.setMediaTypes(types);
            if (encCaption != null) {
                albumMsg.setText(encCaption);
                albumMsg.setTextSender(encCaptionSender);
            }
            // reply и прочее можно не добавлять — упрощённо
            chatRef.child(finalId).setValue(albumMsg);
        }

        // Удаляем временное сообщение из локальной БД (если было)
        AppDatabase.getDatabase(getApplicationContext()).localMessageDao().deleteById(tempMessageId);

        return Result.success();
    }
}
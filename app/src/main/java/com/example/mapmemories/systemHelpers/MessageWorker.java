package com.example.mapmemories.systemHelpers;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.cloudinary.Cloudinary;
import com.example.mapmemories.database.AppDatabase;
import com.example.mapmemories.database.LocalMessage;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class MessageWorker extends Worker {

    public MessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            String tempId = getInputData().getString("tempId");
            String uriStr = getInputData().getString("uri");
            String chatId = getInputData().getString("chatId");
            String senderId = getInputData().getString("senderId");
            String receiverId = getInputData().getString("receiverId");
            String targetPubKey = getInputData().getString("targetPubKey");
            String myPubKey = getInputData().getString("myPubKey");
            String caption = getInputData().getString("caption");
            String type = getInputData().getString("type");

            long timestamp = getInputData().getLong("timestamp", System.currentTimeMillis());
            boolean isOneTime = getInputData().getBoolean("isOneTime", false);

            if (caption == null) caption = "";

            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dvbjhturp");
            config.put("api_key", "149561293632228");
            config.put("api_secret", "U8ZmnwKrLwBxmLbBPMM5CxvEYdU");
            Cloudinary cloudinary = new Cloudinary(config);

            java.io.InputStream is = getApplicationContext().getContentResolver().openInputStream(Uri.parse(uriStr));
            Map uploadResult = cloudinary.uploader().upload(is, com.cloudinary.utils.ObjectUtils.emptyMap());
            String secureUrl = (String) uploadResult.get("secure_url");

            String encForReceiver = CryptoHelper.encryptForRecipient(caption, targetPubKey);
            String encForSender = CryptoHelper.encryptForRecipient(caption, myPubKey);


            int selfDestructTime = getInputData().getInt("selfDestructTime", 0);

            String realMsgId = FirebaseDatabase.getInstance().getReference("chats")
                    .child(chatId).child("messages").push().getKey();

            java.net.URL url = new java.net.URL(secureUrl);
            String fileName = "img_" + realMsgId + ".jpg";
            String localPath = MediaCacheManager.saveToInternal(getApplicationContext(), url.openStream(), fileName);

            if (realMsgId != null) {


                Map<String, Object> fbMessage = new HashMap<>();
                fbMessage.put("messageId", realMsgId);
                fbMessage.put("senderId", senderId);
                fbMessage.put("receiverId", receiverId);
                fbMessage.put("text", encForReceiver);
                fbMessage.put("textSender", encForSender);
                fbMessage.put("imageUrl", secureUrl);
                fbMessage.put("timestamp", timestamp);
                fbMessage.put("type", type);
                fbMessage.put("read", false);
                fbMessage.put("selfDestructTime", selfDestructTime);
                fbMessage.put("isOneTime", isOneTime);

                FirebaseDatabase.getInstance().getReference("chats")
                        .child(chatId).child("messages").child(realMsgId).setValue(fbMessage);



                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());

                if (tempId != null) {
                    db.localMessageDao().deleteById(tempId);
                }

                LocalMessage finalLocal = new LocalMessage();
                finalLocal.messageId = realMsgId;
                finalLocal.chatId = chatId;
                finalLocal.senderId = senderId;
                finalLocal.receiverId = receiverId;
                finalLocal.text = caption;
                finalLocal.imageUrl = uriStr;
                finalLocal.remoteUrl = secureUrl;
                finalLocal.timestamp = timestamp;
                finalLocal.type = type;
                finalLocal.isPending = false;
                finalLocal.selfDestructTime = selfDestructTime;
                finalLocal.isOneTime = isOneTime;

                db.localMessageDao().insertSingleMessage(finalLocal);
            }

            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}
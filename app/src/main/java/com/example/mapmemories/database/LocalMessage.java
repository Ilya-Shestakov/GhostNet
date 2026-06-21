package com.example.mapmemories.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class LocalMessage {
    @PrimaryKey
    @NonNull
    public String messageId;
    public String chatId;
    public String senderId;
    public String receiverId;
    public String text;
    public String imageUrl;
    public String remoteUrl;
    public long timestamp;
    public String type;
    public boolean isPending;
    public int selfDestructTime;
    public boolean isOneTime;

    private String mediaUrls;
    private String mediaTypes;


    public LocalMessage() {}

    public String getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getMediaTypes() {
        return mediaTypes;
    }

    public void setMediaTypes(String mediaTypes) {
        this.mediaTypes = mediaTypes;
    }
}
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

    public LocalMessage() {}
}
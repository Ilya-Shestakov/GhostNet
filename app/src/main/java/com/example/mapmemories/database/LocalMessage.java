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
    public long timestamp;
    public String type;

    public LocalMessage() {}
}
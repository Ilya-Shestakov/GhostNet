package com.example.mapmemories.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface LocalMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<LocalMessage> messages);

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    List<LocalMessage> getMessagesForChat(String chatId);
}
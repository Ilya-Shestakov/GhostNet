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

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    void deleteMessagesByChatId(String chatId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSingleMessage(LocalMessage message);

    @Query("DELETE FROM messages WHERE messageId = :id")
    void deleteById(String id);

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    List<LocalMessage> getMessagesWindow(String chatId, int limit, int offset);

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    int getMessageCount(String chatId);

    @Query("SELECT COUNT(*) FROM messages")
    int getMessageCountAll();

    @Query("DELETE FROM messages")
    void deleteAllMessages();

}
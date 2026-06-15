package com.example.mapmemories.systemHelpers;

import android.content.Context;
import android.content.SharedPreferences;

public class DraftManager {
    private static final String PREF_NAME = "chat_drafts";
    private final SharedPreferences prefs;

    public DraftManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveDraft(String chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            prefs.edit().remove(chatId).apply();
        } else {
            prefs.edit().putString(chatId, text).apply();
        }
    }

    public String getDraft(String chatId) {
        return prefs.getString(chatId, null);
    }

    public void clearDraft(String chatId) {
        prefs.edit().remove(chatId).apply();
    }

    public void saveImageDraft(String chatId, String uri) {
        if (uri == null) {
            prefs.edit().remove(chatId + "_img").apply();
        } else {
            prefs.edit().putString(chatId + "_img", uri).apply();
        }
    }

    public String getImageDraft(String chatId) {
        return prefs.getString(chatId + "_img", null);
    }
}
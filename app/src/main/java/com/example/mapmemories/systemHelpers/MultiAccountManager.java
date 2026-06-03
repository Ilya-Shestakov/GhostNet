package com.example.mapmemories.systemHelpers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MultiAccountManager {
    private static final String PREF_NAME = "secure_accounts";
    private SharedPreferences securePrefs;
    private Gson gson = new Gson();

    public MultiAccountManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            securePrefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void addAccount(LocalAccount account) {
        List<LocalAccount> accounts = getAccounts();
        // Удаляем старую запись, если она была (чтобы обновить данные)
        accounts.removeIf(a -> a.uid.equals(account.uid));
        accounts.add(account);
        securePrefs.edit().putString("account_list", gson.toJson(accounts)).apply();
    }

    public List<LocalAccount> getAccounts() {
        if (securePrefs == null) return new ArrayList<>();
        String json = securePrefs.getString("account_list", "[]");
        Type listType = new TypeToken<ArrayList<LocalAccount>>(){}.getType(); // Исправлено
        return gson.fromJson(json, listType);
    }

    public void removeAccount(String uid) {
        List<LocalAccount> accounts = getAccounts();
        accounts.removeIf(a -> a.uid.equals(uid));
        securePrefs.edit().putString("account_list", gson.toJson(accounts)).apply();
    }
}
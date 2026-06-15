package com.example.mapmemories.systemHelpers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MultiAccountManager {
    private static final String PREF_NAME = "secure_accounts_v2";
    private SharedPreferences securePrefs;
    private final Gson gson = new Gson();

    public MultiAccountManager(Context context) {
        try {
            initPrefs(context);
        } catch (Exception e) {
            try {
                context.deleteSharedPreferences(PREF_NAME);
                initPrefs(context);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private void initPrefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        securePrefs = EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public void addAccount(LocalAccount account) {
        if (securePrefs == null) return; // Защита от вылета

        List<LocalAccount> accounts = getAccounts();
        accounts.removeIf(a -> a.uid.equals(account.uid));
        accounts.add(account);
        securePrefs.edit().putString("account_list", gson.toJson(accounts)).apply();
    }

    public List<LocalAccount> getAccounts() {
        if (securePrefs == null) return new ArrayList<>();

        String json = securePrefs.getString("account_list", "[]");
        Type listType = new TypeToken<ArrayList<LocalAccount>>(){}.getType();
        return gson.fromJson(json, listType);
    }

    public void removeAccount(String uid) {
        if (securePrefs == null) return;

        List<LocalAccount> accounts = getAccounts();
        accounts.removeIf(a -> a.uid.equals(uid));
        securePrefs.edit().putString("account_list", gson.toJson(accounts)).apply();
    }

    public void updateAccountInfo(String uid, String newUsername, String newAvatarUrl) {
        if (securePrefs == null) return;

        List<LocalAccount> accounts = getAccounts();
        boolean changed = false;
        for (LocalAccount acc : accounts) {
            if (acc.uid.equals(uid)) {
                acc.username = newUsername;
                acc.avatarUrl = newAvatarUrl;
                changed = true;
                break;
            }
        }
        if (changed) {
            securePrefs.edit().putString("account_list", gson.toJson(accounts)).apply();
        }
    }
}
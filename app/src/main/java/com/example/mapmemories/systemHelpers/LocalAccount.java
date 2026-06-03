package com.example.mapmemories.systemHelpers;

public class LocalAccount {
    public String uid;
    public String email;
    public String password; // Храним для авто-входа при переключении
    public String username;
    public String avatarUrl;

    public LocalAccount(String uid, String email, String password, String username, String avatarUrl) {
        this.uid = uid;
        this.email = email;
        this.password = password;
        this.username = username;
        this.avatarUrl = avatarUrl;
    }
}
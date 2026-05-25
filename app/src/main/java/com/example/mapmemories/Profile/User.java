package com.example.mapmemories.Profile;

import com.google.firebase.database.Exclude;

public class User {
    private String id;
    private String username;
    private String profileImageUrl;
    private String about;

    public User() {
    }

    public User(String username, String profileImageUrl, String about) {
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.about = about;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }
}
package com.example.mapmemories.Chats;

import java.io.Serializable;
import java.util.List;

public class ChatMessage implements Serializable{

    private static final long serialVersionUID = 1L;

    private String messageId;
    private String senderId;
    private String receiverId;
    private String text;
    private String imageUrl;
    private String postId;
    private long timestamp;
    private String type;
    private String deletedBy;
    private boolean read;
    private String textSender;

    private String replyMessageId;
    private String replySenderId;
    private String replyText;
    private String reaction;

    private List<String> mediaUrls;
    private List<String> mediaTypes;

    private int selfDestructTime;

    private boolean isOneTime;
    public void setOneTime(boolean oneTime) { isOneTime = oneTime; }
    private String remoteUrl;

    public boolean isOneTime() { return isOneTime; }


    public ChatMessage() {
    }

    public ChatMessage(String senderId, String receiverId, String text, long timestamp, String type) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
        this.type = type;
        this.read = false;
    }

    public ChatMessage(String senderId, String receiverId, String imageUrl, String text, long timestamp, String type) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.imageUrl = imageUrl;
        this.text = text;
        this.timestamp = timestamp;
        this.type = type;
        this.read = false;
    }

    public ChatMessage(String senderId, String receiverId, String postId, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.postId = postId;
        this.timestamp = timestamp;
        this.type = "post";
        this.read = false;
    }
    public int getSelfDestructTime() { return selfDestructTime; }






    private transient String decryptedTextCache;
    private transient String decryptedFileNameCache;
    private transient String decryptedReplyTextCache;

    public String getDecryptedTextCache() {
        return decryptedTextCache;
    }

    public void setDecryptedTextCache(String decryptedTextCache) {
        this.decryptedTextCache = decryptedTextCache;
    }

    public String getDecryptedFileNameCache() {
        return decryptedFileNameCache;
    }

    public void setDecryptedFileNameCache(String decryptedFileNameCache) {
        this.decryptedFileNameCache = decryptedFileNameCache;
    }

    public String getDecryptedReplyTextCache() {
        return decryptedReplyTextCache;
    }

    public void setDecryptedReplyTextCache(String decryptedReplyTextCache) {
        this.decryptedReplyTextCache = decryptedReplyTextCache;
    }





    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }

    public List<String> getMediaTypes() { return mediaTypes; }
    public void setMediaTypes(List<String> mediaTypes) { this.mediaTypes = mediaTypes; }

    public void setSelfDestructTime(int selfDestructTime) { this.selfDestructTime = selfDestructTime; }

    public String getMessageId() { return messageId; }

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderId() { return senderId; }

    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getTextSender() { return textSender; }
    public void setTextSender(String textSender) { this.textSender = textSender; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getReplyMessageId() { return replyMessageId; }
    public void setReplyMessageId(String replyMessageId) { this.replyMessageId = replyMessageId; }

    public String getReplySenderId() { return replySenderId; }
    public void setReplySenderId(String replySenderId) { this.replySenderId = replySenderId; }

    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }



}
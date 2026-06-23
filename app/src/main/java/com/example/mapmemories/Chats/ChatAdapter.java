package com.example.mapmemories.Chats;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.Target;
import com.example.mapmemories.Post.ViewPostDetailsActivity;
import com.example.mapmemories.R;
import com.example.mapmemories.systemHelpers.AudioPlayerManager;
import com.example.mapmemories.systemHelpers.CryptoHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/* |-----------------------------------------------------------------------|
 * |                            АДАПТЕР ЧАТА                               |
 * |-----------------------------------------------------------------------| */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private Context context;
    private List<ChatMessage> messages;
    private String currentUserId;
    private String targetUserName = "Собеседник";
    private ChatActionListener actionListener;
    private Map<String, PostCacheData> postCache = new HashMap<>();

    private boolean dismissBlocked = false;
    private final Handler dismissHandler = new Handler();

    private String highlightedMessageId = null;
    private final Handler highlightHandler = new Handler();

    private Set<String> selectedMessageIds = new HashSet<>();
    private Set<String> uploadingMessageIds = new HashSet<>();
    private boolean isSelectionMode = false;

    private PopupWindow currentMenu;

    private Map<String, Integer> voiceDurations = new HashMap<>();

    public interface ChatActionListener {

        void onSecretImageClicked(View thumbView, ChatMessage message);
        void onEditMessage(ChatMessage message);
        void onDeleteMessage(ChatMessage message, boolean forEveryone);
        void onReplyMessage(ChatMessage message);
        void onQuoteClicked(String messageId);
        void onMessageHighlighted(String messageId);
        void onPinMessage(ChatMessage message);
        void onReactionSelected(ChatMessage message, String reaction);
        void onSelectionChanged(int selectedCount);
        void onCancelUpload(String messageId);
        void onImageClicked(View thumbView, ChatMessage message);
    }

    public ChatAdapter(Context context, List<ChatMessage> messages, ChatActionListener listener) {
        this.context = context;
        this.messages = messages;
        this.actionListener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void setTargetUserName(String name) {
        if (name != null && !name.isEmpty()) {
            this.targetUserName = name;
            notifyDataSetChanged();
        }
    }

    public void addUploadingMessage(String id) { uploadingMessageIds.add(id); }
    public void removeUploadingMessage(String id) { uploadingMessageIds.remove(id); }

    public void highlightMessage(String messageId) {
        this.highlightedMessageId = messageId;
        notifyDataSetChanged();
        highlightHandler.postDelayed(() -> {
            if (messageId.equals(highlightedMessageId)) {
                highlightedMessageId = null;
                notifyDataSetChanged();
            }
        }, 2000);
        if (actionListener != null) actionListener.onMessageHighlighted(messageId);
    }

    public void clearSelection() {
        Set<String> previousIds = new HashSet<>(selectedMessageIds);
        selectedMessageIds.clear();
        isSelectionMode = false;
        for (String id : previousIds) {
            int pos = getPositionById(id);
            if (pos != -1) notifyItemChanged(pos);
        }
        if (actionListener != null) actionListener.onSelectionChanged(0);
    }

    public Set<String> getSelectedMessageIds() { return selectedMessageIds; }

    private void toggleSelection(String messageId, int position) {
        if (selectedMessageIds.contains(messageId)) selectedMessageIds.remove(messageId);
        else selectedMessageIds.add(messageId);
        if (selectedMessageIds.isEmpty()) isSelectionMode = false;
        notifyItemChanged(position);
        if (actionListener != null) actionListener.onSelectionChanged(selectedMessageIds.size());
    }

    private int getPositionById(String id) {
        if (id == null) return -1;
        for (int i = 0; i < messages.size(); i++) {
            if (id.equals(messages.get(i).getMessageId())) return i;
        }
        return -1;
    }

    /* |-----------------------------------------------------------------------|
     * |                       КОЛЛБЭКИ АУДИОПЛЕЕРА                            |
     * |-----------------------------------------------------------------------| */
    public void updateAudioState(String messageId, boolean isPlaying) {
        int pos = getPositionById(messageId);
        if (pos != -1) notifyItemChanged(pos, "PLAY_STATE_ONLY");
    }

    public void updateAudioProgress(String messageId, int current, int max) {
        int pos = getPositionById(messageId);
        if (pos != -1) notifyItemChanged(pos, new AudioProgress(current, max));
    }

    /* |-----------------------------------------------------------------------|
     * |                           БИНДИНГ UI                                  |
     * |-----------------------------------------------------------------------| */

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_chat_post, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            ChatMessage message = messages.get(position);
            for (Object payload : payloads) {
                if (payload instanceof String && payload.equals("PLAY_STATE_ONLY")) {
                    boolean isPlaying = AudioPlayerManager.getInstance().isPlaying() && message.getMessageId().equals(AudioPlayerManager.getInstance().getCurrentPlayingId());
                    holder.btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                } else if (payload instanceof AudioProgress) {
                    AudioProgress p = (AudioProgress) payload;

                    voiceDurations.put(message.getMessageId(), p.max);
                    holder.tvVoiceDuration.setText(formatVoiceTime(p.max));
                }
            }
        }
    }

    private String getDecryptedContent(ChatMessage message) {

        String cached = message.getDecryptedTextCache();
        if (cached != null) {
            return cached;
        }

        // Кэша нет — выполняем расшифровку
        boolean isMine = currentUserId != null && currentUserId.equals(message.getSenderId());
        String data;
        if (isMine) {
            data = (message.getTextSender() != null) ? message.getTextSender() : message.getText();
        } else {
            data = message.getText();
        }

        if (data == null) return "";
        if (!data.startsWith("ENC_V3:")) return data; // Не зашифровано — возвращаем как есть

        String decrypted = CryptoHelper.decrypt(data);
        message.setDecryptedTextCache(decrypted); // Сохраняем в кэш
        return decrypted;
    }

    private String getDecryptedReplyText(ChatMessage message) {
        String cached = message.getDecryptedReplyTextCache();
        if (cached != null) return cached;

        String replyText = message.getReplyText();
        if (replyText == null) return "";

        if (!replyText.startsWith("ENC_V3:")) {
            message.setDecryptedReplyTextCache(replyText);
            return replyText;
        }

        String decrypted = CryptoHelper.decrypt(replyText);
        message.setDecryptedReplyTextCache(decrypted);
        return decrypted;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        ChatMessage message = messages.get(position);
        String senderId = message.getSenderId();
        boolean isMine = currentUserId != null && currentUserId.equals(senderId);
        boolean isHighlighted = message.getMessageId() != null && message.getMessageId().equals(highlightedMessageId);
        boolean isSelected = message.getMessageId() != null && selectedMessageIds.contains(message.getMessageId());
        boolean isUploading = message.getMessageId() != null && uploadingMessageIds.contains(message.getMessageId());

        holder.albumLayout.setVisibility(View.GONE);

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.contentLayout.getLayoutParams();

        if (message.getMessageId() != null && message.getMessageId().startsWith("TEMP_")) {
            holder.itemView.setAlpha(0.5f);
        } else {
            holder.itemView.setAlpha(1.0f);
        }

        if (isSelected) {
            holder.itemView.setBackgroundColor(Color.parseColor("#1AE27950"));
            holder.contentLayout.setScaleX(0.92f); holder.contentLayout.setScaleY(0.92f); holder.contentLayout.setAlpha(0.8f);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.contentLayout.setScaleX(1f); holder.contentLayout.setScaleY(1f); holder.contentLayout.setAlpha(1f);
        }

        if (isMine) {
            params.horizontalBias = 1.0f;
            holder.contentLayout.setBackgroundResource(R.drawable.bg_msg_mine);
            holder.tvTextMessage.setTextColor(Color.WHITE);
            holder.timeText.setTextColor(Color.WHITE); holder.timeText.setAlpha(0.7f);
            holder.tvQuotedSender.setTextColor(Color.WHITE);
            holder.tvQuotedText.setTextColor(Color.WHITE); holder.tvQuotedText.setAlpha(0.8f);
            holder.itemView.findViewById(R.id.quoteLine).setBackgroundColor(Color.WHITE);

            if (message.getMessageId() != null && message.getMessageId().startsWith("TEMP_")) {
                holder.imageProgressBar.setVisibility(View.VISIBLE);
                holder.contentLayout.setAlpha(0.6f);
            } else {
                holder.ivReadStatus.setVisibility(View.VISIBLE);
                holder.ivReadStatus.setImageResource(message.isRead() ? R.drawable.ic_check_double : R.drawable.ic_check);
                holder.ivReadStatus.setColorFilter(message.isRead() ? Color.parseColor("#4FC3F7") : Color.WHITE);
            }
        } else {
            params.horizontalBias = 0.0f;
            holder.contentLayout.setBackgroundResource(R.drawable.bg_msg_theirs);
            int otherColor = ContextCompat.getColor(context, R.color.chat_other_text);
            holder.tvTextMessage.setTextColor(otherColor);
            holder.timeText.setTextColor(otherColor); holder.timeText.setAlpha(0.6f);
            holder.tvQuotedSender.setTextColor(ContextCompat.getColor(context, R.color.accent_coral));
            holder.tvQuotedText.setTextColor(otherColor); holder.tvQuotedText.setAlpha(0.8f);
            holder.itemView.findViewById(R.id.quoteLine).setBackgroundColor(ContextCompat.getColor(context, R.color.accent_coral));
            holder.ivReadStatus.setVisibility(View.GONE);
        }
        holder.contentLayout.setLayoutParams(params);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.timeText.setText(sdf.format(message.getTimestamp()));

        holder.imageContainer.setVisibility(View.GONE);
        holder.voiceLayout.setVisibility(View.GONE);
        holder.fileLayout.setVisibility(View.GONE);
        holder.postLayout.setVisibility(View.GONE);
        holder.replyQuotedLayout.setVisibility(View.GONE);
        holder.highlightOverlay.setVisibility(View.GONE);
        holder.secretOverlay.setVisibility(View.GONE);
        holder.ivTimerBadge.setVisibility(View.GONE);
        holder.imageProgressBar.setVisibility(View.GONE);
        holder.tvReactionBadge.setVisibility(View.GONE);
        holder.chatAttachedImage.setImageDrawable(null);
        holder.tvTextMessage.setVisibility(View.GONE);

        if (holder.tvTextMessage.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.tvTextMessage.getLayoutParams();
            lp.topMargin = 0;
            holder.tvTextMessage.setLayoutParams(lp);
        }


        if (isHighlighted) {
            holder.highlightOverlay.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(300);
            holder.highlightOverlay.startAnimation(fadeIn);
        }

        if (message.getReplyMessageId() != null && !message.getReplyMessageId().isEmpty()) {
            holder.replyQuotedLayout.setVisibility(View.VISIBLE);
            holder.tvQuotedSender.setText(message.getReplySenderId().equals(currentUserId) ? "Вы" : targetUserName);

            String replyText = message.getReplyText();
            if (replyText == null || replyText.isEmpty()) {

                if ("voice".equals(message.getType())) replyText = "🎤 Голосовое сообщение";

                else if ("file".equals(message.getType())) replyText = "📁 Документ";

                else if ("image".equals(message.getType())) replyText = "📷 Фотография";

                else replyText = "Вложение";

            }

            holder.tvQuotedText.setText(getDecryptedReplyText(message));
            holder.replyQuotedLayout.setOnClickListener(v -> { if (actionListener != null) actionListener.onQuoteClicked(message.getReplyMessageId()); });
        }

        holder.albumLayout.setVisibility(View.GONE);

        /* |-----------------------------------------------------------------------|
         * |                           ТИПЫ СООБЩЕНИЙ                              |
         * |-----------------------------------------------------------------------| */

        if ("text".equals(message.getType())) {
            holder.tvTextMessage.setVisibility(View.VISIBLE);
            holder.tvTextMessage.setText(getDecryptedContent(message));
        } else if ("image".equals(message.getType())) {
            holder.imageContainer.setVisibility(View.VISIBLE);

            // Всегда показываем фото (никаких isOneTime)
            String path = (message.getRemoteUrl() != null) ? message.getRemoteUrl() : message.getImageUrl();
            com.bumptech.glide.Glide.with(context)
                    .load(path)
                    .into(holder.chatAttachedImage);

            // Подпись (caption)
            String caption = getDecryptedContent(message);
            if (caption != null && !caption.isEmpty()) {
                holder.tvTextMessage.setVisibility(View.VISIBLE);
                holder.tvTextMessage.setText(caption);
            } else {
                holder.tvTextMessage.setVisibility(View.GONE);
            }

            // Клик — всегда обычный просмотр (через MediaBrowserActivity)
            holder.imageContainer.setOnClickListener(v -> {
                actionListener.onImageClicked(holder.chatAttachedImage, message);
            });
        } else if ("file".equals(message.getType())) {
            holder.fileLayout.setVisibility(View.VISIBLE);

            String fileName = getDecryptedContent(message);
            holder.tvFileName.setText(fileName);

            if (isUploading) {
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_upload);
                holder.fileLayout.setOnClickListener(v -> actionListener.onCancelUpload(message.getMessageId()));
            } else {
                holder.ivFileIcon.setImageResource(R.drawable.ic_file);

                if (isMine) holder.tvFileName.setTextColor(Color.WHITE);
                else holder.tvFileName.setTextColor(ContextCompat.getColor(context, R.color.chat_other_text));

                holder.fileLayout.setOnClickListener(v -> {
                    if (isSelectionMode) {
                        toggleSelection(message.getMessageId(), holder.getAdapterPosition());
                    } else {
                        downloadFile(message.getImageUrl(), fileName);
                    }
                });
            }
        } else if ("voice".equals(message.getType())) {
            holder.voiceLayout.setVisibility(View.VISIBLE);

            int tintColor = isMine ? Color.WHITE : Color.parseColor("#3390EC");
            int bgTintColor = isMine ? Color.parseColor("#33FFFFFF") : Color.parseColor("#1A3390EC");

            holder.btnPlayPause.setColorFilter(tintColor);
            holder.btnPlayPause.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgTintColor));
            holder.tvVoiceDuration.setTextColor(tintColor);

            if (isUploading) {
                holder.tvVoiceDuration.setText("Отправка...");
                holder.btnPlayPause.setImageResource(android.R.drawable.ic_menu_upload);
                holder.btnPlayPause.setOnClickListener(v -> actionListener.onCancelUpload(message.getMessageId()));

                holder.tvVoiceDuration.setOnClickListener(null);
                holder.voiceLayout.setOnClickListener(null);
            } else {
                boolean isThisPlaying = message.getMessageId().equals(AudioPlayerManager.getInstance().getCurrentPlayingId());

                holder.btnPlayPause.setImageResource(isThisPlaying && AudioPlayerManager.getInstance().isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

                if (voiceDurations.containsKey(message.getMessageId())) {
                    holder.tvVoiceDuration.setText(formatVoiceTime(voiceDurations.get(message.getMessageId())));
                } else {
                    holder.tvVoiceDuration.setText("▶ Аудио");
                }

                View.OnClickListener playPauseListener = v -> {
                    if (isSelectionMode) {
                        toggleSelection(message.getMessageId(), holder.getAdapterPosition());
                        return;
                    }

                    if (context instanceof ChatActivity) {
                        ((ChatActivity) context).isAudioManuallyPaused = AudioPlayerManager.getInstance().isPlaying();
                    }

                    if (isThisPlaying) {
                        if (AudioPlayerManager.getInstance().isPlaying()) {
                            AudioPlayerManager.getInstance().pause();
                            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        } else {
                            AudioPlayerManager.getInstance().play(message.getMessageId(), message.getImageUrl());
                            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                        }
                    } else {
                        AudioPlayerManager.getInstance().play(message.getMessageId(), message.getImageUrl());
                        holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    }
                };

                holder.btnPlayPause.setOnClickListener(playPauseListener);
                holder.tvVoiceDuration.setOnClickListener(playPauseListener);
                holder.voiceLayout.setOnClickListener(playPauseListener);
            }

        } else if ("album".equals(message.getType())) {
            List<String> urls = message.getMediaUrls();
            if (urls == null || urls.isEmpty()) {
                holder.albumLayout.setVisibility(View.GONE);
                holder.tvTextMessage.setVisibility(View.GONE); // Скрываем текст, если нет данных
                return;
            }

            int size = urls.size();
            holder.albumLayout.setVisibility(View.VISIBLE);

            // --- ВОТ ЭТОТ БЛОК НУЖНО ДОБАВИТЬ ДЛЯ ТЕКСТА ---
            // Внутри else if ("album".equals(message.getType()))
            String caption = getDecryptedContent(message);
            if (caption != null && !caption.isEmpty()) {
                holder.tvTextMessage.setVisibility(View.VISIBLE);
                holder.tvTextMessage.setText(caption);

                // ДОБАВЛЯЕМ ОТСТУП ДЛЯ ТЕКСТА ПОД ФОТО
                if (holder.tvTextMessage.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.tvTextMessage.getLayoutParams();
                    lp.topMargin = (int) (8 * context.getResources().getDisplayMetrics().density);
                    holder.tvTextMessage.setLayoutParams(lp);
                }
            } else {
                holder.tvTextMessage.setVisibility(View.GONE);
            }
            // ----------------------------------------------

            // Сброс состояния анимаций
            holder.albumViewPager.setAlpha(1f);
            holder.albumViewPager.setScaleX(1f);
            holder.albumViewPager.setScaleY(1f);
            holder.albumGridRecycler.setVisibility(View.GONE);
            holder.albumGridRecycler.setAlpha(0f);
            holder.tvAlbumCounter.setAlpha(1f);
            holder.tvAlbumCounter.setText("1 / " + size);

            // Настройка ViewPager (как было раньше)
            AlbumPagerAdapter pagerAdapter = new AlbumPagerAdapter(context, urls, new AlbumPagerAdapter.OnAlbumClickListener() {
                @Override
                public void onClick(View v, int realIdx) {
                    if (context instanceof ChatActivity) {
                        ((ChatActivity) context).openGalleryWithFlattening(v, message, realIdx);
                    }
                }

                @Override
                public void onLongClick(View v, int realIdx) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    holder.albumGridRecycler.setVisibility(View.VISIBLE);
                    holder.albumViewPager.animate().alpha(0).scaleX(0.8f).scaleY(0.8f).setDuration(300).start();
                    holder.albumGridRecycler.animate().alpha(1).scaleX(1f).scaleY(1f).setDuration(300).start();
                    holder.tvAlbumCounter.animate().alpha(0).setDuration(200).start();
                }
            });
            holder.albumViewPager.setAdapter(pagerAdapter);

            int startPos = size * 500;
            holder.albumViewPager.setCurrentItem(startPos, false);

            holder.albumViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    int current = (position % size) + 1;
                    holder.tvAlbumCounter.setText(current + " / " + size);
                }
            });

            // Настройка Сетки (Grid)
            AlbumGridAdapter gridAdapter = new AlbumGridAdapter(context, urls, gridPos -> {
                int currentViewPagerPos = holder.albumViewPager.getCurrentItem();
                int currentRealIdx = currentViewPagerPos % size;
                int diff = gridPos - currentRealIdx;
                holder.albumViewPager.setCurrentItem(currentViewPagerPos + diff, false);

                holder.albumGridRecycler.animate().alpha(0f).scaleX(1.2f).scaleY(1.2f).setDuration(300)
                        .withEndAction(() -> holder.albumGridRecycler.setVisibility(View.GONE)).start();
                holder.albumViewPager.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start();
                holder.tvAlbumCounter.animate().alpha(1f).setDuration(200).start();
            });
            holder.albumGridRecycler.setLayoutManager(new GridLayoutManager(context, 3));
            holder.albumGridRecycler.setAdapter(gridAdapter);

            // Блокировка свайпа
            View viewPagerChild = holder.albumViewPager.getChildAt(0);
            if (viewPagerChild != null) {
                viewPagerChild.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        if (context instanceof ChatActivity) {
                            ((ChatActivity) context).isTouchInsideAlbum = true;
                        }
                    }
                    return false;
                });
            }
        }

        if (message.getReaction() != null && !message.getReaction().isEmpty()) {
            holder.tvReactionBadge.setVisibility(View.VISIBLE);
            holder.tvReactionBadge.setText(message.getReaction());

            holder.tvReactionBadge.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onReactionSelected(message, null);
            });
        } else {
            holder.tvReactionBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        holder.contentLayout.setAlpha(0.7f);
                        break;
                    case MotionEvent.ACTION_UP:
                        holder.contentLayout.setAlpha(1f);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        holder.contentLayout.setAlpha(1f);
                        break;
                }
                return false;
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode && message.getMessageId() != null && !isUploading) {
                isSelectionMode = true;
                toggleSelection(message.getMessageId(), holder.getAdapterPosition());
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isUploading) return;
            if (isSelectionMode && message.getMessageId() != null) toggleSelection(message.getMessageId(), holder.getAdapterPosition());
            else showTelegramStyleMenu(message, isMine, holder.contentLayout);
        });
    }

    private void downloadFile(String url, String fileName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("Скачивание файла...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(context, "Скачивание началось...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Ошибка при скачивании", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetViewHolders(ViewHolder holder) {
        holder.albumLayout.setVisibility(View.GONE);
        holder.tvTextMessage.setVisibility(View.GONE);
        holder.postLayout.setVisibility(View.GONE);
        holder.imageContainer.setVisibility(View.GONE);
        holder.imageProgressBar.setVisibility(View.GONE);
        holder.secretOverlay.setVisibility(View.GONE);
        holder.voiceLayout.setVisibility(View.GONE);
        holder.replyQuotedLayout.setVisibility(View.GONE);
        holder.highlightOverlay.setVisibility(View.GONE);
        holder.ivTimerBadge.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
        holder.tvReactionBadge.setOnClickListener(null);
        holder.chatAttachedImage.setTag(null);
        holder.fileLayout.setVisibility(View.GONE);
        holder.itemView.setAlpha(1.0f);

    }

    private String formatVoiceTime(int totalMs) {
        int totSec = totalMs / 1000;
        return String.format(Locale.getDefault(), "%d:%02d", totSec / 60, totSec % 60);
    }

    /* |-----------------------------------------------------------------------|
     * |                           ВСПЛЫВАЮЩЕЕ МЕНЮ                            |
     * |-----------------------------------------------------------------------| */
    private void showTelegramStyleMenu(ChatMessage message, boolean isMine, View anchorView) {

        // Если заблокировано открытие нового меню – просто выходим
        if (dismissBlocked) return;

        // Если уже есть открытое меню – закрываем его и блокируем открытие на 150 мс
        if (currentMenu != null && currentMenu.isShowing()) {
            currentMenu.dismiss();
            currentMenu = null;
            dismissBlocked = true;
            dismissHandler.postDelayed(() -> dismissBlocked = false, 150);

            Object tag = anchorView.getTag();
            if (tag instanceof String && tag.equals(message.getMessageId())) {
                return;
            }
            return;
        }

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int popupWidth = (int) (screenWidth * 0.7f);

        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_telegram_menu, null);
        popupView.setFocusable(false);
        popupView.setFocusableInTouchMode(false);

        PopupWindow popupWindow = new PopupWindow(popupView, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(24f);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

        // Запоминаем ID сообщения через обычный тег
        anchorView.setTag(message.getMessageId());

        popupWindow.setOnDismissListener(() -> {
            anchorView.setAlpha(1f);
            anchorView.setTag(null);
            if (currentMenu == popupWindow) {
                currentMenu = null;
            }
            dismissBlocked = true;
            dismissHandler.removeCallbacksAndMessages(null);
            dismissHandler.postDelayed(() -> dismissBlocked = false, 150);
        });


        LinearLayout reactionContainer = popupView.findViewById(R.id.reactionContainer);
        reactionContainer.setPadding(16, 16, 16, 16);
        String[] emojis = {"👍", "👎", "❤️", "🔥", "🥰", "👏", "😂", "😮", "😢", "😡", "🎉", "💩"};

        for (String emoji : emojis) {
            TextView tvEmoji = new TextView(context);
            tvEmoji.setText(emoji);
            tvEmoji.setTextSize(28f);
            tvEmoji.setPadding(24, 16, 24, 16);
            tvEmoji.setGravity(Gravity.CENTER);

            if (emoji.equals(message.getReaction())) {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(Color.parseColor("#33E27950"));
                tvEmoji.setBackground(bg);
            }

            tvEmoji.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start(); break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: v.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(new OvershootInterpolator()).start(); break;
                }
                return false;
            });

            tvEmoji.setOnClickListener(v -> {
                popupWindow.dismiss();
                String newReaction = emoji.equals(message.getReaction()) ? null : emoji;
                if (actionListener != null) actionListener.onReactionSelected(message, newReaction);
            });
            reactionContainer.addView(tvEmoji);
        }

        LinearLayout actionsContainer = popupView.findViewById(R.id.actionsContainer);
        actionsContainer.removeAllViews();

        boolean isPost = "post".equals(message.getType());
        boolean isImage = "image".equals(message.getType());
        boolean isVoice = "voice".equals(message.getType());

        addPopupMenuItem(actionsContainer, "↩️ Ответить", v -> { popupWindow.dismiss(); if (actionListener != null) actionListener.onReplyMessage(message); });

        if ("text".equals(message.getType())) {
            addPopupMenuItem(actionsContainer, "📋 Копировать", v -> {
                popupWindow.dismiss();
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Message", message.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show();
            });
        }

        addPopupMenuItem(actionsContainer, "📌 Закрепить", v -> { popupWindow.dismiss(); if (actionListener != null) actionListener.onPinMessage(message); });

        if (isPost) {
            addPopupMenuItem(actionsContainer, "🗺️ Открыть воспоминание", v -> {
                popupWindow.dismiss();
                Intent intent = new Intent(context, ViewPostDetailsActivity.class);
                intent.putExtra("postId", message.getPostId());
                context.startActivity(intent);
            });
        }

        if (isMine && !isPost && !isImage && !isVoice) {
            addPopupMenuItem(actionsContainer, "✏️ Изменить", v -> { popupWindow.dismiss(); if (actionListener != null) actionListener.onEditMessage(message); });
        }

        addPopupMenuItemCustomColor(actionsContainer, "🗑️ Удалить сообщение", Color.parseColor("#FF5252"), v -> {
            popupWindow.dismiss();
            if (actionListener != null) actionListener.onDeleteMessage(message, isMine);
        });

        popupView.measure(View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
        int popupHeight = popupView.getMeasuredHeight();
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int margin = 32;
        int x = isMine ? Math.max(margin, location[0] + anchorView.getWidth() - popupWidth) : Math.min(location[0], screenWidth - popupWidth - margin);
        int y = location[1] - popupHeight - 20;
        if (y < 100) y = location[1] + anchorView.getHeight() + 20;

        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);

        currentMenu = popupWindow;

    }

    private void addPopupMenuItem(LinearLayout parent, String text, View.OnClickListener listener) {
        addPopupMenuItemCustomColor(parent, text, ContextCompat.getColor(context, R.color.text_primary), listener);
    }

    private void addPopupMenuItemCustomColor(LinearLayout parent, String text, int color, View.OnClickListener listener) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(16f);
        textView.setTextColor(color);
        textView.setPadding(40, 32, 40, 32);

        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        textView.setBackgroundResource(outValue.resourceId);

        textView.setOnClickListener(listener);
        parent.addView(textView);
    }

    private void loadPostDataOptimized(String postId, ViewHolder holder) {
        if (postCache.containsKey(postId)) {
            holder.postTitle.setText(postCache.get(postId).title);
            Glide.with(context).load(postCache.get(postId).imageUrl).into(holder.postImage);
            return;
        }
        holder.postTitle.setText("Загрузка...");
        FirebaseDatabase.getInstance().getReference("posts").child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String imageUrl = snapshot.child("mediaUrls").exists() ? snapshot.child("mediaUrls").child("0").getValue(String.class) : snapshot.child("mediaUrl").getValue(String.class);
                    postCache.put(postId, new PostCacheData(title != null ? title : "Без названия", imageUrl));
                    holder.postTitle.setText(title);
                    if (imageUrl != null) Glide.with(context).load(imageUrl).into(holder.postImage);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override public int getItemCount() { return messages.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout contentLayout;
        LinearLayout postLayout, replyQuotedLayout, voiceLayout, fileLayout;
        TextView tvTextMessage, postTitle, timeText, tvQuotedSender, tvQuotedText, tvReactionBadge, tvVoiceDuration, tvFileName;
        ImageView postImage, chatAttachedImage, ivReadStatus, ivFileIcon, ivTimerBadge;
        View highlightOverlay, secretOverlay;
        MaterialCardView imageContainer;
        ProgressBar imageProgressBar;
        ImageButton btnPlayPause;
        RecyclerView albumGridRecycler;
        ViewPager2 albumViewPager;
        RecyclerView albumThumbRecycler;

        MaterialCardView albumLayout;
        TextView albumCaption;

        TextView tvAlbumCounter;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contentLayout = itemView.findViewById(R.id.contentLayout);
            postLayout = itemView.findViewById(R.id.postLayout);
            tvTextMessage = itemView.findViewById(R.id.tvTextMessage);
            postImage = itemView.findViewById(R.id.postImage);
            postTitle = itemView.findViewById(R.id.postTitle);
            timeText = itemView.findViewById(R.id.msgTime);
            chatAttachedImage = itemView.findViewById(R.id.chatAttachedImage);
            replyQuotedLayout = itemView.findViewById(R.id.replyQuotedLayout);
            tvQuotedSender = itemView.findViewById(R.id.tvQuotedSender);
            tvQuotedText = itemView.findViewById(R.id.tvQuotedText);
            highlightOverlay = itemView.findViewById(R.id.highlightOverlay);
            tvReactionBadge = itemView.findViewById(R.id.tvReactionBadge);
            ivReadStatus = itemView.findViewById(R.id.ivReadStatus);
            imageContainer = itemView.findViewById(R.id.imageContainer);
            voiceLayout = itemView.findViewById(R.id.voiceLayout);
            btnPlayPause = itemView.findViewById(R.id.btnPlayPause);
            tvVoiceDuration = itemView.findViewById(R.id.tvVoiceDuration);
            fileLayout = itemView.findViewById(R.id.fileLayout);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            ivTimerBadge = itemView.findViewById(R.id.ivTimerBadge);
            imageProgressBar = itemView.findViewById(R.id.imageProgressBar);
            secretOverlay = itemView.findViewById(R.id.secretOverlay);

            albumGridRecycler = itemView.findViewById(R.id.albumGridRecycler);

            tvAlbumCounter = itemView.findViewById(R.id.tvAlbumCounter);

            albumLayout = itemView.findViewById(R.id.albumLayout);
            albumCaption = itemView.findViewById(R.id.albumCaption);
            albumViewPager = itemView.findViewById(R.id.albumViewPager);

        }
    }

    private static class PostCacheData {
        String title, imageUrl;
        PostCacheData(String title, String imageUrl) { this.title = title; this.imageUrl = imageUrl; }
    }

    private static class AudioProgress {
        int current, max;
        AudioProgress(int current, int max) { this.current = current; this.max = max; }
    }

}
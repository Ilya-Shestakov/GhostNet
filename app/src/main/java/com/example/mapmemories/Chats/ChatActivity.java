package com.example.mapmemories.Chats;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.NotificationManager;
import android.content.Context;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.Cloudinary;
import com.example.mapmemories.Profile.User;
import com.example.mapmemories.Profile.UserProfileActivity;
import com.example.mapmemories.R;
import com.example.mapmemories.Settings.Setting;
import com.example.mapmemories.database.AppDatabase;
import com.example.mapmemories.database.LocalMessage;
import com.example.mapmemories.database.LocalMessageDao;
import com.example.mapmemories.systemHelpers.AudioPlayerManager;
import com.example.mapmemories.systemHelpers.CryptoHelper;
import com.example.mapmemories.systemHelpers.DraftManager;
import com.example.mapmemories.systemHelpers.MessageWorker;
import com.example.mapmemories.systemHelpers.TimeFormatter;
import com.example.mapmemories.systemHelpers.VibratorHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    /* |-----------------------------------------------------------------------|
     * |                           ПЕРЕМЕННЫЕ                              |
     * |-----------------------------------------------------------------------| */

    public static String currentChatUserId = null;

    private float startX, startY;
    private String targetUserId, currentUserId, chatId;
    private String editingMessageId = null;
    private Uri selectedImageUri = null;
    private ChatMessage replyingToMessage = null;

    private String myPublicKey;

    private FrameLayout galleryContainer;
    private androidx.viewpager2.widget.ViewPager2 galleryViewPager;
    private TextView tvGalleryCounter;
    private ImageButton btnGalleryClose, btnGalleryMenu;
    private List<ChatMessage> galleryImages = new ArrayList<>();
    private GalleryAdapter galleryAdapter;
    private boolean isGalleryOpen = false;


    private LinearLayout userInfoContainer, emptyChatContainer, pinnedMessageContainer;
    private ImageView ivChatAvatar;
    private TextView tvChatUsername, tvChatStatus, btnSayHello, tvPinnedText;
    private ImageButton btnUnpin;

    private View rootLayout;
    private boolean isSwipingToClose = false;
    private boolean canSwipeBack = false;
    private int screenWidth;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private FloatingActionButton fabScrollDown;

    private LinearLayout textInputContainer, recordingContainer;
    private ImageButton btnAttach, btnSend, btnRemoveImage;
    private EditText etMessageInput;
    private ConstraintLayout imagePreviewContainer, replyPreviewContainer;
    private ImageView ivPreviewImage;
    private TextView tvReplySender, tvReplyText;
    private ImageButton btnCloseReply;

    private DraftManager draftManager;

    public boolean isAudioManuallyPaused = false;

    private LinearLayout selectionToolbar;
    private TextView tvSelectedCount;
    private ImageButton btnCloseSelection, btnSelectionReact, btnSelectionCopy, btnSelectionForward, btnSelectionDelete;

    private DatabaseReference chatRef, targetUserRef, myStatusRef, pinnedRef, typingRef;
    private ValueEventListener statusListener, pinnedListener, typingListener;
    private ChildEventListener messagesListener;
    private Handler typingHandler = new Handler();
    private Runnable typingRunnable;

    private String targetPublicKey;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> pickFileLauncher;
    private ActivityResultLauncher<String> requestMicLauncher;

    private Cloudinary cloudinary;
    private Map<String, java.util.concurrent.Future<?>> uploadTasks = new HashMap<>();
    private boolean isTargetUserHidden = false;

    private Bitmap blurredScreenshot = null;

    //ГС
    private ImageButton btnRecordVoice;
    private LinearLayout lockOverlay;
    private TextView tvRecordTime, tvSlideToCancel, btnCancelVoiceLock;
    private View redDot;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private boolean isRecordingLocked = false;
    private long recordStartTime;
    private Handler timerHandler = new Handler();

    private int touchSlop;

    private View globalPlayerContainer;
    private View gpExpandedControls;
    private ImageView gpIcon;
    private TextView gpCurrentTime, gpTotalTime;
    private ImageButton gpClose, gpDownload;
    private android.widget.SeekBar gpSeekBar;
    private boolean isGlobalPlayerExpanded = false;

    ImageView btnEditMedia;

    private int currentTimerValue = 0;

    private int windowStartOffset = 0;
    private int windowSize = 40;

    private boolean isLoadingMore = false;
    private int previousFirstVisibleItem = RecyclerView.NO_POSITION;

    private boolean isChatBlocked = false;

    /* |-----------------------------------------------------------------------|
     * |                           ЖИЗНЕННЫЙ ЦИКЛ                          |
     * |-----------------------------------------------------------------------| */

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences preferences = newBase.getSharedPreferences(Setting.PREFS_NAME, Context.MODE_PRIVATE);
        float scale = preferences.getFloat("text_scale", 1.0f);
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.fontScale = scale;
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        View rootLayout = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            String myAlias = "GhostNet_Key_" + FirebaseAuth.getInstance().getUid();
            if (ks.containsAlias(myAlias)) {
                android.util.Log.d("CRYPTO_CHECK", "Ключ для текущего аккаунта на месте!");
            } else {
                android.util.Log.e("CRYPTO_CHECK", "КЛЮЧА НЕТ для этого аккаунта!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        targetUserId = getIntent().getStringExtra("targetUserId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        SharedPreferences unlockPrefs = getSharedPreferences("chat_unlock", MODE_PRIVATE);
        long lastUnlock = unlockPrefs.getLong(chatId, 0);
        if (System.currentTimeMillis() - lastUnlock < 3 * 60 * 1000) {
            isChatBlocked = false;
            loadMessagesOptimized();
            loadPinnedMessage();
            loadMyPublicKey();
            return;
        }

        if (TextUtils.isEmpty(targetUserId)) { finish(); return; }
        chatId = currentUserId.compareTo(targetUserId) < 0 ? currentUserId + "_" + targetUserId : targetUserId + "_" + currentUserId;

        if (chatId != null && currentUserId != null) {
            FirebaseDatabase.getInstance().getReference("chats")
                    .child(chatId).child("blockedBy").child(currentUserId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            isChatBlocked = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue());
                            if (isChatBlocked) {
                                SharedPreferences unlockPrefs = getSharedPreferences("chat_unlock", MODE_PRIVATE);
                                long lastUnlock = unlockPrefs.getLong(chatId, 0);
                                if (System.currentTimeMillis() - lastUnlock < 3 * 60 * 1000) {
                                    onChatUnlocked();
                                } else {
                                    showChatUnlockScreen();
                                }
                            } else {
                                onChatUnlocked();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loadMessagesOptimized();
                            loadPinnedMessage();
                            loadMyPublicKey();
                        }
                    });
        } else {
            // Если нет chatId или currentUserId, просто загружаем
            loadMessagesOptimized();
            loadPinnedMessage();
            loadMyPublicKey();
        }


        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        pinnedRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("pinnedMessageId");
        typingRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("typingStates");
        targetUserRef = FirebaseDatabase.getInstance().getReference("users").child(targetUserId);
        myStatusRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("status");

        screenWidth = getResources().getDisplayMetrics().widthPixels;

        touchSlop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();


        initCloudinary();
        initViews();

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(300); // Длительность анимации появления
        chatRecyclerView.setItemAnimator(animator);

        setupLaunchers();
        setupVoiceRecording();

        loadTargetUserData();

        clearNotification();



    }

    private final ActivityResultLauncher<Intent> mediaEditorLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    currentTimerValue = result.getData().getIntExtra("timerValue", 0);


                    VibratorHelper.vibrate(this, 30);
                }
            });

    /* |-----------------------------------------------------------------------|
     * |                           ИНИЦИАЛИЗАЦИЯ                           |
     * |-----------------------------------------------------------------------| */

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dvbjhturp");
        config.put("api_key", "149561293632228");
        config.put("api_secret", "U8ZmnwKrLwBxmLbBPMM5CxvEYdU");
        cloudinary = new Cloudinary(config);
    }

    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        userInfoContainer = findViewById(R.id.userInfoContainer);
        ivChatAvatar = findViewById(R.id.ivChatAvatar);
        tvChatUsername = findViewById(R.id.tvChatUsername);
        tvChatStatus = findViewById(R.id.tvChatStatus);
        emptyChatContainer = findViewById(R.id.emptyChatContainer);
        btnSayHello = findViewById(R.id.btnSayHello);
        pinnedMessageContainer = findViewById(R.id.pinnedMessageContainer);
        tvPinnedText = findViewById(R.id.tvPinnedText);
        btnUnpin = findViewById(R.id.btnUnpin);


        selectionToolbar = findViewById(R.id.selectionToolbar);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnCloseSelection = findViewById(R.id.btnCloseSelection);
        btnSelectionReact = findViewById(R.id.btnSelectionReact);
        btnSelectionCopy = findViewById(R.id.btnSelectionCopy);
        btnSelectionForward = findViewById(R.id.btnSelectionForward);
        btnSelectionDelete = findViewById(R.id.btnSelectionDelete);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        chatRecyclerView.setItemAnimator(null);

        fabScrollDown = findViewById(R.id.fabScrollDown);

        textInputContainer = findViewById(R.id.textInputContainer);
        recordingContainer = findViewById(R.id.recordingContainer);

        btnAttach = findViewById(R.id.btnAttach);
        btnSend = findViewById(R.id.btnSend);
        etMessageInput = findViewById(R.id.etMessageInput);

        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        ivPreviewImage = findViewById(R.id.ivPreviewImage);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);

        replyPreviewContainer = findViewById(R.id.replyPreviewContainer);
        tvReplySender = findViewById(R.id.tvReplySender);
        tvReplyText = findViewById(R.id.tvReplyText);
        btnCloseReply = findViewById(R.id.btnCloseReply);

        btnRecordVoice = findViewById(R.id.btnRecordVoice);
        lockOverlay = findViewById(R.id.lockOverlay);
        tvRecordTime = findViewById(R.id.tvRecordTime);
        tvSlideToCancel = findViewById(R.id.tvSlideToCancel);
        btnCancelVoiceLock = findViewById(R.id.btnCancelVoiceLock);
        redDot = findViewById(R.id.redDot);

        globalPlayerContainer = findViewById(R.id.globalPlayerContainer);
        gpExpandedControls = findViewById(R.id.gpExpandedControls);
        gpIcon = findViewById(R.id.gpIcon);
        gpCurrentTime = findViewById(R.id.gpCurrentTime);
        gpTotalTime = findViewById(R.id.gpTotalTime);
        gpClose = findViewById(R.id.gpClose);
        gpDownload = findViewById(R.id.gpDownload);
        gpSeekBar = findViewById(R.id.gpSeekBar);

        galleryContainer = findViewById(R.id.galleryContainer);
        galleryViewPager = findViewById(R.id.galleryViewPager);
        tvGalleryCounter = findViewById(R.id.tvGalleryCounter);
        btnGalleryClose = findViewById(R.id.btnGalleryClose);
        btnGalleryMenu = findViewById(R.id.btnGalleryMenu);

        btnGalleryClose.setOnClickListener(v -> closeGallery());

        btnEditMedia = findViewById(R.id.btnEditMedia);

        FrameLayout loadingOverlay = findViewById(R.id.loadingOverlayContainer);
        loadingOverlay.setVisibility(View.VISIBLE);

        chatRecyclerView.setAlpha(0f);


        draftManager = new DraftManager(this);

        String savedDraft = draftManager.getDraft(chatId);
        if (savedDraft != null) {
            etMessageInput.setText(savedDraft);
        }

        String savedImgDraft = draftManager.getImageDraft(chatId);
        if (savedImgDraft != null) {
            selectedImageUri = Uri.parse(savedImgDraft);
            imagePreviewContainer.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedImageUri).into(ivPreviewImage);
            updateInputUI();
        }

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(200);
        animator.setRemoveDuration(200);
        animator.setMoveDuration(200);
        chatRecyclerView.setItemAnimator(animator);

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            java.security.cert.Certificate cert = ks.getCertificate("GhostNet_RSA_Key");
            if (cert != null) {
                myPublicKey = android.util.Base64.encodeToString(cert.getPublicKey().getEncoded(), android.util.Base64.NO_WRAP);
            }
        } catch (Exception e) { e.printStackTrace(); }

        setupGlobalPlayer();

        //setupSelectionActions();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        userInfoContainer.setOnClickListener(v -> {
            VibratorHelper.vibrate(this, 30);
            Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
            intent.putExtra("targetUserId", targetUserId);
            startActivity(intent);
        });

        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            draftManager.saveImageDraft(chatId, null); // Удаляем из черновика
            imagePreviewContainer.setVisibility(View.GONE);
            updateInputUI();
        });

        btnCloseReply.setOnClickListener(v -> closeReplyPreview());
        btnSayHello.setOnClickListener(v -> sendTextMessage("👋 Привет!"));
        btnUnpin.setOnClickListener(v -> pinnedRef.removeValue());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, createChatActionListener());
        chatRecyclerView.setAdapter(chatAdapter);

        if (chatAdapter != null) {
            setupSelectionActions();
        }

        AudioPlayerManager.getInstance().setCallback(new AudioPlayerManager.PlayerCallback() {
            @Override
            public void onStateChanged(String messageId, boolean isPlaying) {
                chatAdapter.updateAudioState(messageId, isPlaying);

                if (isPlaying) {
                    if (globalPlayerContainer.getVisibility() == View.GONE) {
                        globalPlayerContainer.setVisibility(View.VISIBLE);
                        globalPlayerContainer.setTranslationY(-100f);
                        globalPlayerContainer.setAlpha(0f);
                        globalPlayerContainer.animate().translationY(0f).alpha(1f).setDuration(300).setInterpolator(new OvershootInterpolator()).start();
                    }
                    gpIcon.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    gpIcon.setImageResource(android.R.drawable.ic_media_play);

                    if (!isAudioManuallyPaused) {
                        boolean hasNext = playNextVoiceMessage(messageId);
                        if (!hasNext) {
                            closeGlobalPlayer();
                        }
                    }
                }
            }

            @Override
            public void onProgressUpdate(String messageId, int currentPos, int maxDuration) {
                gpSeekBar.setMax(maxDuration);
                gpSeekBar.setProgress(currentPos);
                gpCurrentTime.setText(formatTimeStr(currentPos));
                gpTotalTime.setText(formatTimeStr(maxDuration));
            }

            @Override public void onError(String error) { Toast.makeText(ChatActivity.this, error, Toast.LENGTH_SHORT).show(); }
        });

        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            boolean fabVisible = false;
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                boolean atBottom = !recyclerView.canScrollVertically(1);
                if (atBottom && fabVisible) {
                    fabScrollDown.setVisibility(View.GONE);
                    fabVisible = false;
                } else if (!atBottom && !fabVisible) {
                    fabScrollDown.setVisibility(View.VISIBLE);
                    fabVisible = true;
                }
            }
        });

        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                // Если пользователь докрутил близко к началу окна — догружаем старые сообщения
                if (firstVisibleItem <= 2 && !isLoadingMore && windowStartOffset > 0) {
                    loadOlderMessages();
                }
            }
        });

        fabScrollDown.setOnClickListener(v -> chatRecyclerView.smoothScrollToPosition(messageList.size() - 1));

        MessageSwipeController swipeController = new MessageSwipeController(this, position -> {
            ChatMessage message = messageList.get(position);

            new Handler(getMainLooper()).postDelayed(() -> {
                setupReplyPreview(message, false);
            }, 150);
        });
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeController);
        itemTouchHelper.attachToRecyclerView(chatRecyclerView);

//        chatRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
//            if (bottom < oldBottom && !messageList.isEmpty()) {
//                chatRecyclerView.postDelayed(() -> chatRecyclerView.scrollToPosition(messageList.size() - 1), 100);
//            }
//        });

        etMessageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateInputUI();
                typingRef.child(currentUserId).setValue("typing");
                typingHandler.removeCallbacks(typingRunnable);
                typingRunnable = () -> typingRef.child(currentUserId).setValue("false");
                typingHandler.postDelayed(typingRunnable, 2000);
                draftManager.saveDraft(chatId, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAttach.setOnClickListener(this::showAttachmentMenuPopup);

        btnSend.setOnClickListener(v -> {
            String text = etMessageInput.getText().toString().trim();
            if (selectedImageUri != null) {
                uploadImageToCloudinaryAndSend(selectedImageUri, text);
                selectedImageUri = null;
                imagePreviewContainer.setVisibility(View.GONE);
                etMessageInput.setText("");
                updateInputUI();
            } else if (!text.isEmpty()) {
                if (editingMessageId != null) {
                    String editedId = editingMessageId;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("text", CryptoHelper.encryptForRecipient(text, targetPublicKey));

                    if (replyingToMessage != null) {
                        updates.put("replyMessageId", replyingToMessage.getMessageId());
                        updates.put("replySenderId", replyingToMessage.getSenderId());
                        updates.put("replyText", decryptMessageText(replyingToMessage));
                    } else {
                        updates.put("replyMessageId", null);
                        updates.put("replySenderId", null);
                        updates.put("replyText", null);
                    }

                    chatRef.child(editedId).updateChildren(updates);
                    chatRef.orderByChild("replyMessageId").equalTo(editedId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot child : snapshot.getChildren()) child.getRef().child("replyText").setValue(text);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                    closeReplyPreview();
                } else {
                    etMessageInput.setText("");
                    sendTextMessage(text);
                }
                updateInputUI();
            }
        });


    }

    private void showChatUnlockScreen() {
        SharedPreferences lockPrefs = getSharedPreferences("chat_lock", MODE_PRIVATE);
        String passwordHash = lockPrefs.getString("password_hash", null);

        if (passwordHash == null) {
            // На всякий случай: нет пароля – просто загружаем чат
            loadMessagesOptimized();
            loadPinnedMessage();
            loadMyPublicKey();
            return;
        }

        boolean useBiometric = lockPrefs.getBoolean("use_biometric", false);

        if (useBiometric) {
            showBiometricPrompt();
        } else {
            showPasswordInput();
        }
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                onChatUnlocked();
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(ChatActivity.this, "Не удалось распознать", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                // Если ошибка (например, нет зарегистрированных отпечатков), показываем пароль
                showPasswordInput();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Разблокировка чата")
                .setSubtitle("Подтвердите личность для доступа к переписке")
                .setNegativeButtonText("Ввести пароль")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }



    private void syncKeysIfNeeded() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            String myAlias = "GhostNet_Key_" + currentUserId;

            if (!ks.containsAlias(myAlias)) {
                myPublicKey = CryptoHelper.generateKeyPair(currentUserId);
                FirebaseDatabase.getInstance().getReference("users").child(currentUserId)
                        .child("publicKey").setValue(myPublicKey);
                return;
            }

            java.security.cert.Certificate cert = ks.getCertificate(myAlias);
            String currentLocalPubKey = android.util.Base64.encodeToString(cert.getPublicKey().getEncoded(), android.util.Base64.NO_WRAP);

            if (myPublicKey != null && !myPublicKey.equals(currentLocalPubKey)) {
                android.util.Log.w("GHOST_CRYPTO", "Ключи не совпадают! Обновляю базу...");
                myPublicKey = currentLocalPubKey;
                FirebaseDatabase.getInstance().getReference("users").child(currentUserId)
                        .child("publicKey").setValue(myPublicKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeGlobalPlayer() {
        AudioPlayerManager.getInstance().stop();
        if (globalPlayerContainer.getVisibility() == View.VISIBLE) {
            globalPlayerContainer.animate()
                    .translationY(-100f)
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        globalPlayerContainer.setVisibility(View.GONE);
                        isGlobalPlayerExpanded = false;
                        gpExpandedControls.setVisibility(View.GONE);
                    }).start();
        }
    }

    private void showPasswordInput() {
        // Создаём диалог с размытым фоном
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) {
            activityRootView.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR));
        }

        // Надуваем наш layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_password_unlock, null);

        // Точки
        View[] dots = new View[6];
        dots[0] = dialogView.findViewById(R.id.dot1);
        dots[1] = dialogView.findViewById(R.id.dot2);
        dots[2] = dialogView.findViewById(R.id.dot3);
        dots[3] = dialogView.findViewById(R.id.dot4);
        dots[4] = dialogView.findViewById(R.id.dot5);
        dots[5] = dialogView.findViewById(R.id.dot6);

        // Строим клавиатуру
        GridLayout grid = dialogView.findViewById(R.id.keyboardGrid);
        StringBuilder passwordBuilder = new StringBuilder();
        String[] keys = {"1","2","3","4","5","6","7","8","9","","0","⌫"};

        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            TextView btn = new TextView(this);
            btn.setText(key);
            btn.setTextSize(24f);
            btn.setTextColor(getColor(R.color.text_primary));
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundResource(R.drawable.key_bg);
            btn.setClickable(true);
            btn.setFocusable(true);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(80);
            params.height = dpToPx(80);
            params.setMargins(12, 12, 12, 12);
            params.rowSpec = GridLayout.spec(i / 3);
            params.columnSpec = GridLayout.spec(i % 3);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                if (key.equals("⌫")) {
                    if (passwordBuilder.length() > 0) {
                        dots[passwordBuilder.length() - 1].setBackgroundResource(R.drawable.dot_inactive);
                        passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);
                    }
                    return;
                }
                if (key.isEmpty() || passwordBuilder.length() >= 6) return;

                passwordBuilder.append(key);
                dots[passwordBuilder.length() - 1].setBackgroundResource(R.drawable.dot_active);

                if (passwordBuilder.length() == 6) {
                    // Проверяем пароль
                    if (checkPassword(passwordBuilder.toString())) {
                        // Верно – закрываем диалог и разблокируем чат
                        dialog.dismiss();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) {
                            activityRootView.setRenderEffect(null);
                        }
                        onChatUnlocked();
                    } else {
                        // Неверно – сбрасываем
                        Toast.makeText(ChatActivity.this, "Неверный пароль", Toast.LENGTH_SHORT).show();
                        passwordBuilder.setLength(0);
                        for (View dot : dots) dot.setBackgroundResource(R.drawable.dot_inactive);
                    }
                }
            });

            grid.addView(btn);
        }

        dialog.setContentView(dialogView);
        dialog.setCancelable(false);
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private boolean checkPassword(String input) {
        SharedPreferences lockPrefs = getSharedPreferences("chat_lock", MODE_PRIVATE);
        String storedHash = lockPrefs.getString("password_hash", "");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(storedHash);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    private void onChatUnlocked() {
        SharedPreferences prefs = getSharedPreferences("chat_unlock", MODE_PRIVATE);
        prefs.edit().putLong(chatId, System.currentTimeMillis()).apply();
        loadMessagesOptimized();
        loadPinnedMessage();
        loadMyPublicKey();
    }

    private void loadOlderMessages() {
        if (isLoadingMore || windowStartOffset <= 0) return;
        isLoadingMore = true;

        // Запоминаем первый видимый элемент и его смещение до вставки
        LinearLayoutManager layoutManager = (LinearLayoutManager) chatRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            int firstVisiblePos = layoutManager.findFirstVisibleItemPosition();
            View firstView = layoutManager.findViewByPosition(firstVisiblePos);
            previousFirstVisibleItem = firstVisiblePos;
            if (firstView != null) {
                previousFirstVisibleItem = firstVisiblePos;
            }
        }

        new Thread(() -> {
            LocalMessageDao dao = AppDatabase.getDatabase(this).localMessageDao();

            // Вычисляем offset для предыдущей порции
            int loadCount = windowSize;
            int newOffset = Math.max(0, windowStartOffset - loadCount);
            int adjustedLoadCount = windowStartOffset - newOffset; // если упёрлись в 0

            List<LocalMessage> olderMessages = dao.getMessagesWindow(chatId, adjustedLoadCount, newOffset);
            if (olderMessages == null || olderMessages.isEmpty()) {
                runOnUiThread(() -> isLoadingMore = false);
                return;
            }

            // Расшифровываем в фоне
            List<ChatMessage> newMessages = new ArrayList<>();
            for (LocalMessage local : olderMessages) {
                ChatMessage msg = new ChatMessage();
                msg.setMessageId(local.messageId);
                msg.setSenderId(local.senderId);
                msg.setReceiverId(local.receiverId);
                msg.setTimestamp(local.timestamp);
                msg.setType(local.type);
                msg.setText(local.text);
                msg.setImageUrl(local.imageUrl);
                msg.setRemoteUrl(local.remoteUrl);
                msg.setSelfDestructTime(local.selfDestructTime);
                msg.setOneTime(local.isOneTime);
                decryptMessageFields(msg);
                newMessages.add(msg);
            }

            runOnUiThread(() -> {
                // Сохраняем позицию для восстановления скролла
                int oldFirstVisible = layoutManager.findFirstVisibleItemPosition();
                View oldFirstView = layoutManager.findViewByPosition(oldFirstVisible);
                int offsetY = (oldFirstView != null) ? oldFirstView.getTop() : 0;

                // Вставляем в начало списка
                messageList.addAll(0, newMessages);
                windowStartOffset = newOffset;

                // Обновляем адаптер
                List<ChatMessage> sorted = new ArrayList<>(messageList);
                Collections.sort(sorted, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                updateMessageList(sorted);

                // Восстанавливаем позицию скролла
                int newFirstVisible = oldFirstVisible + newMessages.size();
                layoutManager.scrollToPositionWithOffset(newFirstVisible, offsetY);

                isLoadingMore = false;
            });
        }).start();
    }

    private void setupLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                currentTimerValue = 0;

                imagePreviewContainer.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).into(ivPreviewImage);
                updateInputUI();
            }
        });
        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        String fileName = getFileNameFromUri(uri);
                        uploadFileToCloudinaryAndSend(uri, fileName);
                    }
                }
        );
        ivPreviewImage.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                Intent intent = new Intent(this, MediaPreviewActivity.class);
                intent.putExtra("imageUri", selectedImageUri.toString());
                // Передаем текущие настройки, чтобы они не сбросились
                mediaEditorLauncher.launch(intent);
            }
        });
        requestMicLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) Toast.makeText(this, "Для отправки ГС нужен микрофон", Toast.LENGTH_SHORT).show();
        });
    }

    @android.annotation.SuppressLint("Range")
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result != null ? result : "Неизвестный файл";
    }

    private void uploadFileToCloudinaryAndSend(Uri fileUri, String fileName) {
        String tempMessageId = "temp_file_" + System.currentTimeMillis();

        ChatMessage tempMsg = new ChatMessage(
                currentUserId, targetUserId,
                fileUri.toString(), CryptoHelper.encryptForRecipient(fileName, targetPublicKey),
                System.currentTimeMillis(),
                "file");
        tempMsg.setMessageId(tempMessageId);

        String myRealKey = CryptoHelper.getLocalPublicKey(currentUserId);
        tempMsg.setTextSender(CryptoHelper.encryptForRecipient(fileName, myRealKey));

        attachReplyDataToMessage(tempMsg);

        messageList.add(tempMsg);
        chatAdapter.addUploadingMessage(tempMessageId);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        java.util.concurrent.Future<?> uploadTask = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                Map<String, Object> options = new HashMap<>();
                options.put("resource_type", "auto");
                Map uploadResult = cloudinary.uploader().upload(inputStream, options);
                String secureUrl = (String) uploadResult.get("secure_url");

                String encForReceiver = CryptoHelper.encryptForRecipient(fileName, targetPublicKey);
                String encForSender = CryptoHelper.encryptForRecipient(fileName, myRealKey);

                runOnUiThread(() -> {
                    uploadTasks.remove(tempMessageId);
                    removeTempMessageLocally(tempMessageId);
                    if (!isFinishing() && !isDestroyed()) {
                        String messageId = chatRef.push().getKey();
                        if (messageId != null) {
                            ChatMessage message = new ChatMessage(currentUserId, targetUserId, secureUrl, CryptoHelper.encryptForRecipient(fileName, targetPublicKey), System.currentTimeMillis(), "file");
                            message.setTextSender(encForSender);
                            message.setMessageId(messageId);
                            attachReplyDataToMessage(message);
                            chatRef.child(messageId).setValue(message);
                            closeReplyPreview();
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    uploadTasks.remove(tempMessageId);
                    removeTempMessageLocally(tempMessageId);
                    Toast.makeText(ChatActivity.this, "Ошибка отправки файла", Toast.LENGTH_SHORT).show();
                });
            }
        });
        uploadTasks.put(tempMessageId, uploadTask);
    }

    private void setupGlobalPlayer() {
        globalPlayerContainer.setOnClickListener(v -> {

            android.transition.TransitionManager.beginDelayedTransition((ViewGroup) globalPlayerContainer.getParent(), new android.transition.AutoTransition().setDuration(250));

            isGlobalPlayerExpanded = !isGlobalPlayerExpanded;
            gpExpandedControls.setVisibility(isGlobalPlayerExpanded ? View.VISIBLE : View.GONE);
        });

        gpIcon.setOnClickListener(v -> {
            if (AudioPlayerManager.getInstance().isPlaying()) {
                isAudioManuallyPaused = true;
                AudioPlayerManager.getInstance().pause();
                gpIcon.setImageResource(android.R.drawable.ic_media_play);
            } else {
                isAudioManuallyPaused = false;
                String currentPlayingId = AudioPlayerManager.getInstance().getCurrentPlayingId();
                if (currentPlayingId != null) {
                    for (ChatMessage msg : messageList) {
                        if (msg.getMessageId().equals(currentPlayingId)) {
                            AudioPlayerManager.getInstance().play(currentPlayingId, msg.getImageUrl());
                            break;
                        }
                    }
                }
            }
        });

        gpClose.setOnClickListener(v -> {
            AudioPlayerManager.getInstance().stop();
            globalPlayerContainer.animate().translationY(-100f).alpha(0f).setDuration(200).withEndAction(() -> {
                globalPlayerContainer.setVisibility(View.GONE);
                isGlobalPlayerExpanded = false;
                gpExpandedControls.setVisibility(View.GONE);
            }).start();
        });

        gpSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) AudioPlayerManager.getInstance().seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        gpDownload.setOnClickListener(v -> Toast.makeText(this, "Скачивание аудио...", Toast.LENGTH_SHORT).show());
    }

    private String formatTimeStr(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    /* |-----------------------------------------------------------------------|
     * |                           ЛОГИКА АДАПТЕРА                         |
     * |-----------------------------------------------------------------------| */

    private void updateMessageList(List<ChatMessage> newSortedMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return messageList.size();
            }

            @Override
            public int getNewListSize() {
                return newSortedMessages.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = messageList.get(oldItemPosition).getMessageId();
                String newId = newSortedMessages.get(newItemPosition).getMessageId();
                return oldId != null && oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ChatMessage oldMsg = messageList.get(oldItemPosition);
                ChatMessage newMsg = newSortedMessages.get(newItemPosition);
                // Сравниваем только то, что влияет на отображение
                return oldMsg.isRead() == newMsg.isRead()
                        && java.util.Objects.equals(oldMsg.getReaction(), newMsg.getReaction())
                        && java.util.Objects.equals(oldMsg.getText(), newMsg.getText())
                        && java.util.Objects.equals(oldMsg.getDeletedBy(), newMsg.getDeletedBy());
            }
        });

        messageList.clear();
        messageList.addAll(newSortedMessages);
        diffResult.dispatchUpdatesTo(chatAdapter);
    }

    private ChatAdapter.ChatActionListener createChatActionListener() {
        return new ChatAdapter.ChatActionListener() {
            @Override
            public void onSelectionChanged(int selectedCount) {
                if (selectedCount > 0) {
                    if (selectionToolbar.getVisibility() == View.GONE) {
                        selectionToolbar.setVisibility(View.VISIBLE);
                        selectionToolbar.setAlpha(0f);
                        selectionToolbar.setTranslationY(-50f);
                        selectionToolbar.animate().alpha(1f).translationY(0f).setDuration(200).start();
                    }
                    tvSelectedCount.setText("Выбрано: " + selectedCount);
                } else {
                    selectionToolbar.animate().alpha(0f).translationY(-50f).setDuration(200).withEndAction(() -> selectionToolbar.setVisibility(View.GONE)).start();
                }
            }

            @Override
            public void onReactionSelected(ChatMessage message, String reaction) {
                VibratorHelper.vibrate(ChatActivity.this, 20);
                if (reaction == null)
                    chatRef.child(message.getMessageId()).child("reaction").removeValue();
                else chatRef.child(message.getMessageId()).child("reaction").setValue(reaction);
            }

            @Override
            public void onEditMessage(ChatMessage message) {
                editingMessageId = message.getMessageId();
                etMessageInput.setText(CryptoHelper.decrypt(message.getText()));
                etMessageInput.setSelection(message.getText().length());
                replyPreviewContainer.setVisibility(View.VISIBLE);
                tvReplySender.setText("Редактирование");
                tvReplyText.setText(CryptoHelper.decrypt(message.getText()));
                updateInputUI();
                etMessageInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.showSoftInput(etMessageInput, InputMethodManager.SHOW_IMPLICIT);
            }

            @Override
            public void onDeleteMessage(ChatMessage message, boolean isMine) {
                showPremiumDeleteDialog(message, isMine);
            }

            @Override
            public void onPinMessage(ChatMessage message) {
                pinnedRef.setValue(message.getMessageId());
            }

            @Override
            public void onMessageHighlighted(String messageId) {
                VibratorHelper.vibrate(ChatActivity.this, 20);
            }

            @Override
            public void onReplyMessage(ChatMessage message) {
                setupReplyPreview(message, false);
            }

            @Override
            public void onQuoteClicked(String messageId) {
                scrollToAndHighlightMessage(messageId);
            }

            @Override
            public void onCancelUpload(String messageId) {
                java.util.concurrent.Future<?> task = uploadTasks.get(messageId);
                if (task != null) {
                    task.cancel(true);
                    uploadTasks.remove(messageId);
                }
                removeTempMessageLocally(messageId);
            }

            @Override
            public void onImageClicked(View thumbView, ChatMessage message) {
                List<ChatMessage> imagesOnly = new ArrayList<>();
                int position = 0;
                for (ChatMessage msg : messageList) {
                    if ("image".equals(msg.getType())) {
                        if (msg.getMessageId().equals(message.getMessageId())) {
                            position = imagesOnly.size();
                        }
                        imagesOnly.add(msg);
                    }
                }

                Intent intent = new Intent(ChatActivity.this, MediaBrowserActivity.class);
                intent.putExtra("images", (java.io.Serializable) imagesOnly);
                intent.putExtra("position", position);
                intent.putExtra("chatId", chatId);

                androidx.core.app.ActivityOptionsCompat options =
                        androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                                ChatActivity.this, thumbView, "photo_" + message.getMessageId());

                startActivity(intent, options.toBundle());
            }

            @Override
            public void onSecretImageClicked(View thumbView, ChatMessage message) {
                Intent intent = new Intent(ChatActivity.this, OneTimeViewerActivity.class);
                intent.putExtra("imageUrl", message.getRemoteUrl());
                intent.putExtra("messageId", message.getMessageId());
                intent.putExtra("chatId", chatId);
                intent.putExtra("isSender", message.getSenderId().equals(currentUserId));

                androidx.core.app.ActivityOptionsCompat options =
                        androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                                ChatActivity.this, thumbView, "shared_photo");
                startActivity(intent, options.toBundle());
            }
        };
    }

    private void showPremiumDeleteDialog(ChatMessage message, boolean isMine) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) {
            activityRootView.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.MIRROR));
        }

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#66000000"));
        rootLayout.setAlpha(0f);

        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        cardView.setRadius(48f);
        cardView.setCardElevation(20f);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                (int) (screenWidth * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;
        cardView.setLayoutParams(cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(64, 64, 64, 48);

        TextView title = new TextView(this);
        title.setText("Удалить сообщение?");
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        title.setGravity(Gravity.CENTER);
        cardContent.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Это действие нельзя будет отменить.");
        desc.setTextSize(14f);
        desc.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 16, 0, 48);
        cardContent.addView(desc);

        final boolean[] deleteForEveryone = {false};
        if (isMine) {
            LinearLayout toggleRow = new LinearLayout(this);
            toggleRow.setOrientation(LinearLayout.HORIZONTAL);
            toggleRow.setGravity(Gravity.CENTER_VERTICAL);
            toggleRow.setPadding(32, 24, 32, 24);

            GradientDrawable toggleBg = new GradientDrawable();
            toggleBg.setShape(GradientDrawable.RECTANGLE);
            toggleBg.setCornerRadius(16f);
            toggleBg.setColor(Color.parseColor("#1A000000"));
            toggleRow.setBackground(toggleBg);

            ImageView tickIcon = new ImageView(this);
            tickIcon.setImageResource(R.drawable.ic_check);
            tickIcon.setColorFilter(Color.GRAY);
            tickIcon.setLayoutParams(new LinearLayout.LayoutParams(48, 48));

            TextView toggleText = new TextView(this);
            toggleText.setText("Удалить также у " + tvChatUsername.getText());
            toggleText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            toggleText.setPadding(24, 0, 0, 0);

            toggleRow.addView(tickIcon);
            toggleRow.addView(toggleText);
            cardContent.addView(toggleRow);

            toggleRow.setOnClickListener(v -> {
                deleteForEveryone[0] = !deleteForEveryone[0];
                if (deleteForEveryone[0]) {
                    toggleBg.setColor(Color.parseColor("#33FF5252"));
                    tickIcon.setColorFilter(Color.parseColor("#FF5252"));
                } else {
                    toggleBg.setColor(Color.parseColor("#1A000000"));
                    tickIcon.setColorFilter(Color.GRAY);
                }
            });
        }

        LinearLayout btnContainer = new LinearLayout(this);
        btnContainer.setOrientation(LinearLayout.HORIZONTAL);
        btnContainer.setPadding(0, 48, 0, 0);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Отмена");
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextSize(16f);
        btnCancel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        btnCancel.setPadding(0, 32, 0, 32);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnDelete = new TextView(this);
        btnDelete.setText("Удалить");
        btnDelete.setGravity(Gravity.CENTER);
        btnDelete.setTextSize(16f);
        btnDelete.setTypeface(null, android.graphics.Typeface.BOLD);
        btnDelete.setTextColor(Color.parseColor("#FF5252"));
        btnDelete.setPadding(0, 32, 0, 32);
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnContainer.addView(btnCancel);
        btnContainer.addView(btnDelete);
        cardContent.addView(btnContainer);
        cardView.addView(cardContent);
        rootLayout.addView(cardView);
        dialog.setContentView(rootLayout);

        rootLayout.animate().alpha(1f).setDuration(200).start();
        cardView.setScaleX(0.8f); cardView.setScaleY(0.8f);
        cardView.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new OvershootInterpolator(1.2f)).start();

        Runnable closeDialog = () -> {
            rootLayout.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) {
                    activityRootView.setRenderEffect(null);
                }
                dialog.dismiss();
            }).start();
        };

        btnCancel.setOnClickListener(v -> closeDialog.run());

        btnDelete.setOnClickListener(v -> {
            closeDialog.run();

            String msgId = message.getMessageId();

            if (isMine && deleteForEveryone[0]) {
                // Удаляем совсем для всех
                chatRef.child(msgId).removeValue();
            } else {
                // Помечаем как удаленное для себя (или удаляем ветку, если хочешь радикально)
                chatRef.child(msgId).child("deletedBy").setValue(currentUserId);
            }

            new Thread(() -> {
                AppDatabase.getDatabase(this).localMessageDao().deleteById(msgId);

                runOnUiThread(() -> {
                    int position = messageList.indexOf(message);
                    if (position >= 0) {
                        messageList.remove(position);
                        chatAdapter.notifyItemRemoved(position);
                    }
                    if (messageList.isEmpty()) {
                        emptyChatContainer.setVisibility(View.VISIBLE);
                    }
                });
            }).start();
        });
        rootLayout.setOnClickListener(v -> closeDialog.run());

        dialog.setOnCancelListener(d -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) activityRootView.setRenderEffect(null);
        });
        dialog.show();
    }


    private Handler updateHandler = new Handler();
    private Runnable pendingUpdate = null;

    private void scheduleUpdate() {
        if (pendingUpdate != null) {
            updateHandler.removeCallbacks(pendingUpdate);
        }
        pendingUpdate = () -> {
            List<ChatMessage> sorted = new ArrayList<>(messageList);
            Collections.sort(sorted, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
            updateMessageList(sorted);
        };
        updateHandler.postDelayed(pendingUpdate, 100);
    }


    private void setupSelectionActions() {

        Set<String> selectedIds = chatAdapter.getSelectedMessageIds();

        btnCloseSelection.setOnClickListener(v -> chatAdapter.clearSelection());

        btnSelectionCopy.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (ChatMessage msg : messageList) {
                if (selectedIds.contains(msg.getMessageId()) && "text".equals(msg.getType())) {
                    sb.append(CryptoHelper.decrypt(msg.getText())).append("\n");
                }
            }
            if (sb.length() > 0) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Messages", sb.toString().trim());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Текст скопирован", Toast.LENGTH_SHORT).show();
            } else Toast.makeText(this, "Нет текста", Toast.LENGTH_SHORT).show();
            chatAdapter.clearSelection();
        });

        btnSelectionDelete.setOnClickListener(v -> {
            if (selectedIds.isEmpty()) return;

            // Сохраняем ID выбранных сообщений и находим их позиции
            List<Integer> positionsToRemove = new ArrayList<>();
            for (int i = 0; i < messageList.size(); i++) {
                if (selectedIds.contains(messageList.get(i).getMessageId())) {
                    positionsToRemove.add(i);
                }
            }

            // Удаляем из Firebase и Room в фоне
            for (String id : selectedIds) {
                chatRef.child(id).removeValue();
                new Thread(() -> {
                    AppDatabase.getDatabase(ChatActivity.this).localMessageDao().deleteById(id);
                }).start();
            }

            // Удаляем из локального списка и анимируем
            chatAdapter.clearSelection();
            // Удаляем в обратном порядке, чтобы не сбивались индексы
            Collections.sort(positionsToRemove, Collections.reverseOrder());
            for (int pos : positionsToRemove) {
                if (pos >= 0 && pos < messageList.size()) {
                    messageList.remove(pos);
                    chatAdapter.notifyItemRemoved(pos);
                }
            }

            if (messageList.isEmpty()) {
                emptyChatContainer.setVisibility(View.VISIBLE);
            }
        });

        btnSelectionReact.setOnClickListener(v -> {
            LinearLayout menuLayout = new LinearLayout(this);
            menuLayout.setOrientation(LinearLayout.HORIZONTAL);
            menuLayout.setPadding(16, 16, 16, 16);
            menuLayout.setBackgroundResource(R.drawable.bg_telegram_popup);

            String[] emojis = {"👍", "❤️", "😂", "😢", "😡", "🎉"};
            for (String emoji : emojis) {
                TextView tvEmoji = new TextView(this);
                tvEmoji.setText(emoji);
                tvEmoji.setTextSize(28f);
                tvEmoji.setPadding(16, 16, 16, 16);

                tvEmoji.setOnTouchListener((view, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start(); break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: view.animate().scaleX(1f).scaleY(1f).setDuration(100).setInterpolator(new OvershootInterpolator()).start(); break;
                    }
                    return false;
                });

                menuLayout.addView(tvEmoji);
            }

            PopupWindow popupWindow = new PopupWindow(menuLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.setElevation(16f);
            popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

            for(int i=0; i<menuLayout.getChildCount(); i++){
                View child = menuLayout.getChildAt(i);
                child.setOnClickListener(click -> {
                    popupWindow.dismiss();
                    for (String id : chatAdapter.getSelectedMessageIds()) chatRef.child(id).child("reaction").setValue(((TextView)child).getText().toString());
                    chatAdapter.clearSelection();
                });
            }

            menuLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            popupWindow.showAsDropDown(btnSelectionReact, -menuLayout.getMeasuredWidth() / 2, 20);
        });

        btnSelectionForward.setOnClickListener(v -> {});
    }

    private void showAttachmentMenuPopup(View anchorView) {
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(0, 16, 0, 16);
        menuLayout.setBackgroundResource(R.drawable.bg_telegram_popup);

        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

        TextView btnPhoto = new TextView(this);
        btnPhoto.setText("📷 Фото и Видео");
        btnPhoto.setTextSize(16f);
        btnPhoto.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        btnPhoto.setPadding(48, 36, 64, 36);
        btnPhoto.setBackgroundResource(outValue.resourceId);

        menuLayout.addView(btnPhoto);

        PopupWindow popupWindow = new PopupWindow(menuLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(16f);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        btnPhoto.setOnClickListener(v -> {
            popupWindow.dismiss();
            pickImageLauncher.launch("image/*");
        });


        TextView btnFile = new TextView(this);
        btnFile.setText("📁 Документ / Файл");
        btnFile.setTextSize(16f);
        btnFile.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        btnFile.setPadding(48, 36, 64, 36);
        btnFile.setBackgroundResource(outValue.resourceId);

        menuLayout.addView(btnFile);

        btnFile.setOnClickListener(v -> {
            popupWindow.dismiss();
            pickFileLauncher.launch("*/*");
        });

        menuLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popupWindow.showAsDropDown(anchorView, 0, -menuLayout.getMeasuredHeight() - anchorView.getHeight() - 20);
    }


    /* |-----------------------------------------------------------------------|
     * |                           ГАЛЕРЕЯ И ЗУМ                               |
     * |-----------------------------------------------------------------------| */

//    private void openGallery(View thumbView, ChatMessage clickedMessage) {
//        if (isGalleryOpen) return;
//        isGalleryOpen = true;
//
//        galleryImages.clear();
//        int startIndex = 0;
//        for (ChatMessage msg : messageList) {
//            if ("image".equals(msg.getType()) && msg.getImageUrl() != null) {
//                if (msg.getMessageId().equals(clickedMessage.getMessageId())) {
//                    startIndex = galleryImages.size();
//                }
//                galleryImages.add(msg);
//            }
//        }
//
//        galleryAdapter = new GalleryAdapter(galleryImages);
//        galleryViewPager.setAdapter(galleryAdapter);
//        galleryViewPager.setCurrentItem(startIndex, false);
//        updateGalleryCounter(startIndex);
//
//        galleryViewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageSelected(int position) {
//                updateGalleryCounter(position);
//            }
//        });
//
//        btnGalleryMenu.setOnClickListener(v -> showGalleryMenu(v, galleryImages.get(galleryViewPager.getCurrentItem())));
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            android.graphics.RenderEffect blurEffect = android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.MIRROR);
//            findViewById(R.id.appBarLayout).setRenderEffect(blurEffect);
//            findViewById(R.id.chatRecyclerView).setRenderEffect(blurEffect);
//            findViewById(R.id.bottomInputContainer).setRenderEffect(blurEffect);
//        }
//
//        galleryContainer.setBackgroundColor(Color.BLACK);
//        findViewById(R.id.galleryToolbar).setAlpha(1f);
//
//
//        galleryContainer.setAlpha(0f);
//        galleryContainer.setVisibility(View.VISIBLE);
//        galleryContainer.animate()
//                .alpha(1f)
//                .setDuration(250)
//                .start();
//    }


    private void openGallery(View thumbView, ChatMessage clickedMessage) {
        List<ChatMessage> imagesOnly = new ArrayList<>();
        int position = 0;
        for (ChatMessage msg : messageList) {
            if ("image".equals(msg.getType())) {
                if (msg.getMessageId().equals(clickedMessage.getMessageId())) {
                    position = imagesOnly.size();
                }
                imagesOnly.add(msg);
            }
        }

        Intent intent = new Intent(this, MediaBrowserActivity.class);
        intent.putExtra("images", (java.io.Serializable) imagesOnly);
        intent.putExtra("position", position);
        intent.putExtra("chatId", chatId); // ДОБАВЬ ЭТО
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    private void closeGallery() {
        if (!isGalleryOpen) return;

        galleryContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    galleryContainer.setVisibility(View.GONE);
                    isGalleryOpen = false;

                    // СНИМАЕМ БЛЮР
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        findViewById(R.id.appBarLayout).setRenderEffect(null);
                        findViewById(R.id.chatRecyclerView).setRenderEffect(null);
                        findViewById(R.id.bottomInputContainer).setRenderEffect(null);
                    }

                    // Возвращаем картинку в нормальное состояние (если закрыли свайпом)
                    if (galleryViewPager.getChildAt(0) != null) {
                        View currentView = galleryViewPager.getChildAt(0);
                        com.github.chrisbanes.photoview.PhotoView photoView = currentView.findViewById(R.id.photoView);
                        if (photoView != null) {
                            photoView.setTranslationY(0f);
                            photoView.setScaleX(1f);
                            photoView.setScaleY(1f);
                        }
                    }
                }).start();
    }

    private void updateGalleryCounter(int position) {
        tvGalleryCounter.setText((position + 1) + " из " + galleryImages.size());
    }

    private void showGalleryMenu(View anchor, ChatMessage currentMessage) {
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(0, 16, 0, 16);
        menuLayout.setBackgroundResource(R.drawable.bg_telegram_popup); // Ваш фон для меню

        TextView btnDownload = createMenuTextView("💾 Сохранить в галерею");

        TextView btnShare = createMenuTextView("📤 Поделиться");

        menuLayout.addView(btnDownload);
        menuLayout.addView(btnShare);

        PopupWindow popupWindow = new PopupWindow(menuLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(20f);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        btnDownload.setOnClickListener(v -> {
            popupWindow.dismiss();
            downloadImage(currentMessage.getImageUrl());
        });

        btnShare.setOnClickListener(v -> {
            popupWindow.dismiss();
            shareImage(currentMessage.getImageUrl());
        });

        menuLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popupWindow.showAsDropDown(anchor, -menuLayout.getMeasuredWidth() + anchor.getWidth(), 20);
    }

    private TextView createMenuTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16f);
        textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        textView.setPadding(48, 32, 64, 32);
        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        textView.setBackgroundResource(outValue.resourceId);
        return textView;
    }

    private void downloadImage(String imageUrl) {
        android.app.DownloadManager downloadManager = (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(imageUrl);
        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(uri);
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "MapMemories_" + System.currentTimeMillis() + ".jpg");
        downloadManager.enqueue(request);
        Toast.makeText(this, "Скачивание началось...", Toast.LENGTH_SHORT).show();
    }

    private void shareImage(String imageUrl) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Смотри это фото: " + imageUrl);
        startActivity(Intent.createChooser(shareIntent, "Поделиться фото"));
    }

    /* |-----------------------------------------------------------------------|
     * |                         АДАПТЕР ГАЛЕРЕИ                               |
     * |-----------------------------------------------------------------------| */
    private class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {
        private List<ChatMessage> images;

        public GalleryAdapter(List<ChatMessage> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_image, parent, false);
            return new GalleryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
            String url = images.get(position).getImageUrl();

            Glide.with(ChatActivity.this)
                    .load(url)
                    .override(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .into(holder.photoView);

            holder.photoView.setOnPhotoTapListener((view, x, y) -> closeGallery());

            holder.photoView.setOnTouchListener(new View.OnTouchListener() {
                private float startY;
                private boolean isDragging = false;
                private View galleryToolbar = findViewById(R.id.galleryToolbar);

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (holder.photoView.getScale() > 1.0f) return false;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getRawY();
                            isDragging = false;
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float dy = event.getRawY() - startY;

                            if (!isDragging && Math.abs(dy) > 30) {
                                isDragging = true;
                                galleryViewPager.setUserInputEnabled(false);
                            }

                            if (isDragging) {
                                holder.photoView.setTranslationY(dy);

                                float alpha = 1f - Math.min(Math.abs(dy) / (galleryContainer.getHeight() / 2f), 1f);
                                galleryContainer.setBackgroundColor(Color.argb((int) (alpha * 255), 0, 0, 0));
                                galleryToolbar.setAlpha(alpha);

                                float scale = 1f - Math.min(Math.abs(dy) / (galleryContainer.getHeight() * 2f), 0.2f);
                                holder.photoView.setScaleX(scale);
                                holder.photoView.setScaleY(scale);

                                return true;
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isDragging) {
                                float finalDy = event.getRawY() - startY;

                                if (Math.abs(finalDy) > 250) {
                                    closeGallery();
                                } else {
                                    holder.photoView.animate()
                                            .translationY(0f)
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(200)
                                            .setInterpolator(new OvershootInterpolator(1.0f))
                                            .start();

                                    galleryContainer.setBackgroundColor(Color.BLACK);
                                    galleryToolbar.animate().alpha(1f).setDuration(200).start();
                                }

                                isDragging = false;
                                galleryViewPager.setUserInputEnabled(true);
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class GalleryViewHolder extends RecyclerView.ViewHolder {
            com.github.chrisbanes.photoview.PhotoView photoView;
            GalleryViewHolder(@NonNull View itemView) {
                super(itemView);
                photoView = itemView.findViewById(R.id.photoView);
            }
        }
    }


    /* |-----------------------------------------------------------------------|
     * |                           ЛОГИКА ЗАПИСИ ГС                        |
     * |                           ЛОГИКА ЗАПИСИ ГС                        |
     * |-----------------------------------------------------------------------| */

    private void setupVoiceRecording() {
        btnCancelVoiceLock.setOnClickListener(v -> cancelRecording());

        btnRecordVoice.setOnTouchListener(new View.OnTouchListener() {
            float startY, startX;
            boolean isCancelled = false;
            boolean lockedInThisGesture = false;
            boolean directionDecided = false;
            boolean movingUp = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isRecordingLocked) {
                            btnRecordVoice.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                            return true;
                        }

                        boolean started = startRecording();
                        if (!started) return false; // разрешения нет, ничего не делаем

                        VibratorHelper.vibrate(ChatActivity.this, 50);

                        startY = event.getRawY(); startX = event.getRawX();
                        isCancelled = false;
                        lockedInThisGesture = false;
                        directionDecided = false;
                        movingUp = false;

                        btnRecordVoice.animate().scaleX(1.5f).scaleY(1.5f).setDuration(200).start();

                        textInputContainer.setVisibility(View.GONE);
                        recordingContainer.setVisibility(View.VISIBLE);

                        lockOverlay.setVisibility(View.VISIBLE);
                        lockOverlay.setAlpha(0f);
                        lockOverlay.animate().alpha(1f).translationY(0).setDuration(200).start();

                        tvSlideToCancel.setVisibility(View.VISIBLE);
                        tvSlideToCancel.setAlpha(1f);
                        btnCancelVoiceLock.setVisibility(View.GONE);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (isRecordingLocked || isCancelled || !isRecording) break;

                        float dy = startY - event.getRawY();
                        float dx = startX - event.getRawX();

                        if (!directionDecided && (Math.abs(dy) > 20 || Math.abs(dx) > 20)) {
                            directionDecided = true;
                            movingUp = Math.abs(dy) > Math.abs(dx);
                        }

                        if (directionDecided) {
                            if (movingUp) {
                                if (dy > 150) {
                                    lockedInThisGesture = true;
                                    isRecordingLocked = true;
                                    VibratorHelper.vibrate(ChatActivity.this, 30);

                                    lockOverlay.animate().alpha(0f).translationY(-50).setDuration(200).withEndAction(() -> lockOverlay.setVisibility(View.GONE)).start();
                                    tvSlideToCancel.setVisibility(View.GONE);
                                    btnCancelVoiceLock.setVisibility(View.VISIBLE);

                                    btnRecordVoice.animate().translationY(0).translationX(0).scaleX(1f).scaleY(1f).setDuration(200).start();
                                    btnRecordVoice.setImageResource(android.R.drawable.ic_menu_send);
                                    btnRecordVoice.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3390EC")));
                                    btnRecordVoice.setColorFilter(Color.WHITE);
                                } else if (dy > 0) {
                                    btnRecordVoice.setTranslationY(-dy);
                                }
                            } else {
                                if (dx > 150) {
                                    isCancelled = true;
                                    cancelRecording();
                                } else if (dx > 0) {
                                    btnRecordVoice.setTranslationX(-dx);
                                    tvSlideToCancel.setAlpha(1 - (dx / 150f));
                                }
                            }
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isRecordingLocked) {
                            btnRecordVoice.animate().scaleX(1f).scaleY(1f).setDuration(100).start();

                            if (!lockedInThisGesture) {
                                stopRecordingAndSend();
                            } else {
                                lockedInThisGesture = false;
                            }
                            return true;
                        }

                        if (isCancelled) break;
                        stopRecordingAndSend();
                        break;
                }
                return true;
            }
        });
    }

    private boolean startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO);
            return false; // запись не началась
        }

        isRecording = true;
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".m4a";
        typingRef.child(currentUserId).setValue("recording");

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            recordStartTime = System.currentTimeMillis();
            timerHandler.post(updateTimerRunnable);

            AlphaAnimation blink = new AlphaAnimation(1.0f, 0.2f);
            blink.setDuration(500);
            blink.setRepeatMode(Animation.REVERSE);
            blink.setRepeatCount(Animation.INFINITE);
            redDot.startAnimation(blink);
        } catch (Exception e) {
            e.printStackTrace();
            cancelRecording();
            Toast.makeText(this, "Ошибка микрофона", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void cancelRecording() {
        resetRecordingUI();
        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch (Exception e) {}
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (audioFilePath != null) new File(audioFilePath).delete();
    }

    private void stopRecordingAndSend() {
        long duration = System.currentTimeMillis() - recordStartTime;
        resetRecordingUI();

        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch (Exception e) {}
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if (duration < 1000) {
            if (audioFilePath != null) new File(audioFilePath).delete();
            Toast.makeText(this, "Слишком короткое", Toast.LENGTH_SHORT).show();
            return;
        }
        uploadAudioAndSend(Uri.fromFile(new File(audioFilePath)), duration);
    }

    private void resetRecordingUI() {
        isRecording = false;
        isRecordingLocked = false;
        typingRef.child(currentUserId).setValue("false");
        timerHandler.removeCallbacks(updateTimerRunnable);
        redDot.clearAnimation();

        recordingContainer.setVisibility(View.GONE);
        textInputContainer.setVisibility(View.VISIBLE);
        lockOverlay.setVisibility(View.GONE);
        btnCancelVoiceLock.setVisibility(View.GONE);

        btnRecordVoice.animate().scaleX(1f).scaleY(1f).translationY(0).translationX(0).setDuration(200).start();
        btnRecordVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
        btnRecordVoice.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A3390EC")));
        btnRecordVoice.setColorFilter(Color.parseColor("#3390EC"));

        tvSlideToCancel.setText("◀ Отменить");
        tvSlideToCancel.setAlpha(1f);

        updateInputUI();
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - recordStartTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            tvRecordTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    private void uploadAudioAndSend(Uri audioUri, long durationMs) {
        String tempMessageId = "temp_voice_" + System.currentTimeMillis();
        ChatMessage tempMsg = new ChatMessage(currentUserId, targetUserId, audioUri.toString(), null, System.currentTimeMillis(), "voice");
        tempMsg.setMessageId(tempMessageId);
        attachReplyDataToMessage(tempMsg);

        messageList.add(tempMsg);
        chatAdapter.addUploadingMessage(tempMessageId);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        java.util.concurrent.Future<?> uploadTask = Executors.newSingleThreadExecutor().submit(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(audioUri);
                Map<String, Object> options = new HashMap<>();
                options.put("resource_type", "video");
                Map uploadResult = cloudinary.uploader().upload(inputStream, options);
                String secureUrl = (String) uploadResult.get("secure_url");

                runOnUiThread(() -> {
                    uploadTasks.remove(tempMessageId);
                    removeTempMessageLocally(tempMessageId);
                    if (!isFinishing() && !isDestroyed()) {
                        String finalId = chatRef.push().getKey();
                        if (finalId != null) {
                            ChatMessage message = new ChatMessage(currentUserId, targetUserId, secureUrl, null, System.currentTimeMillis(), "voice");
                            message.setMessageId(finalId);

                            if (replyingToMessage != null) {
                                message.setReplyMessageId(replyingToMessage.getMessageId());
                                message.setReplySenderId(replyingToMessage.getSenderId());
                                message.setReplyText(replyingToMessage.getText() != null ? replyingToMessage.getText() : "Вложение");
                            }

                            chatRef.child(finalId).setValue(message);
                            closeReplyPreview();
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    uploadTasks.remove(tempMessageId);
                    removeTempMessageLocally(tempMessageId);
                    Toast.makeText(ChatActivity.this, "Ошибка отправки ГС", Toast.LENGTH_SHORT).show();
                });
            }
        });
        uploadTasks.put(tempMessageId, uploadTask);
    }

    /* |-----------------------------------------------------------------------|
     * |                       ЛОГИКА ОТПРАВКИ И ПРЕВЬЮ                    |
     * |-----------------------------------------------------------------------| */

    private void updateInputUI() {
        boolean hasText = etMessageInput.getText().toString().trim().length() > 0;
        boolean hasImage = selectedImageUri != null;

        // Убрали проверку isRecordingLocked отсюда!
        if (hasText || hasImage) {
            btnSend.setVisibility(View.VISIBLE);
            btnRecordVoice.setVisibility(View.GONE);
            btnAttach.setVisibility(View.GONE);
        } else {
            btnAttach.setVisibility(View.VISIBLE);
            btnSend.setVisibility(View.GONE);
            btnRecordVoice.setVisibility(View.VISIBLE);
        }
    }

    private void closeReplyPreview() {
        replyingToMessage = null;
        editingMessageId = null;
        replyPreviewContainer.setVisibility(View.GONE);
        etMessageInput.setText("");
        updateInputUI();
    }

    private void setupReplyPreview(ChatMessage message, boolean isEditing) {
        VibratorHelper.vibrate(this, 30);

        replyingToMessage = message;

        if (replyPreviewContainer.getVisibility() == View.GONE) {
            replyPreviewContainer.setVisibility(View.VISIBLE);
            replyPreviewContainer.setTranslationY(50f);
            replyPreviewContainer.setAlpha(0f);
            replyPreviewContainer.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(new OvershootInterpolator(1.0f))
                    .start();
        }

        if (!isEditing) editingMessageId = null;

        String sender = message.getSenderId().equals(currentUserId) ? "Вы" : tvChatUsername.getText().toString();
        tvReplySender.setText(isEditing ? "Редактирование" : sender);

        String previewText = "";
        if ("text".equals(message.getType())) previewText = decryptMessageText(message);
        else if ("image".equals(message.getType())) previewText = "📷 Фотография";
        else if ("voice".equals(message.getType())) previewText = "🎤 Голосовое сообщение";
        else if ("post".equals(message.getType())) previewText = "🗺️ Воспоминание";
        else if ("file".equals(message.getType())) previewText = "📁 Документ";
        tvReplyText.setText(previewText);

        etMessageInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etMessageInput, InputMethodManager.SHOW_IMPLICIT);
        updateInputUI();
    }

    private void attachReplyDataToMessage(ChatMessage message) {
        if (replyingToMessage != null) {
            message.setReplyMessageId(replyingToMessage.getMessageId());
            message.setReplySenderId(replyingToMessage.getSenderId());

            String replyTxt;
            if ("text".equals(replyingToMessage.getType())) {
                replyTxt = decryptMessageText(replyingToMessage);
            } else if ("image".equals(replyingToMessage.getType())) {
                replyTxt = "📷 Фотография";
            } else if ("voice".equals(replyingToMessage.getType())) {
                replyTxt = "🎤 Голосовое сообщение";
            } else if ("post".equals(replyingToMessage.getType())) {
                replyTxt = "🗺️ Воспоминание";
            } else if ("file".equals(replyingToMessage.getType())) {
                replyTxt = "📁 Документ";
            } else {
                replyTxt = "Вложение";
            }
            message.setReplyText(replyTxt);
        }
    }

    private void sendTextMessage(String text) {
        String messageId = chatRef.push().getKey();

        String myRealKey = CryptoHelper.getLocalPublicKey(currentUserId);

        if (messageId != null && targetPublicKey != null && myRealKey != null) {
            String encForReceiver = CryptoHelper.encryptForRecipient(text, targetPublicKey);
            String encForSender = CryptoHelper.encryptForRecipient(text, myRealKey);

            ChatMessage message = new ChatMessage(currentUserId, targetUserId, encForReceiver, System.currentTimeMillis(), "text");
            message.setTextSender(encForSender);
            message.setMessageId(messageId);

            attachReplyDataToMessage(message);
            chatRef.child(messageId).setValue(message);
            closeReplyPreview();

            draftManager.clearDraft(chatId);
            draftManager.saveImageDraft(chatId, null);

        } else {
            if (myRealKey == null) Toast.makeText(this, "Ошибка: локальный ключ не найден", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Ошибка: ключ собеседника еще загружается", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMyPublicKey() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("publicKey")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        myPublicKey = snapshot.getValue(String.class);
                        // ПРОВЕРЯЕМ СИНХРОНИЗАЦИ
                        syncKeysIfNeeded();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void uploadImageToCloudinaryAndSend(Uri imageUri, String caption) {
        scheduleBackgroundUpload(imageUri, "image", caption);
    }

    private void scheduleBackgroundUpload(Uri uri, String type, String caption) {
        String tempId = "TEMP_" + System.currentTimeMillis();

        LocalMessage local = new LocalMessage();
        local.messageId = tempId;
        local.chatId = chatId;
        local.senderId = currentUserId;
        local.receiverId = targetUserId;
        local.text = caption;
        local.imageUrl = uri.toString();
        local.timestamp = System.currentTimeMillis();
        local.type = type;
        local.isPending = true;

        long currentTime = System.currentTimeMillis();

        new Thread(() -> {
            AppDatabase.getDatabase(this).localMessageDao().insertSingleMessage(local);
            runOnUiThread(() -> {
                ChatMessage uiMsg = new ChatMessage(currentUserId, targetUserId, uri.toString(), caption, local.timestamp, type);
                uiMsg.setMessageId(tempId);
                uiMsg.setSelfDestructTime(currentTimerValue);

                messageList.add(uiMsg);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                chatRecyclerView.scrollToPosition(messageList.size() - 1);
                emptyChatContainer.setVisibility(View.GONE);
            });
        }).start();

        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putLong("timestamp", currentTime)
                .putString("tempId", tempId)
                .putString("uri", uri.toString())
                .putString("type", type)
                .putString("chatId", chatId)
                .putString("senderId", currentUserId)
                .putString("receiverId", targetUserId)
                .putString("targetPubKey", targetPublicKey)
                .putString("myPubKey", CryptoHelper.getLocalPublicKey(currentUserId))
                .putString("caption", caption)
                .putInt("selfDestructTime", currentTimerValue)
                .build();

        androidx.work.OneTimeWorkRequest uploadRequest = new androidx.work.OneTimeWorkRequest.Builder(MessageWorker.class)
                .setInputData(inputData)
                .build();
        androidx.work.WorkManager.getInstance(this).enqueue(uploadRequest);

        etMessageInput.setText("");
        selectedImageUri = null;
        imagePreviewContainer.setVisibility(View.GONE);
        draftManager.clearDraft(chatId);
        draftManager.saveImageDraft(chatId, null);
        currentTimerValue = 0;
        updateInputUI();
    }

    private void removeTempMessageLocally(String tempId) {
        chatAdapter.removeUploadingMessage(tempId);
        for(int i = 0; i < messageList.size(); i++) {
            if(messageList.get(i).getMessageId().equals(tempId)) {
                messageList.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }


    private boolean playNextVoiceMessage(String currentId) {
        if (messageList == null || messageList.isEmpty()) return false;

        int currentIndex = -1;
        String senderId = null;

        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getMessageId() != null && messageList.get(i).getMessageId().equals(currentId)) {
                currentIndex = i;
                senderId = messageList.get(i).getSenderId();
                break;
            }
        }

        if (currentIndex != -1 && senderId != null) {
            for (int i = currentIndex + 1; i < messageList.size(); i++) {
                ChatMessage nextMsg = messageList.get(i);

                if ("voice".equals(nextMsg.getType())) {
                    if (senderId.equals(nextMsg.getSenderId())) {
                        String nextId = nextMsg.getMessageId();
                        String nextUrl = nextMsg.getImageUrl();

                        int finalI = i;
                        new Handler(getMainLooper()).postDelayed(() -> {
                            chatRecyclerView.smoothScrollToPosition(finalI);
                            AudioPlayerManager.getInstance().play(nextId, nextUrl);
                        }, 500);
                        return true;
                    }
                    break;
                }
            }
        }
        return false;
    }

    private void scrollToAndHighlightMessage(String messageId) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getMessageId() != null && messageList.get(i).getMessageId().equals(messageId)) {
                androidx.recyclerview.widget.LinearSmoothScroller smoothScroller = new androidx.recyclerview.widget.LinearSmoothScroller(this) {
                    @Override protected int getVerticalSnapPreference() { return androidx.recyclerview.widget.LinearSmoothScroller.SNAP_TO_ANY; }
                    @Override public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                        return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
                    }
                };
                smoothScroller.setTargetPosition(i);
                if (chatRecyclerView.getLayoutManager() != null) chatRecyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                new Handler().postDelayed(() -> chatAdapter.highlightMessage(messageId), 400);
                break;
            }
        }
    }

    /* |-----------------------------------------------------------------------|
     * |                           FIREBASE СТАТУСЫ                            |
     * |-----------------------------------------------------------------------| */

    private void loadTargetUserData() {

        statusListener = targetUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !isDestroyed()) {
                    String username = snapshot.child("username").getValue(String.class);
                    String avatarUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    targetPublicKey = snapshot.child("publicKey").getValue(String.class);

                    isTargetUserHidden = snapshot.child("privacy").child("hide_online").exists() &&
                            Boolean.TRUE.equals(snapshot.child("privacy").child("hide_online").getValue(Boolean.class));

                    String finalUsername = TextUtils.isEmpty(username) ? "Пользователь" : username;
                    tvChatUsername.setText(finalUsername);
                    if (chatAdapter != null) chatAdapter.setTargetUserName(finalUsername);

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(ChatActivity.this).load(avatarUrl).circleCrop().placeholder(R.drawable.ic_profile_placeholder).into(ivChatAvatar);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        typingListener = typingRef.child(targetUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String state = snapshot.getValue(String.class);
                if ("typing".equals(state)) {
                    tvChatStatus.setText("печатает...");
                    tvChatStatus.setTextColor(Color.parseColor("#3390EC"));
                } else if ("recording".equals(state)) {
                    tvChatStatus.setText("записывает голосовое...");
                    tvChatStatus.setTextColor(Color.parseColor("#3390EC"));
                } else {
                    targetUserRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot statSnap) {
                            Object statusObj = statSnap.getValue();
                            String statusText = TimeFormatter.formatStatus(statusObj, isTargetUserHidden);
                            tvChatStatus.setText(statusText);
                            if ("в сети".equals(statusText)) tvChatStatus.setTextColor(getResources().getColor(R.color.online_indicator));
                            else tvChatStatus.setTextColor(getResources().getColor(R.color.text_secondary));
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadMessagesOptimized() {
        new Thread(() -> {
            LocalMessageDao dao = AppDatabase.getDatabase(this).localMessageDao();

            // 1. Узнаём общее количество сообщений в чате
            int totalCount = dao.getMessageCount(chatId);  // Нужно добавить этот метод (см. ниже)

            // 2. Вычисляем offset так, чтобы взять последние windowSize сообщений
            int offset = Math.max(0, totalCount - windowSize);

            // 3. Загружаем только окно
            List<LocalMessage> localWindow = dao.getMessagesWindow(chatId, windowSize, offset);
            windowStartOffset = offset; // запоминаем, где начало окна

            // 4. Создаём ChatMessage и расшифровываем в фоне
            List<ChatMessage> loadedMessages = new ArrayList<>();
            for (LocalMessage local : localWindow) {
                ChatMessage msg = new ChatMessage();
                msg.setMessageId(local.messageId);
                msg.setSenderId(local.senderId);
                msg.setReceiverId(local.receiverId);
                msg.setTimestamp(local.timestamp);
                msg.setType(local.type);
                msg.setText(local.text);
                msg.setImageUrl(local.imageUrl);
                msg.setRemoteUrl(local.remoteUrl);
                msg.setSelfDestructTime(local.selfDestructTime);
                msg.setOneTime(local.isOneTime);
                // Расшифровываем в фоне
                decryptMessageFields(msg);
                loadedMessages.add(msg);
            }

            runOnUiThread(() -> {
                messageList.clear();
                messageList.addAll(loadedMessages);

                List<ChatMessage> sorted = new ArrayList<>(messageList);
                Collections.sort(sorted, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                updateMessageList(sorted);

                if (!messageList.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                    emptyChatContainer.setVisibility(View.GONE);
                }

                startFirebaseListener();
                runCurtainAnimation();
            });
        }).start();
    }

//    private void performFullDelete(ChatMessage message) {
//
//        chatRef.child(message.getMessageId()).removeValue();
//
//        new Thread(() -> {
//            AppDatabase.getDatabase(this).localMessageDao().deleteById(message.getMessageId());
//            runOnUiThread(() -> {
//                messageList.remove(message);
//                chatAdapter.notifyDataSetChanged();
//            });
//        }).start();
//    }


    private void runCurtainAnimation() {
        FrameLayout overlay = findViewById(R.id.loadingOverlayContainer);
        ProgressBar progressBar = findViewById(R.id.loadingProgressBar);

        overlay.postDelayed(() -> {
            ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
            progressAnim.setDuration(600);
            progressAnim.setInterpolator(new DecelerateInterpolator());

            progressAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    overlay.animate()
                            .translationY(-overlay.getHeight())
                            .setDuration(400)
                            .setInterpolator(new AccelerateInterpolator())
                            .withEndAction(() -> {
                                overlay.setVisibility(View.GONE);
                                if (messageList.isEmpty()) {
                                    emptyChatContainer.setVisibility(View.VISIBLE);
                                }
                            })
                            .start();

                    chatRecyclerView.animate().alpha(1f).setDuration(400).start();
                }
            });
            progressAnim.start();
        }, 100);
    }

    private void startFirebaseListener() {
        messagesListener = chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg == null) return;

                // Расшифровываем в фоне, потом добавляем в список на UI
                new Thread(() -> {
                    decryptMessageFields(msg);
                    runOnUiThread(() -> {
                        if (msg.getDeletedBy() != null && msg.getDeletedBy().equals(currentUserId)) {
                            return; // удалено для нас – не показываем
                        }

                        // Проверка на дубликат
                        int existingIndex = -1;
                        for (int i = 0; i < messageList.size(); i++) {
                            ChatMessage existing = messageList.get(i);
                            if (existing.getMessageId() != null && existing.getMessageId().equals(msg.getMessageId())) {
                                existingIndex = i;
                                break;
                            }
                        }

// Если нашли TEMP_ по timestamp, заменяем его реальным сообщением
                        if (existingIndex == -1) {
                            for (int i = 0; i < messageList.size(); i++) {
                                ChatMessage existing = messageList.get(i);
                                if (existing.getMessageId().startsWith("TEMP_") && existing.getTimestamp() == msg.getTimestamp()) {
                                    existingIndex = i;
                                    break;
                                }
                            }
                            if (existingIndex != -1) {
                                // Убираем временное сообщение из индикаторов загрузки
                                String oldTempId = messageList.get(existingIndex).getMessageId();
                                chatAdapter.removeUploadingMessage(oldTempId);
                                // Заменяем на реальное
                                messageList.set(existingIndex, msg);
                                chatAdapter.notifyItemChanged(existingIndex);
                                // Помечаем прочитанным (как обычно)
                                if (msg.getReceiverId() != null && msg.getReceiverId().equals(currentUserId) && !msg.isRead()) {
                                    snapshot.getRef().child("read").setValue(true);
                                    msg.setRead(true);
                                }
                                scheduleUpdate();
                                chatRecyclerView.post(() -> chatRecyclerView.scrollToPosition(messageList.size() - 1));
                                return; // выходим, чтобы не добавлять второй раз
                            }
                        }

// Если это не TEMP_ замена, обрабатываем как обычно
                        if (existingIndex != -1) {
                            messageList.set(existingIndex, msg);
                        } else {
                            if (msg.getDeletedBy() == null || !msg.getDeletedBy().equals(currentUserId)) {
                                messageList.add(msg);
                                emptyChatContainer.setVisibility(View.GONE);
                            }
                        }

                        if (msg.getReceiverId() != null && msg.getReceiverId().equals(currentUserId) && !msg.isRead()) {
                            snapshot.getRef().child("read").setValue(true);
                            msg.setRead(true);
                        }

                        scheduleUpdate();
                        chatRecyclerView.post(() -> chatRecyclerView.scrollToPosition(messageList.size() - 1));
                    });
                }).start();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage updatedMsg = snapshot.getValue(ChatMessage.class);
                if (updatedMsg == null || updatedMsg.getMessageId() == null) return;
                for (int i = 0; i < messageList.size(); i++) {
                    ChatMessage local = messageList.get(i);
                    if (local.getMessageId() != null && local.getMessageId().equals(updatedMsg.getMessageId())) {
                        local.setRead(updatedMsg.isRead());
                        local.setReaction(updatedMsg.getReaction());
                        local.setText(updatedMsg.getText());
                        local.setTextSender(updatedMsg.getTextSender());
                        local.setDeletedBy(updatedMsg.getDeletedBy());
                        local.setDecryptedTextCache(null);
                        local.setDecryptedReplyTextCache(null);
                        chatAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String id = snapshot.getKey();
                for (int i = 0; i < messageList.size(); i++) {
                    if (messageList.get(i).getMessageId().equals(id)) {
                        messageList.remove(i);
                        chatAdapter.notifyItemRemoved(i);
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });
    }

    private String decryptMessageText(ChatMessage msg) {
        if (msg.getDecryptedTextCache() != null) return msg.getDecryptedTextCache();
        String raw = (currentUserId != null && currentUserId.equals(msg.getSenderId()))
                ? (msg.getTextSender() != null ? msg.getTextSender() : msg.getText())
                : msg.getText();
        if (raw == null) return "";
        if (raw.startsWith("ENC_V3:")) {
            String dec = CryptoHelper.decrypt(raw);
            msg.setDecryptedTextCache(dec);
            return dec;
        }
        return raw;
    }

    private void loadPinnedMessage() {
        pinnedListener = pinnedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String pinnedId = snapshot.getValue(String.class);
                    chatRef.child(pinnedId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot msgSnap) {
                            if (msgSnap.exists()) {
                                ChatMessage msg = msgSnap.getValue(ChatMessage.class);
                                if (msg != null) {
                                    pinnedMessageContainer.setVisibility(View.VISIBLE);
                                    String text = "text".equals(msg.getType()) ? decryptMessageText(msg) :
                                            ("image".equals(msg.getType()) ? "📷 Фотография" :
                                                    ("voice".equals(msg.getType()) ? "🎤 Голосовое" :
                                                            ("file".equals(msg.getType()) ? "📁 Документ" : "🗺️ Воспоминание")));
                                    tvPinnedText.setText(text);
                                    pinnedMessageContainer.setOnClickListener(v -> scrollToAndHighlightMessage(pinnedId));
                                }
                            } else {
                                pinnedRef.removeValue();
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else {
                    pinnedMessageContainer.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void sortMessagesAndNotify() {
        java.util.Collections.sort(messageList, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));
        chatAdapter.notifyDataSetChanged();
    }



    /* |-----------------------------------------------------------------------|
     * |                           СВАЙП ЗАКРЫТИЯ                              |
     * |-----------------------------------------------------------------------| */

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (rootLayout == null) return super.dispatchTouchEvent(ev);

        if (chatAdapter != null && !chatAdapter.getSelectedMessageIds().isEmpty()) return super.dispatchTouchEvent(ev);
        if (isGalleryOpen) return super.dispatchTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN:
                startX = ev.getRawX();
                startY = ev.getRawY();
                isSwipingToClose = false;
                canSwipeBack = startX < (screenWidth * 0.9f);
                break;

            case android.view.MotionEvent.ACTION_MOVE:
                if (!canSwipeBack) break;
                float dx = ev.getRawX() - startX;
                float dy = ev.getRawY() - startY;

                if (!isSwipingToClose && dx > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    isSwipingToClose = true;
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && getCurrentFocus() != null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                    android.view.MotionEvent cancelEvent = android.view.MotionEvent.obtain(ev);
                    cancelEvent.setAction(android.view.MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (isSwipingToClose) {
                    dx = ev.getRawX() - startX;
                    rootLayout.setTranslationX(Math.max(0, dx * 1.2f));

                    if (isSwipingToClose) {
                        dx = ev.getRawX() - startX;
                        rootLayout.setTranslationX(Math.max(0, dx * 1.2f));
                        return true;
                    }

                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isSwipingToClose) {
                    float dxUp = ev.getRawX() - startX;

                    if (dxUp > screenWidth * 0.15f) {
                        rootLayout.animate()
                                .translationX(screenWidth)
                                .setDuration(150)
                                .withEndAction(() -> {
                                    finish();
                                    overridePendingTransition(0, 0);
                                }).start();
                    } else {
                        rootLayout.animate().translationX(0).setDuration(150).start();
                    }
                    isSwipingToClose = false;
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }






    private void decryptMessageFields(ChatMessage msg) {
        if (msg == null) return;

        // Основной текст (и подпись к фото, и имя файла – все лежат в text)
        String rawText = null;
        if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
            rawText = msg.getTextSender() != null ? msg.getTextSender() : msg.getText();
        } else {
            rawText = msg.getText();
        }

        if (rawText != null) {
            if (rawText.startsWith("ENC_V3:")) {
                msg.setDecryptedTextCache(CryptoHelper.decrypt(rawText));
            } else {
                msg.setDecryptedTextCache(rawText);
            }
        }

        // Текст цитаты
        String replyRaw = msg.getReplyText();
        if (replyRaw != null) {
            if (replyRaw.startsWith("ENC_V3:")) {
                msg.setDecryptedReplyTextCache(CryptoHelper.decrypt(replyRaw));
            } else {
                msg.setDecryptedReplyTextCache(replyRaw);
            }
        }
    }







    @Override
    public void onBackPressed() {
        if (isGalleryOpen) {
            closeGallery();
            return;
        }

        if (chatAdapter != null && !chatAdapter.getSelectedMessageIds().isEmpty()) {
            chatAdapter.clearSelection();
            return;
        }

        getOnBackPressedDispatcher().onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void updateMyStatus(Object status) {
        SharedPreferences prefs = getSharedPreferences(Setting.PREFS_NAME, MODE_PRIVATE);
        boolean hideOnline = prefs.getBoolean("privacy_hide_online", false);
        if (hideOnline) myStatusRef.setValue("hidden");
        else {
            myStatusRef.setValue(status);
            myStatusRef.onDisconnect().setValue(System.currentTimeMillis());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        currentChatUserId = targetUserId;
        clearNotification();

        updateMyStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentChatUserId = null;
        updateMyStatus(System.currentTimeMillis());
    }


    private void clearNotification() {
        if (targetUserId != null) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(targetUserId.hashCode());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioPlayerManager.getInstance().stop();
        typingRef.child(currentUserId).setValue("false");
//        for (java.util.concurrent.Future<?> task : uploadTasks.values()) {
//            if (task != null) task.cancel(true);
//        }
        if (targetUserRef != null && statusListener != null) targetUserRef.removeEventListener(statusListener);
        if (chatRef != null && messagesListener != null) chatRef.removeEventListener(messagesListener);
        if (pinnedRef != null && pinnedListener != null) pinnedRef.removeEventListener(pinnedListener);
        if (typingRef != null && typingListener != null) typingRef.child(targetUserId).removeEventListener(typingListener);

        if (mediaRecorder != null) {
            try { mediaRecorder.stop(); } catch(Exception e){}
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}
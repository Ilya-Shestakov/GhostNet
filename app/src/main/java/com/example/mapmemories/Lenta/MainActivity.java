package com.example.mapmemories.Lenta;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.badge.BadgeDrawable;
import com.example.mapmemories.Chats.ChatMessage;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.example.mapmemories.systemHelpers.MessageListenerService;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.mapmemories.Chats.ChatListFragment;
import com.example.mapmemories.LogRegStart.LoginActivity;
import com.example.mapmemories.Post.CreatePostActivity;
import com.example.mapmemories.Profile.Profile;
import com.example.mapmemories.R;
import com.example.mapmemories.Settings.Setting;
import com.example.mapmemories.database.AppDatabase;
import com.example.mapmemories.systemHelpers.DialogHelper;
import com.example.mapmemories.systemHelpers.VibratorHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MaterialCardView fabAdd, bottomDock;
    private ImageView profileButton, fabSettings, offlineBadge, fabAddIcon;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private long backPressedTime;
    private boolean isDockVisible = true;

    private TextView appTitle;
    private DatabaseReference connectedRef;
    private ValueEventListener connectionListener;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    SharedPreferences prefs = getSharedPreferences(Setting.PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("notifications_enabled", isGranted).apply();

                    if (isGranted) {
                        startService(new Intent(this, MessageListenerService.class));
                        Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        View rootLayout = findViewById(R.id.topHeader);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, insets.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        mAuth = FirebaseAuth.getInstance();
        checkCurrentUser();

        initViews();
        setupViewPager();
        observeUnreadMessages();
        setupClickListeners();
        loadUserAvatar();
        observeOfflinePosts();
        setupDoubleBackExit();
        checkNotificationPermission();
        setupConnectionObserver();
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }
    }

    private void checkNotificationPermission() {
        SharedPreferences prefs = getSharedPreferences(Setting.PREFS_NAME, Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

                prefs.edit().putBoolean("notifications_enabled", true).apply();
                startService(new Intent(this, MessageListenerService.class));
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            prefs.edit().putBoolean("notifications_enabled", true).apply();
            startService(new Intent(this, MessageListenerService.class));
        }
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        bottomDock = findViewById(R.id.bottomDock);
        fabAdd = findViewById(R.id.fabAdd);
        fabAddIcon = findViewById(R.id.fabAddIcon);
        fabSettings = findViewById(R.id.fabSettings);
        profileButton = findViewById(R.id.profileButton);
        offlineBadge = findViewById(R.id.offlineBadge);
        appTitle = findViewById(R.id.appTitle);
        appTitle.setOnClickListener(v -> showNetworkStatsDialog());
    }

    public void toggleBottomDock(boolean show) {
        if (show && !isDockVisible) {
            bottomDock.animate().translationY(0).setDuration(300).withStartAction(() -> isDockVisible = true).start();
        } else if (!show && isDockVisible) {
            bottomDock.animate().translationY(bottomDock.getHeight() + 150).setDuration(300).withEndAction(() -> isDockVisible = false).start();
        }
    }



    private void showNetworkStatsDialog() {
        com.example.mapmemories.systemHelpers.VibratorHelper.vibrate(this, 30);
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        View activityRootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && activityRootView != null) {
            activityRootView.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(20f, 20f, android.graphics.Shader.TileMode.MIRROR));
        }

        android.widget.FrameLayout rootLayout = new android.widget.FrameLayout(this);
        rootLayout.setBackgroundColor(android.graphics.Color.parseColor("#B3000000"));
        rootLayout.setAlpha(0f);

        com.google.android.material.card.MaterialCardView cardView = new com.google.android.material.card.MaterialCardView(this);
        cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#0D1117"));
        cardView.setRadius(48f);
        cardView.setCardElevation(24f);
        cardView.setStrokeColor(android.graphics.Color.parseColor("#30363D"));
        cardView.setStrokeWidth(3);

        android.widget.FrameLayout.LayoutParams cardParams = new android.widget.FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = android.view.Gravity.CENTER;
        cardView.setLayoutParams(cardParams);

        android.widget.LinearLayout cardContent = new android.widget.LinearLayout(this);
        cardContent.setOrientation(android.widget.LinearLayout.VERTICAL);
        cardContent.setPadding(48, 64, 48, 64);

        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("SYS // NETWORK_TELEMETRY");
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        title.setTextColor(android.graphics.Color.parseColor("#58A6FF"));
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 0, 0, 32);
        cardContent.addView(title);

        android.widget.TextView tvSpeed = new android.widget.TextView(this);
        tvSpeed.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        tvSpeed.setTextSize(18f);
        tvSpeed.setGravity(android.view.Gravity.CENTER);
        tvSpeed.setPadding(0, 0, 0, 24);
        cardContent.addView(tvSpeed);

        View graphView = new View(this) {
            private final android.graphics.Paint paintRx = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            private final android.graphics.Paint paintTx = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            private final android.graphics.Paint paintGrid = new android.graphics.Paint();
            private final android.graphics.Path pathRx = new android.graphics.Path();
            private final android.graphics.Path pathTx = new android.graphics.Path();

            public final java.util.LinkedList<Float> rxData = new java.util.LinkedList<>();
            public final java.util.LinkedList<Float> txData = new java.util.LinkedList<>();
            private final int MAX_POINTS = 50;
            public float maxSpeed = 100f;

            {
                paintRx.setColor(android.graphics.Color.parseColor("#39FF14"));
                paintRx.setStyle(android.graphics.Paint.Style.STROKE);
                paintRx.setStrokeWidth(5f);
                paintRx.setStrokeJoin(android.graphics.Paint.Join.ROUND);

                paintTx.setColor(android.graphics.Color.parseColor("#FF007F"));
                paintTx.setStyle(android.graphics.Paint.Style.STROKE);
                paintTx.setStrokeWidth(5f);
                paintTx.setStrokeJoin(android.graphics.Paint.Join.ROUND);

                paintGrid.setColor(android.graphics.Color.parseColor("#1AFFFFFF"));
                paintGrid.setStyle(android.graphics.Paint.Style.STROKE);
                paintGrid.setStrokeWidth(2f);

                for (int i = 0; i < MAX_POINTS; i++) { rxData.add(0f); txData.add(0f); }
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth(); int h = getHeight();

                for (int i = 0; i <= 4; i++) {
                    float y = h * (i / 4f);
                    canvas.drawLine(0, y, w, y, paintGrid);
                }
                for (int i = 0; i <= 6; i++) {
                    float x = w * (i / 6f);
                    canvas.drawLine(x, 0, x, h, paintGrid);
                }

                pathRx.reset(); pathTx.reset();
                float dx = w / (float) (MAX_POINTS - 1);

                for (int i = 0; i < MAX_POINTS; i++) {
                    float x = i * dx;
                    float displayMax = Math.max(maxSpeed, 1024f);
                    float yRx = h - (rxData.get(i) / displayMax) * (h * 0.9f);
                    float yTx = h - (txData.get(i) / displayMax) * (h * 0.9f);

                    yRx = Math.max(10, Math.min(yRx, h));
                    yTx = Math.max(10, Math.min(yTx, h));

                    if (i == 0) { pathRx.moveTo(x, yRx); pathTx.moveTo(x, yTx); }
                    else { pathRx.lineTo(x, yRx); pathTx.lineTo(x, yTx); }
                }
                canvas.drawPath(pathRx, paintRx);
                canvas.drawPath(pathTx, paintTx);
            }
        };
        graphView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 350));
        cardContent.addView(graphView);

        android.widget.TextView tvDetails = new android.widget.TextView(this);
        tvDetails.setTypeface(android.graphics.Typeface.MONOSPACE);
        tvDetails.setTextColor(android.graphics.Color.parseColor("#8B949E"));
        tvDetails.setTextSize(13f);
        tvDetails.setPadding(0, 32, 0, 0);
        tvDetails.setLineSpacing(0f, 1.3f);
        cardContent.addView(tvDetails);

        cardView.addView(cardContent);
        rootLayout.addView(cardView);
        dialog.setContentView(rootLayout);

        rootLayout.animate().alpha(1f).setDuration(200).start();
        cardView.setScaleX(0.8f); cardView.setScaleY(0.8f);
        cardView.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)).start();

        int uid = android.os.Process.myUid();
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

        Runnable updateRunnable = new Runnable() {
            long lastTotalRxBytes = TrafficStats.getTotalRxBytes();
            long lastTotalTxBytes = TrafficStats.getTotalTxBytes();
            long lastTotalRxPkts = TrafficStats.getTotalRxPackets();
            long lastTotalTxPkts = TrafficStats.getTotalTxPackets();
            long lastTime = System.currentTimeMillis();

            float smoothRx = 0f;
            float smoothTx = 0f;

            @Override
            public void run() {
                if (!dialog.isShowing()) return;

                long now = System.currentTimeMillis();

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = cm != null ? cm.getActiveNetworkInfo() : null;
                boolean isOnline = netInfo != null && netInfo.isConnected();

                long currentTotalRxBytes = TrafficStats.getTotalRxBytes();
                long currentTotalTxBytes = TrafficStats.getTotalTxBytes();
                long currentTotalRxPkts = TrafficStats.getTotalRxPackets();
                long currentTotalTxPkts = TrafficStats.getTotalTxPackets();

                long appRxBytes = TrafficStats.getUidRxBytes(uid);
                long appTxBytes = TrafficStats.getUidTxBytes(uid);

                float timeDiff = (now - lastTime) / 1000f;
                float rawRxSpeed = 0, rawTxSpeed = 0;
                float rxPps = 0, txPps = 0;

                if (isOnline && timeDiff > 0) {
                    rawRxSpeed = Math.max(0, (currentTotalRxBytes - lastTotalRxBytes) / timeDiff);
                    rawTxSpeed = Math.max(0, (currentTotalTxBytes - lastTotalTxBytes) / timeDiff);
                    rxPps = Math.max(0, (currentTotalRxPkts - lastTotalRxPkts) / timeDiff);
                    txPps = Math.max(0, (currentTotalTxPkts - lastTotalTxPkts) / timeDiff);
                }

                if (rawRxSpeed > 0) smoothRx = (smoothRx * 0.5f) + (rawRxSpeed * 0.5f);
                else smoothRx = smoothRx * 0.85f;

                if (rawTxSpeed > 0) smoothTx = (smoothTx * 0.5f) + (rawTxSpeed * 0.5f);
                else smoothTx = smoothTx * 0.85f;

                if (!isOnline) { smoothRx = 0; smoothTx = 0; rxPps = 0; txPps = 0; }

                if (isOnline) {
                    String speedText = String.format(java.util.Locale.US, "<font color='#39FF14'>↓ %s/s</font> | <font color='#FF007F'>↑ %s/s</font>",
                            formatBytes(smoothRx), formatBytes(smoothTx));
                    tvSpeed.setText(android.text.Html.fromHtml(speedText, android.text.Html.FROM_HTML_MODE_LEGACY));
                } else {
                    tvSpeed.setText(android.text.Html.fromHtml("<font color='#FF5252'>[ NETWORK OFFLINE ]</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
                }

                String details = String.format(java.util.Locale.US,
                        "DEVICE PACKETS/s : ↓ %-5.0f | ↑ %.0f\n" +
                                "APP TOTAL RX     : %s\n" +
                                "APP TOTAL TX     : %s",
                        rxPps, txPps, formatBytes(appRxBytes), formatBytes(appTxBytes));
                tvDetails.setText(details);

                try {
                    java.lang.reflect.Field rxDataField = graphView.getClass().getField("rxData");
                    java.lang.reflect.Field txDataField = graphView.getClass().getField("txData");
                    java.util.LinkedList<Float> rxList = (java.util.LinkedList<Float>) rxDataField.get(graphView);
                    java.util.LinkedList<Float> txList = (java.util.LinkedList<Float>) txDataField.get(graphView);

                    rxList.removeFirst(); rxList.add(smoothRx);
                    txList.removeFirst(); txList.add(smoothTx);

                    float currentMax = 1024f;
                    for (Float val : rxList) if (val > currentMax) currentMax = val;
                    for (Float val : txList) if (val > currentMax) currentMax = val;

                    graphView.getClass().getField("maxSpeed").set(graphView, currentMax);
                    graphView.invalidate();
                } catch (Exception e) { e.printStackTrace(); }

                lastTotalRxBytes = currentTotalRxBytes; lastTotalTxBytes = currentTotalTxBytes;
                lastTotalRxPkts = currentTotalRxPkts; lastTotalTxPkts = currentTotalTxPkts;
                lastTime = now;

                handler.postDelayed(this, 300);
            }

            private String formatBytes(float bytes) {
                if (bytes < 1024) return String.format(java.util.Locale.US, "%3.0f B ", bytes);
                else if (bytes < 1024 * 1024) return String.format(java.util.Locale.US, "%5.1f KB", bytes / 1024f);
                else return String.format(java.util.Locale.US, "%5.2f MB", bytes / (1024f * 1024f));
            }
        };
        handler.post(updateRunnable);

        Runnable closeDialog = () -> {
            rootLayout.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && activityRootView != null) {
                    activityRootView.setRenderEffect(null);
                }
                dialog.dismiss();
            }).start();
        };

        rootLayout.setOnClickListener(v -> closeDialog.run());
        dialog.setOnCancelListener(d -> closeDialog.run());
        dialog.show();
    }

    private void observeUnreadMessages() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalUnreadCount = 0;

                for (DataSnapshot chatSnap : snapshot.getChildren()) {
                    String chatId = chatSnap.getKey();

                    if (chatId != null && chatId.contains(currentUserId)) {
                        DataSnapshot messagesSnap = chatSnap.child("messages");

                        for (DataSnapshot msgSnap : messagesSnap.getChildren()) {
                            ChatMessage msg = msgSnap.getValue(ChatMessage.class);
                            if (msg != null &&
                                    msg.getReceiverId() != null &&
                                    msg.getReceiverId().equals(currentUserId) &&
                                    !msg.isRead()) {

                                if (msg.getDeletedBy() == null || !msg.getDeletedBy().equals(currentUserId)) {
                                    totalUnreadCount++;
                                }
                            }
                        }
                    }
                }

                updateChatsTabBadge(totalUnreadCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupConnectionObserver() {
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);

                if (connected) {
                    appTitle.setText("Соединение...");
                    appTitle.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_primary));

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!isDestroyed()) {
                            appTitle.setText("GhostNet");
                            appTitle.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_primary));
                        }
                    }, 1200);

                } else {
                    appTitle.setText("Ожидание сети...");
                    appTitle.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text_primary));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        connectedRef.addValueEventListener(connectionListener);
    }

    private void updateChatsTabBadge(int unreadCount) {
        TabLayout.Tab chatTab = tabLayout.getTabAt(1);

        if (chatTab != null) {
            if (unreadCount > 0) {
                BadgeDrawable badge = chatTab.getOrCreateBadge();
                badge.setNumber(unreadCount);
                badge.setBackgroundColor(ContextCompat.getColor(this, R.color.accent));
                badge.setBadgeTextColor(ContextCompat.getColor(this, R.color.primary));
                badge.setVisible(true);
            } else {
                chatTab.removeBadge();
            }
        }
    }

    private void setupViewPager() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Лента" : "Чаты");
        }).attach();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedRef != null && connectionListener != null) {
            connectedRef.removeEventListener(connectionListener);
        }
    }

    private void setupClickListeners() {
        profileButton.setOnClickListener(v -> {
            VibratorHelper.vibrate(this, 50);
            startActivityWithAnimation(Profile.class, v);
        });

        fabSettings.setOnClickListener(v -> {
            VibratorHelper.vibrate(this, 50);
            startActivityWithAnimation(Setting.class, v);
        });

        fabAdd.setOnClickListener(v -> {
            VibratorHelper.vibrate(this, 50);
            if (fabAddIcon.getDrawable() instanceof Animatable) {
                ((Animatable) fabAddIcon.getDrawable()).start();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivityWithAnimation(CreatePostActivity.class, bottomDock);
            }, 250);
        });

        offlineBadge.setOnClickListener(v -> DialogHelper.showOfflineQueue(this, null));
    }

    private void loadUserAvatar() {
        if (userRef == null) return;
        userRef.child("profileImageUrl").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !isDestroyed()) Glide.with(MainActivity.this).load(url).circleCrop().into(profileButton);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void observeOfflinePosts() {
        AppDatabase.getDatabase(this).offlinePostDao().getAllPostsLive().observe(this, posts -> {
            offlineBadge.setVisibility(posts != null && !posts.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void setupDoubleBackExit() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finishAffinity();
                } else {
                    Toast.makeText(MainActivity.this, "Нажмите еще раз для выхода", Toast.LENGTH_SHORT).show();
                    backPressedTime = System.currentTimeMillis();
                }
            }
        });
    }

    private void startActivityWithAnimation(Class<?> targetActivity, View sourceView) {
        int[] location = new int[2];
        sourceView.getLocationOnScreen(location);
        int revealX = location[0] + sourceView.getWidth() / 2;
        int revealY = location[1] + sourceView.getHeight() / 2;

        Intent intent = new Intent(this, targetActivity);
        intent.putExtra("revealX", revealX);
        intent.putExtra("revealY", revealY);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) { super(fragmentActivity); }
        @NonNull @Override public Fragment createFragment(int position) {
            return position == 0 ? new LentaFragment() : new ChatListFragment();
        }
        @Override public int getItemCount() { return 2; }
    }

    @Override protected void onResume() { super.onResume(); updateStatus("online"); }
    @Override protected void onPause() { super.onPause(); updateStatus(System.currentTimeMillis()); }
    private void updateStatus(Object status) { if (userRef != null) userRef.child("status").setValue(status); }
}
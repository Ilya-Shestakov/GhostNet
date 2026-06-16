package com.example.mapmemories.Chats;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mapmemories.Chats.ChatLockSetupActivity;
import com.example.mapmemories.Profile.User;
import com.example.mapmemories.Profile.UsersAdapter;
import com.example.mapmemories.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class ChatListFragment extends Fragment {

    private RecyclerView chatsRecyclerView, globalRecyclerView;
    private EditText searchInput;
    private TextView emptyChatsText, localChatsTitle;
    private LinearLayout globalSearchContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView chatScrollView;

    private ChatListAdapter chatListAdapter;
    private UsersAdapter globalUsersAdapter;

    private List<ChatListItem> allChatListItems = new ArrayList<>();
    private List<ChatListItem> filteredChatListItems = new ArrayList<>();
    private List<User> globalUserList = new ArrayList<>();

    private Map<String, Long> pinnedChatsMap = new HashMap<>();

    private ChatListItem pendingBlockItem;

    private DatabaseReference chatsRef, usersRef, myPinnedRef;
    private String currentUserId;

    private boolean isDragging = false;
    private boolean pendingUpdate = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return view;

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        myPinnedRef = usersRef.child(currentUserId).child("pinnedChats");

        initViews(view);
        setupRecyclerViews();
        setupDragAndDrop();
        setupSearch();
        setupSwipeRefresh();

        loadLocalChatsData();

        return view;
    }

    private void initViews(View v) {
        chatsRecyclerView = v.findViewById(R.id.chatsRecyclerView);
        globalRecyclerView = v.findViewById(R.id.globalRecyclerView);
        searchInput = v.findViewById(R.id.searchInput);
        emptyChatsText = v.findViewById(R.id.emptyChatsText);
        localChatsTitle = v.findViewById(R.id.localChatsTitle);
        globalSearchContainer = v.findViewById(R.id.globalSearchContainer);
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshLayout);
        chatScrollView = v.findViewById(R.id.chatScrollView);
    }

    private final ActivityResultLauncher<Intent> lockSetupLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && pendingBlockItem != null) {
                    applyBlockToggle(pendingBlockItem);
                    pendingBlockItem = null;
                }
            });

    private void setupRecyclerViews() {
        chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatListAdapter = new ChatListAdapter(getContext(), filteredChatListItems, currentUserId, new ChatListAdapter.OnChatInteractionListener() {
            @Override
            public void onChatClick(ChatListItem item) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("targetUserId", item.user.getId());
                startActivity(intent);

                if (getActivity() != null) {
                    getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }

            }

            @Override
            public void onChatLongClick(ChatListItem item, View anchorView) {
                showContextMenu(item, anchorView);
            }
        });
        chatsRecyclerView.setAdapter(chatListAdapter);

        globalRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        globalUsersAdapter = new UsersAdapter(getContext(), globalUserList, user -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("targetUserId", user.getId());
            startActivity(intent);
        });
        globalRecyclerView.setAdapter(globalUsersAdapter);
    }

    private void showContextMenu(ChatListItem item, View anchorView) {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_chat_options, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10f);
        popupWindow.setOutsideTouchable(true);

        TextView btnPin = popupView.findViewById(R.id.btnPopupPin);
        TextView btnBlock = popupView.findViewById(R.id.btnPopupBlock);
        TextView btnDelete = popupView.findViewById(R.id.btnPopupDelete);

        btnPin.setText(item.isPinned ? "Открепить" : "Закрепить");

        // Кнопка блокировки
        btnBlock.setText(item.isBlocked() ? "Разблокировать" : "Заблокировать");
        btnBlock.setOnClickListener(v -> {
            popupWindow.dismiss();
            toggleChatBlock(item);
        });

        // Кнопка "Закрепить"
        btnPin.setOnClickListener(v -> {
            popupWindow.dismiss();
            if (item.isPinned) {
                myPinnedRef.child(item.user.getId()).removeValue();
            } else {
                myPinnedRef.child(item.user.getId()).setValue(-System.currentTimeMillis());
            }
        });

        // Кнопка "Удалить"
        btnDelete.setOnClickListener(v -> {
            popupWindow.dismiss();
            chatsRef.child(item.chatId).removeValue();
            FirebaseDatabase.getInstance().getReference("chats").child(item.chatId).removeValue();
            new Thread(() -> {
                com.example.mapmemories.database.AppDatabase.getDatabase(getContext())
                        .localMessageDao().deleteMessagesByChatId(item.chatId);
                getActivity().runOnUiThread(() -> {
                    allChatListItems.remove(item);
                    updateLocalFilter(searchInput.getText().toString());
                });
            }).start();
        });

        checkAndClearLockIfNeeded();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int xOffset = 120;
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0] + xOffset, location[1] + (anchorView.getHeight() / 2));

        ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (popupWindow.isShowing()) {
                    int[] newLoc = new int[2];
                    anchorView.getLocationOnScreen(newLoc);
                    popupWindow.update(newLoc[0] + xOffset, newLoc[1] + (anchorView.getHeight() / 2), -1, -1);
                }
                return true;
            }
        };
        anchorView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        popupWindow.setOnDismissListener(() -> anchorView.getViewTreeObserver().removeOnPreDrawListener(preDrawListener));
    }

    private void toggleChatBlock(ChatListItem item) {
        SharedPreferences lockPrefs = requireContext().getSharedPreferences("chat_lock", Context.MODE_PRIVATE);
        String passwordHash = lockPrefs.getString("password_hash", null);

        if (passwordHash == null) {
            pendingBlockItem = item;
            Intent intent = new Intent(getActivity(), ChatLockSetupActivity.class);
            lockSetupLauncher.launch(intent);
        } else {
            applyBlockToggle(item);
        }
    }



    private void applyBlockToggle(ChatListItem item) {
        if (item.isBlocked()) {
            // Если чат заблокирован, для разблокировки нужен пароль
            showUnlockForBlock(item);
        } else {
            // Блокировка – просто переключаем
            doApplyBlockToggle(item);
        }
    }

    private void doApplyBlockToggle(ChatListItem item) {
        DatabaseReference blockRef = FirebaseDatabase.getInstance()
                .getReference("chats").child(item.chatId).child("blockedBy").child(currentUserId);

        boolean newBlockedState = !item.isBlocked();
        if (newBlockedState) {
            blockRef.setValue(true);
        } else {
            blockRef.removeValue();
        }
        item.setBlocked(newBlockedState);
        updateLocalFilter(searchInput.getText().toString());

        // Если после этого не осталось заблокированных чатов – сбрасываем пароль
        checkAndClearLockIfNeeded();
    }

    private void showUnlockForBlock(ChatListItem item) {
        SharedPreferences lockPrefs = requireContext().getSharedPreferences("chat_lock", Context.MODE_PRIVATE);
        String passwordHash = lockPrefs.getString("password_hash", null);

        if (passwordHash == null) {
            // Пароля нет (не должно случаться), просто разблокируем
            doApplyBlockToggle(item);
            return;
        }

        boolean useBiometric = lockPrefs.getBoolean("use_biometric", false);

        if (useBiometric && BiometricManager.from(requireContext())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS) {
            // Биометрия
            Executor executor = ContextCompat.getMainExecutor(requireContext());
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    doApplyBlockToggle(item);
                }

                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(requireContext(), "Не удалось распознать", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    // При ошибке биометрии просим пароль
                    showPasswordForUnblock(item);
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Разблокировка чата")
                    .setSubtitle("Подтвердите личность для снятия блокировки")
                    .setNegativeButtonText("Ввести пароль")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            // Пароль
            showPasswordForUnblock(item);
        }
    }

    private void showPasswordForUnblock(ChatListItem item) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Блюр фона
        View activityRootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) {
            activityRootView.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR));
        }

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_password_unlock, null);

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        title.setText("Разблокировка чата");
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        message.setText("Введите пароль, чтобы разблокировать чат");

        // Точки
        View[] dots = new View[6];
        dots[0] = dialogView.findViewById(R.id.dot1);
        dots[1] = dialogView.findViewById(R.id.dot2);
        dots[2] = dialogView.findViewById(R.id.dot3);
        dots[3] = dialogView.findViewById(R.id.dot4);
        dots[4] = dialogView.findViewById(R.id.dot5);
        dots[5] = dialogView.findViewById(R.id.dot6);

        // Клавиатура
        GridLayout grid = dialogView.findViewById(R.id.keyboardGrid);
        StringBuilder passwordBuilder = new StringBuilder();
        String[] keys = {"1","2","3","4","5","6","7","8","9","","0","⌫"};

        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            TextView btn = new TextView(activity);
            btn.setText(key);
            btn.setTextSize(24f);
            btn.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
            btn.setGravity(Gravity.CENTER);
            btn.setBackgroundResource(R.drawable.key_bg);
            btn.setClickable(true);
            btn.setFocusable(true);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(activity, 80);
            params.height = dpToPx(activity, 80);
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
                    if (checkPassword(passwordBuilder.toString())) {
                        dialog.dismiss();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) {
                            activityRootView.setRenderEffect(null);
                        }
                        doApplyBlockToggle(item);
                    } else {
                        Toast.makeText(activity, "Неверный пароль", Toast.LENGTH_SHORT).show();
                        passwordBuilder.setLength(0);
                        for (View dot : dots) dot.setBackgroundResource(R.drawable.dot_inactive);
                    }
                }
            });

            grid.addView(btn);
        }

        dialog.setContentView(dialogView);
        dialog.setCancelable(true); // можно закрыть кнопкой "Назад"
        dialog.setCanceledOnTouchOutside(true); // можно закрыть, тапнув мимо
        dialog.setOnDismissListener(d -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activityRootView != null) {
                activityRootView.setRenderEffect(null);
            }
        });
        dialog.show();
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private boolean checkPassword(String input) {
        SharedPreferences lockPrefs = requireContext().getSharedPreferences("chat_lock", Context.MODE_PRIVATE);
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

    private void checkAndClearLockIfNeeded() {
        boolean anyBlocked = false;
        for (ChatListItem item : allChatListItems) {
            if (item.isBlocked()) {
                anyBlocked = true;
                break;
            }
        }
        if (!anyBlocked) {
            requireContext().getSharedPreferences("chat_lock", Context.MODE_PRIVATE)
                    .edit().clear().apply();
        }
    }

    private void setupDragAndDrop() {
        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                List<ChatListItem> currentList = chatListAdapter.getItems();
                if (position >= 0 && position < currentList.size()) {
                    if (!currentList.get(position).isPinned) {
                        return makeMovementFlags(0, 0);
                    }
                }
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                List<ChatListItem> currentList = chatListAdapter.getItems();
                if (currentList.get(fromPos).isPinned && currentList.get(toPos).isPinned) {
                    chatListAdapter.swapItems(fromPos, toPos);
                    return true;
                }
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    isDragging = true;
                    if (viewHolder != null) {
                        viewHolder.itemView.setScaleX(1.02f);
                        viewHolder.itemView.setScaleY(1.02f);
                    }
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setScaleX(1f);
                viewHolder.itemView.setScaleY(1f);

                savePinnedOrderToFirebase();

                isDragging = false;
                if (pendingUpdate) {
                    pendingUpdate = false;
                    updateLocalFilter(searchInput.getText().toString());
                }
            }
        };

        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(chatsRecyclerView);
    }

    private void savePinnedOrderToFirebase() {
        List<ChatListItem> list = chatListAdapter.getItems();
        long orderIndex = 0;
        for (ChatListItem item : list) {
            if (item.isPinned) {
                myPinnedRef.child(item.user.getId()).setValue(orderIndex++);
            }
        }
    }

    private void loadLocalChatsData() {
        myPinnedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pinnedChatsMap.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Object val = ds.getValue();
                    long order = (val instanceof Number) ? ((Number)val).longValue() : 0L;
                    pinnedChatsMap.put(ds.getKey(), order);
                }
                for (ChatListItem item : allChatListItems) {
                    if (pinnedChatsMap.containsKey(item.user.getId())) {
                        item.isPinned = true;
                        item.pinnedOrder = pinnedChatsMap.get(item.user.getId());
                    } else {
                        item.isPinned = false;
                        item.pinnedOrder = 0L;
                    }
                }
                if (!isDragging) updateLocalFilter(searchInput.getText().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allChatListItems.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String chatId = ds.getKey();
                    if (chatId != null && chatId.contains(currentUserId)) {
                        String otherUserId = chatId.replace(currentUserId, "").replace("_", "");
                        fetchChatDetails(chatId, otherUserId, ds.child("messages"));
                    }
                }
                if (allChatListItems.isEmpty() && !isDragging) {
                    updateLocalFilter(searchInput.getText().toString());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchChatDetails(String chatId, String otherUserId, DataSnapshot messagesSnapshot) {

        if (currentUserId == null) return;

        ChatMessage lastMsg = null;
        int unreadCount = 0;

        for (DataSnapshot msgSnap : messagesSnapshot.getChildren()) {
            try {
                ChatMessage msg = msgSnap.getValue(ChatMessage.class);

                // Проверяем всё на null перед сравнением
                if (msg != null) {
                    String deletedBy = msg.getDeletedBy();

                    // Безопасная проверка: не удалено ли сообщение нами
                    if (deletedBy == null || !deletedBy.equals(currentUserId)) {
                        lastMsg = msg;

                        // Безопасный подсчет непрочитанных
                        String receiverId = msg.getReceiverId();
                        if (currentUserId != null && currentUserId.equals(msg.getReceiverId()) && !msg.isRead()) {
                            unreadCount++;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("CHAT_LIST_ERR", "Ошибка парсинга сообщения: " + e.getMessage());
            }
        }

        final ChatMessage finalLastMsg = lastMsg;
        final int finalUnreadCount = unreadCount;

        // Проверяем, что ID собеседника не null
        if (otherUserId == null) return;

        usersRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                User user = userSnap.getValue(User.class);
                if (user != null) {
                    user.setId(otherUserId);
                    ChatListItem item = new ChatListItem(chatId, user);
                    item.lastMessage = finalLastMsg;
                    item.unreadCount = finalUnreadCount;

                    // Проверяем, заблокирован ли чат текущим пользователем
                    FirebaseDatabase.getInstance().getReference("chats")
                            .child(chatId).child("blockedBy").child(currentUserId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    boolean blocked = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue());
                                    item.setBlocked(blocked);
                                    updateLocalFilter(searchInput.getText().toString()); // перерисовать список
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });

                    if (pinnedChatsMap.containsKey(otherUserId)) {
                        item.isPinned = true;
                        item.pinnedOrder = pinnedChatsMap.get(otherUserId);
                    } else {
                        item.isPinned = false;
                        item.pinnedOrder = 0L;
                    }

                    addAndUpdateList(item);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private synchronized void addAndUpdateList(ChatListItem newItem) {
        allChatListItems.removeIf(item -> item.chatId.equals(newItem.chatId));
        allChatListItems.add(newItem);
        if (isDragging) pendingUpdate = true;
        else updateLocalFilter(searchInput.getText().toString());
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                updateLocalFilter(query);

                if (TextUtils.isEmpty(query)) {
                    globalSearchContainer.setVisibility(View.GONE);
                    localChatsTitle.setVisibility(View.GONE);
                } else {
                    localChatsTitle.setVisibility(View.VISIBLE);
                    performGlobalSearch(query);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chatListAdapter != null) {
            chatListAdapter.notifyDataSetChanged();
        }
    }

    private void updateLocalFilter(String query) {
        filteredChatListItems.clear();
        if (TextUtils.isEmpty(query)) {
            filteredChatListItems.addAll(allChatListItems);
        } else {
            for (ChatListItem item : allChatListItems) {
                if (item.user.getUsername() != null && item.user.getUsername().toLowerCase().contains(query)) {
                    filteredChatListItems.add(item);
                }
            }
        }

        Collections.sort(filteredChatListItems, (a, b) -> {
            if (a.isPinned && !b.isPinned) return -1;
            if (!a.isPinned && b.isPinned) return 1;

            if (a.isPinned && b.isPinned) {
                return Long.compare(a.pinnedOrder, b.pinnedOrder);
            }

            long timeA = a.lastMessage != null ? a.lastMessage.getTimestamp() : 0;
            long timeB = b.lastMessage != null ? b.lastMessage.getTimestamp() : 0;
            return Long.compare(timeB, timeA);
        });

        chatListAdapter.setChats(filteredChatListItems);
        emptyChatsText.setVisibility(filteredChatListItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void performGlobalSearch(String searchText) {
        Query query = usersRef.orderByChild("username").startAt(searchText).endAt(searchText + "\uf8ff").limitToFirst(15);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                globalUserList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.setId(ds.getKey());
                        if (!user.getId().equals(currentUserId)) globalUserList.add(user);
                    }
                }
                globalSearchContainer.setVisibility(globalUserList.isEmpty() ? View.GONE : View.VISIBLE);
                globalUsersAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.accent);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadLocalChatsData();
            swipeRefreshLayout.setRefreshing(false);
        });
    }
}
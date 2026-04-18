package com.example.mapmemories.Chats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mapmemories.R;

public class MessageSwipeController extends ItemTouchHelper.Callback {

    private final Context context;
    private final SwipeControllerActions buttonsActions;
    private Drawable replyIcon;

    private boolean replyTriggered = false;
    private boolean hapticPlayed = false;

    // Константы упругости
    private static final float MAX_SWIPE_DISTANCE = 250f; // Максимум, на сколько пикселей можно оттянуть
    private static final float TRIGGER_DISTANCE = 150f;   // Дистанция, после которой срабатывает "Ответить"

    public interface SwipeControllerActions {
        void onReplyClicked(int position);
    }

    public MessageSwipeController(Context context, SwipeControllerActions buttonsActions) {
        this.context = context;
        this.buttonsActions = buttonsActions;
        // Убедись, что у тебя есть какая-нибудь иконка со стрелочкой влево
        this.replyIcon = ContextCompat.getDrawable(context, R.drawable.ic_refresh); // или ic_reply
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Разрешаем тянуть сообщение только ВЛЕВО (для ответа)
        return makeMovementFlags(0, ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // Мы не перемещаем элементы вверх/вниз
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Этот метод никогда не вызовется, так как мы блокируем полное смахивание
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Делаем порог смахивания недостижимым, чтобы элемент всегда отпружинивал назад
        return 2.0f;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        // Игнорируем резкие свайпы, чтобы они случайно не удалили элемент
        return defaultValue * 10;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            View itemView = viewHolder.itemView;

            // 1. Ограничиваем дистанцию свайпа (эффект резинки)
            float limitedDX = dX;
            if (dX < -MAX_SWIPE_DISTANCE) {
                limitedDX = -MAX_SWIPE_DISTANCE - (float) Math.log10(Math.abs(dX) - MAX_SWIPE_DISTANCE) * 20;
            }

            // 2. Логика срабатывания ответа
            if (Math.abs(limitedDX) >= TRIGGER_DISTANCE) {
                replyTriggered = true;

                // Легкая вибрация при достижении порога (как в Telegram)
                if (!hapticPlayed && isCurrentlyActive) {
                    itemView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    hapticPlayed = true;
                }
            } else {
                replyTriggered = false;
                hapticPlayed = false;
            }

            // 3. Рисуем иконку "Ответить" справа
            if (replyIcon != null && limitedDX < 0) {
                int iconMargin = (itemView.getHeight() - replyIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + (itemView.getHeight() - replyIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + replyIcon.getIntrinsicHeight();

                // Высчитываем появление иконки
                int iconLeft = itemView.getRight() + (int) limitedDX + iconMargin;
                int iconRight = iconLeft + replyIcon.getIntrinsicWidth();

                // Если иконка не вылезла за край экрана
                if (iconRight > itemView.getRight() - 20) {
                    iconRight = itemView.getRight() - 20;
                    iconLeft = iconRight - replyIcon.getIntrinsicWidth();
                }

                replyIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                // Плавное появление (Alpha)
                int alpha = (int) (Math.min(Math.abs(limitedDX) / TRIGGER_DISTANCE, 1.0f) * 255);
                replyIcon.setAlpha(alpha);
                replyIcon.draw(c);
            }

            // 4. Сдвигаем саму плашку сообщения
            super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive);

            // 5. Обработка отпускания пальца
            if (!isCurrentlyActive && replyTriggered) {
                // Защита от двойного срабатывания
                replyTriggered = false;
                if (buttonsActions != null) {
                    buttonsActions.onReplyClicked(viewHolder.getAdapterPosition());
                }
            }
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        hapticPlayed = false;
        replyTriggered = false;
    }
}
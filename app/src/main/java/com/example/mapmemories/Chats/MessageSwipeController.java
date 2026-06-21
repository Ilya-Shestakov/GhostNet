package com.example.mapmemories.Chats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mapmemories.R;

public class MessageSwipeController extends ItemTouchHelper.Callback {

    private final Context context;
    private final SwipeControllerActions buttonsActions;
    private final SwipeCondition swipeCondition;   // новое поле
    private Drawable replyIcon;

    private boolean replyTriggered = false;
    private boolean hapticPlayed = false;

    private static final float MAX_SWIPE_DISTANCE = 250f;
    private static final float TRIGGER_DISTANCE = 150f;

    public interface SwipeControllerActions {
        void onReplyClicked(int position);
    }

    public interface SwipeCondition {
        boolean canSwipe(int position);
    }

    public MessageSwipeController(Context context, SwipeControllerActions buttonsActions, SwipeCondition swipeCondition) {
        this.context = context;
        this.buttonsActions = buttonsActions;
        this.swipeCondition = swipeCondition;
        this.replyIcon = ContextCompat.getDrawable(context, R.drawable.ic_refresh);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Запрещаем свайп, если условие не позволяет
        if (swipeCondition != null && !swipeCondition.canSwipe(viewHolder.getAbsoluteAdapterPosition())) {
            return 0;  // никаких движений
        }
        return makeMovementFlags(0, ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // не используется, потому что мы не доводим до onSwiped
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 2.0f;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 10;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            View itemView = viewHolder.itemView;

            float limitedDX = dX;
            if (dX < -MAX_SWIPE_DISTANCE) {
                limitedDX = -MAX_SWIPE_DISTANCE - (float) Math.log10(Math.abs(dX) - MAX_SWIPE_DISTANCE) * 20;
            }

            if (Math.abs(limitedDX) >= TRIGGER_DISTANCE) {
                replyTriggered = true;

                if (!hapticPlayed && isCurrentlyActive) {
                    itemView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    hapticPlayed = true;
                }
            } else {
                replyTriggered = false;
                hapticPlayed = false;
            }

            if (replyIcon != null && limitedDX < 0) {
                int iconMargin = (itemView.getHeight() - replyIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + (itemView.getHeight() - replyIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + replyIcon.getIntrinsicHeight();

                int iconLeft = itemView.getRight() + (int) limitedDX + iconMargin;
                int iconRight = iconLeft + replyIcon.getIntrinsicWidth();

                if (iconRight > itemView.getRight() - 20) {
                    iconRight = itemView.getRight() - 20;
                    iconLeft = iconRight - replyIcon.getIntrinsicWidth();
                }

                replyIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                int alpha = (int) (Math.min(Math.abs(limitedDX) / TRIGGER_DISTANCE, 1.0f) * 255);
                replyIcon.setAlpha(alpha);
                replyIcon.draw(c);
            }

            super.onChildDraw(c, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive);

            if (!isCurrentlyActive && replyTriggered) {
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
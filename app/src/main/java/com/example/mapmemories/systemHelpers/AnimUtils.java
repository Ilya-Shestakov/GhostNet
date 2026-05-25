package com.example.mapmemories.systemHelpers;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

public class AnimUtils {

    public static void animateLike(ImageView likeIcon, boolean isLiked) {
        likeIcon.setScaleX(1f);
        likeIcon.setScaleY(1f);

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(likeIcon, "scaleX", 0.8f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(likeIcon, "scaleY", 0.8f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(likeIcon, "scaleX", 1.2f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(likeIcon, "scaleY", 1.2f);
        scaleUpX.setInterpolator(new OvershootInterpolator(4f));
        scaleUpY.setInterpolator(new OvershootInterpolator(4f));
        scaleUpX.setDuration(300);
        scaleUpY.setDuration(300);

        ObjectAnimator scaleNormalX = ObjectAnimator.ofFloat(likeIcon, "scaleX", 1f);
        ObjectAnimator scaleNormalY = ObjectAnimator.ofFloat(likeIcon, "scaleY", 1f);
        scaleNormalX.setDuration(100);
        scaleNormalY.setDuration(100);

        AnimatorSet animatorSet = new AnimatorSet();

        animatorSet.play(scaleDownX).with(scaleDownY);
        animatorSet.play(scaleUpX).with(scaleUpY).after(scaleDownX);
        animatorSet.play(scaleNormalX).with(scaleNormalY).after(scaleUpX);

        animatorSet.start();
    }
}
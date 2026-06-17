package com.example.mapmemories.Info;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mapmemories.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SecurityInfoActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private MaterialButton btnNext, btnBack;
    private int currentPage = 0;

    private final List<Slide> slides = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_info);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        btnNext = findViewById(R.id.btnNext);
        btnBack = findViewById(R.id.btnBack);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupSlides();
        viewPager.setAdapter(new SlideAdapter(slides));

        // Добавляем эффект перелистывания
        viewPager.setPageTransformer(new DepthPageTransformer());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateButtons();
                updateDots();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < slides.size() - 1) {
                viewPager.setCurrentItem(currentPage + 1, true);
            } else {
                finish();
            }
        });

        btnBack.setOnClickListener(v -> {
            if (currentPage > 0) {
                viewPager.setCurrentItem(currentPage - 1, true);
            }
        });

        createDots();
        updateButtons();
        updateDots();
    }

    private void setupSlides() {
        slides.add(new Slide(
                R.drawable.ic_lock,  // Генерация ключей
                "Генерация ключей",
                "При первом входе на устройстве создаётся уникальная пара RSA-ключей (4096 бит). Приватный ключ хранится в аппаратном Android KeyStore и никогда не покидает устройство. Публичный ключ отправляется в Firebase."
        ));
        slides.add(new Slide(
                R.drawable.ic_chat,  // Шифрование сообщения
                "Шифрование сообщения",
                "Каждое сообщение шифруется случайным AES-256 ключом в режиме GCM. Затем этот ключ шифруется публичным RSA-ключом получателя. Только получатель может расшифровать сообщение."
        ));
        slides.add(new Slide(
                R.drawable.ic_file,  // Хранение данных
                "Хранение данных",
                "Все сообщения в Firebase хранятся в зашифрованном виде. Даже при компрометации базы злоумышленник увидит только ENC_V3:... Прочитать их можно только с приватным ключом на вашем устройстве."
        ));
        slides.add(new Slide(
                R.drawable.ic_profile_placeholder,  // Защита сессий
                "Защита сессий",
                "Каждое устройство имеет уникальный ID. При входе с нового устройства старая сессия принудительно завершается. Это предотвращает параллельное использование аккаунта."
        ));
        slides.add(new Slide(
                R.drawable.ic_arrow_back,
                "Миграция без посредников",
                "Перенос данных на новое устройство происходит напрямую по локальной сети с временным шифрованием. QR-код содержит только публичный ключ и IP, никакие данные не идут через сервер."
        ));
    }

    private void createDots() {
        dotsLayout.removeAllViews();
        for (int i = 0; i < slides.size(); i++) {
            ImageView dot = new ImageView(this);
            dot.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(24, 24);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dotsLayout.addView(dot);
        }
    }

    private void updateDots() {
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            dot.setImageDrawable(ContextCompat.getDrawable(this,
                    i == currentPage ? R.drawable.dot_active : R.drawable.dot_inactive));
        }
    }

    private void updateButtons() {
        btnBack.setVisibility(currentPage > 0 ? View.VISIBLE : View.INVISIBLE);
        if (currentPage == slides.size() - 1) {
            btnNext.setText("Понятно");
        } else {
            btnNext.setText("Далее");
        }
    }

    static class Slide {
        int animationRes;
        String title;
        String description;

        Slide(int animationRes, String title, String description) {
            this.animationRes = animationRes;
            this.title = title;
            this.description = description;
        }
    }

    class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.SlideViewHolder> {
        private final List<Slide> slides;

        SlideAdapter(List<Slide> slides) {
            this.slides = slides;
        }

        @NonNull @Override
        public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_security_slide, parent, false);
            return new SlideViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
            Slide slide = slides.get(position);
            holder.title.setText(slide.title);
            holder.description.setText(slide.description);
            holder.image.setImageResource(slide.animationRes);

            // Запускаем простую анимацию пульсации
            holder.image.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(800)
                    .withEndAction(() -> holder.image.animate().scaleX(1f).scaleY(1f).setDuration(800).start());
        }

        @Override
        public int getItemCount() { return slides.size(); }

        class SlideViewHolder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title, description;
            SlideViewHolder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.animationView);
                title = itemView.findViewById(R.id.tvTitle);
                description = itemView.findViewById(R.id.tvDescription);
            }
        }
    }

    // Трансформация страниц для эффекта глубины
    static class DepthPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;

        @Override
        public void transformPage(@NonNull View view, float position) {
            int pageWidth = view.getWidth();
            if (position < -1) {
                view.setAlpha(0f);
            } else if (position <= 0) {
                view.setAlpha(1f);
                view.setTranslationX(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);
            } else if (position <= 1) {
                view.setAlpha(1 - position);
                view.setTranslationX(pageWidth * -position);
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            } else {
                view.setAlpha(0f);
            }
        }
    }
}
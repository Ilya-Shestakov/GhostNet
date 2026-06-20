package com.example.mapmemories.Info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mapmemories.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class SecurityInfoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FaqAdapter adapter;
    private final List<FaqItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.faqRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        items.add(new FaqItem("Как шифруются мои сообщения?", 0));
        items.add(new FaqItem("Почему нельзя зайти с двух устройств одновременно?", 1));
        items.add(new FaqItem("Почему сообщения сохраняются только при переносе по QR?", 2));

        adapter = new FaqAdapter(items);
        recyclerView.setAdapter(adapter);
    }

    // ---------------------------
    static class FaqItem {
        String question;
        int contentType;

        FaqItem(String question, int contentType) {
            this.question = question;
            this.contentType = contentType;
        }
    }

    class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.ViewHolder> {

        private final List<FaqItem> items;
        private int expandedPosition = RecyclerView.NO_POSITION;

        FaqAdapter(List<FaqItem> items) {
            this.items = items;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_faq, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FaqItem item = items.get(position);
            boolean isExpanded = (position == expandedPosition);
            holder.bind(item, isExpanded, position);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView questionText;
            FrameLayout answerContainer;
            WebView webView;
            int loadedContentType = -1;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.card);
                questionText = itemView.findViewById(R.id.questionText);
                answerContainer = itemView.findViewById(R.id.answerContainer);
            }

            void bind(FaqItem item, boolean isExpanded, int position) {
                questionText.setText(item.question);

                card.setOnClickListener(null);

                if (isExpanded) {
                    if (webView == null || loadedContentType != item.contentType) {

                        if (webView != null) {
                            answerContainer.removeView(webView);
                            webView.destroy();
                            webView = null;
                        }

                        webView = new WebView(itemView.getContext());
                        webView.setLayoutParams(new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT));
                        WebSettings ws = webView.getSettings();
                        ws.setJavaScriptEnabled(true);
                        ws.setDomStorageEnabled(true);
                        ws.setAllowFileAccess(true);
                        webView.setWebViewClient(new WebViewClient());
                        loadContent(webView, item.contentType);
                        answerContainer.addView(webView);
                        loadedContentType = item.contentType;
                    }
                    answerContainer.setVisibility(View.VISIBLE);
                } else {
                    answerContainer.setVisibility(View.GONE);

                }

                card.setOnClickListener(v -> {
                    int clickedPos = getAdapterPosition();
                    if (clickedPos == RecyclerView.NO_POSITION) return;

                    int oldPos = expandedPosition;
                    if (oldPos == clickedPos) {
                        expandedPosition = RecyclerView.NO_POSITION;
                        notifyItemChanged(clickedPos);
                    } else {
                        expandedPosition = clickedPos;
                        if (oldPos != RecyclerView.NO_POSITION) {
                            notifyItemChanged(oldPos);
                        }
                        notifyItemChanged(clickedPos);
                        recyclerView.smoothScrollToPosition(clickedPos);
                    }
                });
            }

            private void loadContent(WebView webView, int contentType) {
                switch (contentType) {
                    case 0:
                        webView.loadUrl("file:///android_res/raw/anim.html");
                        break;
                    case 1:
                        webView.loadUrl("file:///android_res/raw/anim_session.html");
                        break;
                    case 2:
                        webView.loadUrl("file:///android_res/raw/anim_qr_transfer.html");
                        break;
                }
            }
        }
    }
}
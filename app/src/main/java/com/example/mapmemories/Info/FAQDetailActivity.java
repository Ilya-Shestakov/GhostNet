package com.example.mapmemories.Info;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.mapmemories.R;

public class FAQDetailActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        webView = findViewById(R.id.webView);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());

        // добавляем интерфейс для кнопки «Понятно» в анимации
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        int index = getIntent().getIntExtra("faq_index", 0);
        loadContent(index);
    }

    private void loadContent(int index) {
        switch (index) {
            case 0:
                // анимация шифрования
                webView.loadUrl("file:///android_res/raw/anim.html");
                break;
            case 1:
                // текст про сессии
                String html1 = "<html><body style='background:#1a1a2e;color:#e0e0ff;font-family:sans-serif;padding:24px'>"
                        + "<h2>Почему нельзя зайти с двух устройств?</h2>"
                        + "<p>Каждый вход генерирует уникальный идентификатор сессии. "
                        + "При входе с нового устройства старая сессия немедленно блокируется. "
                        + "Это предотвращает параллельное использование аккаунта и защищает от перехвата данных.</p>"
                        + "</body></html>";
                webView.loadDataWithBaseURL(null, html1, "text/html", "UTF-8", null);
                break;
            case 2:
                // текст про QR-перенос
                String html2 = "<html><body style='background:#1a1a2e;color:#e0e0ff;font-family:sans-serif;padding:24px'>"
                        + "<h2>Почему перенос только по QR?</h2>"
                        + "<p>Перенос данных происходит напрямую между устройствами по локальной сети. "
                        + "QR-код содержит только временный публичный ключ и IP-адрес. "
                        + "Сами сообщения никогда не проходят через сервер, оставаясь под вашим контролем.</p>"
                        + "</body></html>";
                webView.loadDataWithBaseURL(null, html2, "text/html", "UTF-8", null);
                break;
            default:
                webView.loadUrl("about:blank");
        }
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void closeActivity() {
            finish();
        }
    }
}
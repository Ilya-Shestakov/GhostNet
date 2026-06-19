package com.example.mapmemories.Info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mapmemories.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FAQBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_INDEX = "faq_index";

    public static FAQBottomSheetFragment newInstance(int index) {
        FAQBottomSheetFragment fragment = new FAQBottomSheetFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_sheet_faq, container, false);

        WebView webView = view.findViewById(R.id.webView);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());

        // Интерфейс для закрытия Bottom Sheet по нажатию кнопки «Понятно» в HTML
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void closeBottomSheet() {
                dismiss();
            }
        }, "Android");

        int index = getArguments() != null ? getArguments().getInt(ARG_INDEX, 0) : 0;
        loadContent(webView, index);

        return view;
    }

    private void loadContent(WebView webView, int index) {
        switch (index) {
            case 0:
                // анимация шифрования (anim.html в res/raw)
                webView.loadUrl("file:///android_res/raw/anim.html");
                break;
            case 1:
                String html1 = "<html><body style='background:#1a1a2e;color:#e0e0ff;font-family:sans-serif;padding:24px'>"
                        + "<h2>Почему нельзя зайти с двух устройств?</h2>"
                        + "<p>Каждый вход генерирует уникальный идентификатор сессии. "
                        + "При входе с нового устройства старая сессия немедленно блокируется. "
                        + "Это предотвращает параллельное использование аккаунта и защищает от перехвата данных.</p>"
                        + "</body></html>";
                webView.loadDataWithBaseURL(null, html1, "text/html", "UTF-8", null);
                break;
            case 2:
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
}
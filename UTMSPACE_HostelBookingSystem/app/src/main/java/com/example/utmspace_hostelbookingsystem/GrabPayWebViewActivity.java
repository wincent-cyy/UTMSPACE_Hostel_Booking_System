package com.example.utmspace_hostelbookingsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class GrabPayWebViewActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String paymentIntentClientSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grab_pay_web_view);

        String url = getIntent().getStringExtra("PAYMENT_URL");
        paymentIntentClientSecret = getIntent().getStringExtra("CLIENT_SECRET");

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);

        setupWebView();
        webView.loadUrl(url);

        // ✅ 使用 OnBackPressedCallback 处理返回按钮
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);

                // 检查支付结果
                if (url.contains("redirect_status=succeeded") || url.contains("success")) {
                    handlePaymentResult(true);
                } else if (url.contains("redirect_status=failed") || url.contains("canceled")) {
                    handlePaymentResult(false);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("redirect_status=succeeded")) {
                    handlePaymentResult(true);
                    return true;
                } else if (url.contains("redirect_status=failed")) {
                    handlePaymentResult(false);
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });
    }

    private void handlePaymentResult(boolean success) {
        if (success) {
            Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("PAYMENT_SUCCESS", true);
            resultIntent.putExtra("CLIENT_SECRET", paymentIntentClientSecret);
            setResult(RESULT_OK, resultIntent);
        } else {
            Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
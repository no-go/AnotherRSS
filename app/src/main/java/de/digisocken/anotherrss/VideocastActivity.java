package de.digisocken.anotherrss;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.VideoView;

public class VideocastActivity extends AppCompatActivity {
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoweb);
        VideoView videoView = (VideoView) findViewById(R.id.videoView);
        WebView webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            String url = uri.toString();
            if (url.endsWith(".mp4")) {
                webView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                videoView.setMediaController(AnotherRSS.mediaController);
                videoView.setVideoURI(uri);
                videoView.requestFocus();
                videoView.start();
            } else {
                webView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.GONE);
                webView.setWebViewClient(new MyWebClient());
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);
                webView.loadUrl(url);
            }
        }
    }

    public class MyWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
            super.onPageFinished(view, url);
        }
    }
}

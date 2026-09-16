package com.coachinghisab.pro;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int STORAGE_REQUEST = 1002;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private PendingSave pendingSave;

    private static class PendingSave {
        String name, mime, base64;
        PendingSave(String name, String mime, String base64) { this.name=name; this.mime=mime; this.base64=base64; }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(91,92,226));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(245,247,251));
        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        webView.loadUrl("file:///android_asset/coaching_hisab_mobile.html");
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); }
                catch (Exception e) { filePathCallback = null; callback.onReceiveValue(null); Toast.makeText(MainActivity.this, "ফাইল বাছাই করা যাচ্ছে না", Toast.LENGTH_SHORT).show(); }
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                } catch (Exception ignored) { }
            }
        });

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidApp");
    }

    private class AndroidBridge {
        @JavascriptInterface public void saveTextFile(String fileName, String mimeType, String base64Data) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(fileName, mimeType, base64Data);
            } else {
                pendingSave = new PendingSave(fileName, mimeType, base64Data);
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
                } else saveLegacy(fileName, mimeType, base64Data);
            }
        }
    }

    private void saveWithMediaStore(String name, String mime, String base64) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, name);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Coaching Hisab Pro");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("MediaStore insert failed");
            try (OutputStream out = getContentResolver().openOutputStream(uri)) { out.write(data); }
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(uri, values, null, null);
            Toast.makeText(this, "Downloads/Coaching Hisab Pro-এ সংরক্ষণ হয়েছে", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "ফাইল সংরক্ষণ করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLegacy(String name, String mime, String base64) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/Coaching Hisab Pro");
            if (!dir.exists() && !dir.mkdirs()) throw new Exception("mkdir failed");
            File file = new File(dir, name);
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(data); }
            Toast.makeText(this, "Downloads/Coaching Hisab Pro-এ সংরক্ষণ হয়েছে", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "ফাইল সংরক্ষণ করা যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST && pendingSave != null) {
            PendingSave p = pendingSave; pendingSave = null;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) saveLegacy(p.name, p.mime, p.base64);
            else Toast.makeText(this, "Storage permission না দেওয়ায় ফাইল সংরক্ষণ হয়নি", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) results = new Uri[]{uri};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) { webView.loadUrl("about:blank"); webView.stopLoading(); webView.destroy(); }
        super.onDestroy();
    }
}

package com.solarcleaner.pro;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    // IP del ESP32 (modo punto de acceso). Cambiala aqui si algun dia cambia.
    static final String URL = "http://192.168.4.1/";
    static final String ERROR = "file:///android_asset/error.html";

    WebView w;
    ConnectivityManager cm;
    ConnectivityManager.NetworkCallback cb;
    boolean conectado = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0F172A"));
        getWindow().setNavigationBarColor(Color.parseColor("#0F172A"));

        w = new WebView(this);
        w.setBackgroundColor(Color.parseColor("#0F172A"));
        setContentView(w);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        w.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) v.loadUrl(ERROR);
            }
        });

        // Fuerza que la app use el WiFi del equipo aunque no tenga internet
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
        cb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network n) {
                conectado = true;
                cm.bindProcessToNetwork(n);
                runOnUiThread(() -> w.loadUrl(URL));
            }
            @Override
            public void onLost(Network n) {
                conectado = false;
                cm.bindProcessToNetwork(null);
            }
        };
        cm.registerNetworkCallback(req, cb);

        // Si en 2.5 s no hay WiFi, muestra la pantalla de aviso
        new Handler().postDelayed(() -> { if (!conectado) w.loadUrl(ERROR); }, 2500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String u = w.getUrl();
        if (conectado && u != null && u.startsWith("file:")) w.loadUrl(URL);
    }

    @Override
    public void onBackPressed() {
        if (w.canGoBack()) w.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        try { cm.unregisterNetworkCallback(cb); } catch (Exception ignored) {}
        super.onDestroy();
    }
}

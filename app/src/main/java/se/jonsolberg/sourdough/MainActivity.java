package se.jonsolberg.sourdough;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Hosts the bundled, fully offline Levain bread calculator (assets/index.html) in a WebView.
 * The app has no INTERNET permission; nothing is loaded from the network.
 */
public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.bg));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // saved recipes + settings (localStorage)
        s.setAllowContentAccess(false);
        s.setBlockNetworkLoads(true);          // belt and braces: never touch the network
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                // Ko-fi, GitHub and jonsolberg.se open in the phone's browser; the app itself stays offline.
                String host = u.getHost();
                if ("https".equals(u.getScheme()) && ("ko-fi.com".equals(host) || "github.com".equals(host) || "jonsolberg.se".equals(host))) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); }
                    catch (ActivityNotFoundException ignored) { }
                    return true;
                }
                // Only the bundled page may load inside the app.
                return !"file".equals(u.getScheme());
            }
        });
        web.addJavascriptInterface(new Bridge(), "Android");

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Timers may have been cancelled or rung from the notification while we were away.
        web.evaluateJavascript("window.onTimersChanged&&onTimersChanged()", null);
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();   // closes baking mode
        else super.onBackPressed();
    }

    private boolean systemDark() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean dark = (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        web.evaluateJavascript("window.onSystemTheme&&onSystemTheme(" + dark + ")", null);
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }

    private void applyBars(boolean dark) {
        int color = getColor(dark ? R.color.bg_dark : R.color.bg_light);
        getWindow().setStatusBarColor(color);
        getWindow().setNavigationBarColor(color);
        web.setBackgroundColor(color);
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            c.setSystemBarsAppearance(dark ? 0 : mask, mask);
        } else {
            View d = getWindow().getDecorView();
            int f = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            d.setSystemUiVisibility(dark ? (d.getSystemUiVisibility() & ~f) : (d.getSystemUiVisibility() | f));
        }
    }

    /** Called from JavaScript as window.Android.* */
    private class Bridge {
        @JavascriptInterface
        public void share(String text, String title) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, title);
                send.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(send, getString(R.string.share_title)));
            });
        }

        @JavascriptInterface
        public void keepAwake(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public boolean isSystemDark() {
            return systemDark();
        }

        @JavascriptInterface
        public void setDark(boolean dark) {
            runOnUiThread(() -> applyBars(dark));
        }

        /** Starts (or restarts) one of Levain's own timers. Returns when it ends, in ms since 1970. */
        @JavascriptInterface
        public double startTimer(int id, String label, int seconds) {
            runOnUiThread(MainActivity.this::askNotificationPermission);
            return Timers.start(MainActivity.this, id, label, seconds);
        }

        @JavascriptInterface
        public void cancelTimer(int id) {
            Timers.cancel(MainActivity.this, id);
        }

        /** Running timers as JSON: [{id, label, endAt}]. */
        @JavascriptInterface
        public String getTimers() {
            return Timers.all(MainActivity.this).toString();
        }

        @JavascriptInterface
        public boolean isAlarmRinging() {
            return AlarmService.isRinging();
        }

        @JavascriptInterface
        public void stopAlarm() {
            AlarmService.stop(MainActivity.this);
        }
    }
}

package online.cyberspace.twa;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class LauncherActivity extends Activity {

    private static final int REQUEST_FILE_CHOOSER = 1001;
    private static final int REQUEST_POST_NOTIFICATIONS = 1002;

    private WebView webView;
    private FrameLayout rootLayout;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean firstLoad = true;

    private ValueCallback<Uri[]> fileChooserCallback;
    private String pendingDownloadUrl;
    private String pendingDownloadMimeType;
    private String pendingDownloadCookie;
    private String pendingDownloadFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: draw behind status bar and nav bar
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        WindowCompat.setDecorFitsSystemWindows(window, false);

        // Root layout: SwipeRefreshLayout > WebView, plus spinner overlay
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setAlpha(0f);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // Wrap WebView in SwipeRefreshLayout so pull-to-refresh only fires at scroll top
        swipeRefreshLayout = new SwipeRefreshLayout(this) {
            @Override
            public boolean canChildScrollUp() {
                return webView != null && webView.getScrollY() > 0;
            }
        };
        swipeRefreshLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        swipeRefreshLayout.setColorSchemeColors(Color.WHITE);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#1A1A1A"));
        swipeRefreshLayout.addView(webView);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                webView.reload();
            } else {
                swipeRefreshLayout.setRefreshing(false);
                showOfflinePage();
            }
        });

        rootLayout.addView(swipeRefreshLayout);
        setContentView(rootLayout);
        overridePendingTransition(0, 0);

        configureWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            if (isNetworkAvailable()) {
                swipeRefreshLayout.setRefreshing(true);
                webView.loadUrl(BuildConfig.APP_URL);
            } else {
                showOfflinePage();
            }
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Security hardening
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        // Performance: pre-raster tiles just outside viewport to reduce scroll jank
        settings.setOffscreenPreRaster(true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebViewClient(new CyberspaceWebViewClient());
        webView.setWebChromeClient(new CyberspaceWebChromeClient());

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                    String contentDisposition, String mimeType, long contentLength) {
                pendingDownloadUrl = url;
                pendingDownloadMimeType = mimeType;
                pendingDownloadCookie = android.webkit.CookieManager.getInstance().getCookie(url);
                pendingDownloadFilename = URLUtil.guessFileName(url, contentDisposition, mimeType);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(LauncherActivity.this,
                            Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(LauncherActivity.this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                REQUEST_POST_NOTIFICATIONS);
                        return;
                    }
                }
                startDownload();
            }
        });
    }

    private void startDownload() {
        if (pendingDownloadUrl == null) return;
        try {
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(pendingDownloadUrl));
            request.setMimeType(pendingDownloadMimeType);
            if (pendingDownloadCookie != null) {
                request.addRequestHeader("Cookie", pendingDownloadCookie);
            }
            request.setDescription("Downloading file...");
            request.setTitle(pendingDownloadFilename);
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, pendingDownloadFilename);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Download started: " + pendingDownloadFilename,
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show();
        } finally {
            pendingDownloadUrl = null;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(
                    android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnectedOrConnecting();
        }
    }

    private void showOfflinePage() {
        swipeRefreshLayout.setRefreshing(false);
        webView.setAlpha(1f);
        String html = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body{background:#000;color:#fff;font-family:sans-serif;"
                + "display:flex;flex-direction:column;align-items:center;"
                + "justify-content:center;height:100vh;margin:0;text-align:center;padding:24px;box-sizing:border-box;}"
                + "h1{font-size:22px;margin-bottom:12px;}"
                + "p{font-size:15px;color:#aaa;margin-bottom:32px;}"
                + "button{background:#333;color:#fff;border:none;padding:14px 32px;"
                + "border-radius:8px;font-size:16px;cursor:pointer;}"
                + "button:active{background:#555;}"
                + "</style></head><body>"
                + "<h1>No Connection</h1>"
                + "<p>Check your internet connection and try again.</p>"
                + "<button onclick='window.location.reload()'>Retry</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL(BuildConfig.APP_URL, html, "text/html", "utf-8", null);
    }

    private void showErrorPage(String description) {
        swipeRefreshLayout.setRefreshing(false);
        webView.setAlpha(1f);
        String html = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body{background:#000;color:#fff;font-family:sans-serif;"
                + "display:flex;flex-direction:column;align-items:center;"
                + "justify-content:center;height:100vh;margin:0;text-align:center;padding:24px;box-sizing:border-box;}"
                + "h1{font-size:22px;margin-bottom:12px;}"
                + "p{font-size:15px;color:#aaa;margin-bottom:32px;}"
                + "button{background:#333;color:#fff;border:none;padding:14px 32px;"
                + "border-radius:8px;font-size:16px;cursor:pointer;}"
                + "button:active{background:#555;}"
                + "</style></head><body>"
                + "<h1>Something went wrong</h1>"
                + "<p>" + (description != null ? description : "The page could not be loaded.") + "</p>"
                + "<button onclick='window.location.reload()'>Retry</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL(BuildConfig.APP_URL, html, "text/html", "utf-8", null);
    }

    // -------------------------------------------------------------------------
    // WebViewClient
    // -------------------------------------------------------------------------

    private class CyberspaceWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            swipeRefreshLayout.setRefreshing(false);
            if (firstLoad) {
                firstLoad = false;
                webView.animate().alpha(1f).setDuration(250).start();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();
            if (host != null && host.equals("beta.cyberspace.online")) {
                return false; // Load inside WebView
            }
            // Open external URLs in the default browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (Exception e) {
                // No app can handle this URL; ignore
            }
            return true;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request,
                WebResourceError error) {
            if (request.isForMainFrame()) {
                String desc = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? error.getDescription().toString()
                        : "Connection error";
                showErrorPage(desc);
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request,
                android.webkit.WebResourceResponse errorResponse) {
            if (request.isForMainFrame()) {
                int code = errorResponse.getStatusCode();
                showErrorPage("HTTP error " + code);
            }
        }
    }

    // -------------------------------------------------------------------------
    // WebChromeClient — file chooser
    // -------------------------------------------------------------------------

    private class CyberspaceWebChromeClient extends WebChromeClient {

        @Override
        public boolean onShowFileChooser(WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams) {
            // Cancel any previous callback
            if (fileChooserCallback != null) {
                fileChooserCallback.onReceiveValue(null);
            }
            fileChooserCallback = filePathCallback;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            if (acceptTypes != null && acceptTypes.length > 0 && !acceptTypes[0].isEmpty()) {
                intent.setType(acceptTypes[0]);
            }

            try {
                startActivityForResult(
                        Intent.createChooser(intent, "Select File"),
                        REQUEST_FILE_CHOOSER);
            } catch (Exception e) {
                fileChooserCallback = null;
                Toast.makeText(LauncherActivity.this,
                        "Cannot open file picker", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // Activity results
    // -------------------------------------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FILE_CHOOSER) {
            if (fileChooserCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            // Proceed with download regardless of permission result
            startDownload();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            // Detach from parent before destroying
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

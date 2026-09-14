package com.melasarang.room;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import org.json.JSONObject;

public final class MainActivity extends Activity implements
    VoiceAssistant.Callback,
    FloatingVoiceController.Listener {

    public static final String HOME_URL = "https://taeguad1-crypto.github.io/Mela1/";
    private static final String EXTRA_RESUME_MIC = "resume_sarangbang_voice";
    private static final String VOICE_CHANNEL = "sarangbang_voice_restore";
    private static final int VOICE_NOTIFICATION_ID = 125;
    private static final int PERMISSION_REQUEST = 1250;
    private static final int FILE_CHOOSER_REQUEST = 1251;

    private FrameLayout root;
    private WebView browser;
    private FloatingVoiceController voiceController;
    private VoiceAssistant voiceAssistant;
    private BrandSplashView brandSplash;
    private ValueCallback<Uri[]> pendingFileChooser;
    private String pendingPortalCommand;
    private final VoiceCommandRouter commandRouter = new VoiceCommandRouter();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initializeApp(savedInstanceState);
        } catch (Throwable startupError) {
            showStartupRecovery(startupError);
        }
    }

    /**
     * Keep the browser startup independent from optional voice services. Some vendor TTS or
     * recognition services can throw during construction; that must never close 멜라루카 사랑방.
     */
    private void initializeApp(Bundle savedInstanceState) {
        enterImmersiveModeSafely();
        createVoiceNotificationChannelSafely();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(2, 10, 28));
        setContentView(root);

        browser = createConfiguredWebView();
        root.addView(browser, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        voiceController = new FloatingVoiceController(this);
        voiceController.setListener(this);
        root.addView(voiceController);
        root.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (voiceController != null &&
                (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)) {
                voiceController.onHostBoundsChanged();
            }
        });

        brandSplash = new BrandSplashView(this);
        root.addView(brandSplash, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        brandSplash.bringToFront();
        brandSplash.postDelayed(() -> {
            if (brandSplash == null || brandSplash.getParent() == null || isFinishing()) return;
            brandSplash.animate()
                .alpha(0f)
                .setDuration(420)
                .withEndAction(() -> {
                    if (brandSplash != null && brandSplash.getParent() == root) {
                        root.removeView(brandSplash);
                    }
                })
                .start();
        }, 1650);

        if (savedInstanceState == null || browser.restoreState(savedInstanceState) == null) {
            browser.loadUrl(HOME_URL);
        }

        // Ask for the microphone only after the visual shell is alive. Voice initialization is
        // delayed until permission has been granted, so a broken TTS engine cannot kill startup.
        requestVoicePermissionIfNeeded();
        if (hasMicrophonePermission()) {
            root.postDelayed(this::initializeVoiceAssistantSafely, 700);
        }

        if (getIntent().getBooleanExtra(EXTRA_RESUME_MIC, false)) {
            root.postDelayed(this::resumeVoiceFromSystem, 850);
        }
    }

    private WebView createConfiguredWebView() {
        WebView view = new WebView(this);
        view.setBackgroundColor(Color.rgb(2, 10, 28));
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        try {
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
        } catch (RuntimeException ignored) {
            // Cookie support is helpful but must not block the first screen on a damaged WebView.
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        view.setWebViewClient(new SarangbangWebViewClient());
        view.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams
            ) {
                if (pendingFileChooser != null) pendingFileChooser.onReceiveValue(null);
                pendingFileChooser = filePathCallback;
                try {
                    Intent chooser = fileChooserParams.createIntent();
                    chooser.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException error) {
                    pendingFileChooser.onReceiveValue(null);
                    pendingFileChooser = null;
                    Toast.makeText(MainActivity.this, "파일을 선택할 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            @Override public boolean onCreateWindow(
                WebView source,
                boolean isDialog,
                boolean isUserGesture,
                Message resultMsg
            ) {
                WebView popupTransport = new WebView(MainActivity.this);
                popupTransport.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView ignored, WebResourceRequest request) {
                        openInsideSarangbangai(request.getUrl().toString());
                        popupTransport.destroy();
                        return true;
                    }

                    @Override public boolean shouldOverrideUrlLoading(WebView ignored, String url) {
                        openInsideSarangbangai(url);
                        popupTransport.destroy();
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupTransport);
                resultMsg.sendToTarget();
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try { WebView.startSafeBrowsing(this, null); }
            catch (RuntimeException ignored) { }
        }
        return view;
    }

    private final class SarangbangWebViewClient extends WebViewClient {
        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleNavigation(request.getUrl());
        }

        @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleNavigation(Uri.parse(url));
        }

        @Override public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (isPortalHome(url)) {
                injectPortalEnhancements(() -> {
                    if (pendingPortalCommand != null) {
                        String command = pendingPortalCommand;
                        pendingPortalCommand = null;
                        evaluatePortalCommand(command, null);
                    }
                });
            }
        }

        @Override public void onSafeBrowsingHit(
            WebView view,
            WebResourceRequest request,
            int threatType,
            SafeBrowsingResponse callback
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) callback.backToSafety(true);
            Toast.makeText(MainActivity.this, "안전하지 않은 페이지를 차단했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean handleNavigation(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (scheme.equals("sarangbang")) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (host.equals("mic")) onMicTap();
            else if (host.equals("home")) goHome();
            else if (host.equals("back")) navigateBack();
            else if (host.equals("forward")) navigateForward();
            return true;
        }
        if (scheme.equals("http") || scheme.equals("https")) return false;
        if (scheme.equals("about") || scheme.equals("data") || scheme.equals("blob")) return false;
        if (scheme.equals("file") && uri.toString().startsWith("file:///android_asset/")) return false;

        if (scheme.equals("tel") || scheme.equals("mailto") || scheme.equals("sms") ||
            scheme.equals("market") || scheme.equals("geo")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException error) {
                Toast.makeText(this, "연결할 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (scheme.equals("intent")) {
            try {
                Intent target = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                startActivity(target);
            } catch (Exception error) {
                Toast.makeText(this, "외부 앱 연결을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    private void openInsideSarangbangai(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (scheme.equals("https") || scheme.equals("http") ||
            (scheme.equals("file") && url.startsWith("file:///android_asset/"))) browser.loadUrl(url);
        else handleNavigation(uri);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || pendingFileChooser == null) return;
        Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        pendingFileChooser.onReceiveValue(result);
        pendingFileChooser = null;
    }

    private boolean hasMicrophonePermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestVoicePermissionIfNeeded() {
        if (!hasMicrophonePermission()) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST);
        }
    }

    private void initializeVoiceAssistantSafely() {
        if (voiceAssistant != null || isFinishing() || isDestroyed()) return;
        try {
            voiceAssistant = new VoiceAssistant(this, this);
            if (hasMicrophonePermission()) voiceAssistant.onHostResume();
        } catch (Throwable voiceError) {
            voiceAssistant = null;
            onVoiceUnavailable("음성 서비스를 준비하지 못했습니다. 마이크를 눌러 다시 시도해 주세요.");
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST) return;
        if (hasMicrophonePermission()) {
            initializeVoiceAssistantSafely();
            if (voiceAssistant != null) {
                voiceAssistant.resumeCompletely();
                voiceAssistant.onHostResume();
            }
        } else {
            onVoiceUnavailable(getString(R.string.voice_permission_reason));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        enterImmersiveModeSafely();
        if (voiceAssistant != null && hasMicrophonePermission()) {
            voiceAssistant.onHostResume();
        }
    }

    @Override protected void onPause() {
        if (voiceAssistant != null) voiceAssistant.onHostPause();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (browser != null) browser.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // The launcher icon and the restore notification are both explicit ways to re-enable it.
        resumeVoiceFromSystem();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (root != null && voiceController != null) {
            root.post(() -> voiceController.onHostBoundsChanged());
        }
    }

    @Override protected void onDestroy() {
        if (voiceAssistant != null) voiceAssistant.destroy();
        if (pendingFileChooser != null) {
            pendingFileChooser.onReceiveValue(null);
            pendingFileChooser = null;
        }
        if (browser != null) {
            browser.stopLoading();
            browser.setWebChromeClient(null);
            browser.setWebViewClient(null);
            browser.destroy();
        }
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        navigateBack();
    }

    @Override public void onTranscript(String transcript) {
        try {
            VoiceCommandRouter.Action action = commandRouter.route(transcript);
            execute(action);
        } catch (RuntimeException commandError) {
            onVoiceUnavailable("명령 실행 중 문제가 발생했습니다. 다시 말씀해 주세요.");
        }
    }

    private void execute(VoiceCommandRouter.Action action) {
        switch (action.type) {
            case OPEN_URL:
                openInsideSarangbangai(action.value);
                respondSafely(action.confirmation);
                break;
            case BACK:
                navigateBack();
                respondSafely(action.confirmation);
                break;
            case FORWARD:
                navigateForward();
                respondSafely(action.confirmation);
                break;
            case HOME:
                goHome();
                respondSafely(action.confirmation);
                break;
            case RELOAD:
                if (browser != null) browser.reload();
                respondSafely(action.confirmation);
                break;
            case SHOW_MIC:
                if (voiceController != null) voiceController.showController();
                respondSafely(action.confirmation);
                break;
            case HIDE_MIC:
                if (voiceAssistant != null && voiceController != null) {
                    voiceAssistant.respond(action.confirmation, voiceController::hideController);
                }
                break;
            case STOP_MIC:
                if (voiceAssistant != null) {
                    voiceAssistant.respond(action.confirmation, this::stopVoiceAndOfferRestore);
                }
                break;
            case MUTE_SPEECH:
                if (voiceAssistant != null) voiceAssistant.setSpeechMuted(true);
                break;
            case UNMUTE_SPEECH:
                if (voiceAssistant != null) {
                    voiceAssistant.setSpeechMuted(false);
                    voiceAssistant.respond(action.confirmation);
                }
                break;
            case TRANSLATE_PAGE:
                translateCurrentPage();
                respondSafely(action.confirmation);
                break;
            case PAGE_COMMAND:
                runPageCommand(action.value);
                respondSafely(action.confirmation);
                break;
            case UNKNOWN:
            default:
                // Ignore accidental background phrases instead of talking over the page.
                respondSafely("");
                break;
        }
    }

    private void runPageCommand(String command) {
        if (browser == null || command == null || command.trim().isEmpty()) return;
        String current = browser.getUrl();
        if (isPortalHome(current)) {
            evaluatePortalCommand(command, handled -> {
                if (!handled) {
                    pendingPortalCommand = command;
                    browser.loadUrl(HOME_URL);
                }
            });
        } else {
            pendingPortalCommand = command;
            browser.loadUrl(HOME_URL);
        }
    }

    private interface CommandResult { void onResult(boolean handled); }

    private void evaluatePortalCommand(String command, CommandResult callback) {
        if (browser == null) return;
        String quoted = JSONObject.quote(command);
        String script = "(function(){try{return window.__SARANGBANG_ROUTE_COMMAND__?!!window.__SARANGBANG_ROUTE_COMMAND__(" +
            quoted + "):false}catch(e){return false}})();";
        browser.evaluateJavascript(script, value -> {
            if (callback != null) callback.onResult("true".equals(value));
        });
    }

    private boolean isPortalHome(String url) {
        return url != null && url.startsWith(HOME_URL);
    }

    private void injectPortalEnhancements(Runnable after) {
        if (after != null) after.run();
    }

    @Override public void onListeningState(boolean listening) {
        if (voiceController != null) voiceController.setListening(listening);
    }

    @Override public void onAudioLevel(float normalizedLevel) {
        if (voiceController != null) voiceController.setAudioLevel(normalizedLevel);
    }

    @Override public void onVoiceUnavailable(String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
    }

    @Override public void onMicTap() {
        if (hasMicrophonePermission()) {
            initializeVoiceAssistantSafely();
            if (voiceAssistant != null) voiceAssistant.startListeningNow();
        } else {
            requestVoicePermissionIfNeeded();
        }
    }

    @Override public void onBack() { navigateBack(); }
    @Override public void onHome() { goHome(); }
    @Override public void onForward() { navigateForward(); }

    private void navigateBack() {
        if (browser != null && browser.canGoBack()) browser.goBack();
        else goHome();
    }

    private void navigateForward() {
        if (browser != null && browser.canGoForward()) browser.goForward();
        else Toast.makeText(this, "다음 페이지가 없습니다.", Toast.LENGTH_SHORT).show();
    }

    private void goHome() {
        if (browser != null) browser.loadUrl(HOME_URL);
        if (voiceController != null) {
            voiceController.showController();
            voiceController.collapse();
        }
    }

    private void translateCurrentPage() {
        String current = browser == null ? null : browser.getUrl();
        if (current == null || !(current.startsWith("https://") || current.startsWith("http://"))) {
            Toast.makeText(this, "번역할 웹페이지가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String encoded = URLEncoder.encode(current, "UTF-8");
            if (browser != null) {
                browser.loadUrl("https://translate.google.com/translate?sl=auto&tl=ko&u=" + encoded);
            }
        } catch (Exception error) {
            Toast.makeText(this, "페이지 번역 주소를 만들 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopVoiceAndOfferRestore() {
        if (voiceAssistant != null) voiceAssistant.stopCompletely();
        if (voiceController != null) voiceController.hideController();
        showRestoreNotification();
    }

    private void resumeVoiceFromSystem() {
        if (voiceController == null) return;
        NotificationManager notifications = getSystemService(NotificationManager.class);
        if (notifications != null) notifications.cancel(VOICE_NOTIFICATION_ID);
        voiceController.showController();
        if (hasMicrophonePermission()) {
            initializeVoiceAssistantSafely();
            if (voiceAssistant != null) voiceAssistant.resumeCompletely();
        } else {
            requestVoicePermissionIfNeeded();
        }
    }

    private void createVoiceNotificationChannelSafely() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        try {
            NotificationChannel channel = new NotificationChannel(
                VOICE_CHANNEL,
                getString(R.string.voice_restore_channel),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.voice_restore_body));
            NotificationManager notifications = getSystemService(NotificationManager.class);
            if (notifications != null) notifications.createNotificationChannel(channel);
        } catch (RuntimeException ignored) { }
    }

    private void showRestoreNotification() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "앱 아이콘을 다시 누르면 마이크가 켜집니다.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent restore = new Intent(this, MainActivity.class)
            .putExtra(EXTRA_RESUME_MIC, true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
            this,
            125,
            restore,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        android.app.Notification notification = new android.app.Notification.Builder(this, VOICE_CHANNEL)
            .setSmallIcon(R.drawable.ic_sarangbang)
            .setContentTitle(getString(R.string.voice_restore_title))
            .setContentText(getString(R.string.voice_restore_body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOngoing(false)
            .build();
        NotificationManager notifications = getSystemService(NotificationManager.class);
        if (notifications != null) notifications.notify(VOICE_NOTIFICATION_ID, notification);
    }

    private void enterImmersiveModeSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            }
        } catch (RuntimeException ignored) { }
    }

    private void respondSafely(String text) {
        if (voiceAssistant != null) voiceAssistant.respond(text);
    }

    private void showStartupRecovery(Throwable error) {
        try {
            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setGravity(android.view.Gravity.CENTER);
            int padding = Math.round(28 * getResources().getDisplayMetrics().density);
            panel.setPadding(padding, padding, padding, padding);
            panel.setBackgroundColor(Color.rgb(238, 255, 255));

            TextView title = new TextView(this);
            title.setText("멜라루카 사랑방");
            title.setTextColor(Color.rgb(5, 43, 83));
            title.setTextSize(34);
            title.setGravity(android.view.Gravity.CENTER);

            TextView message = new TextView(this);
            message.setText("시작 화면을 복구했습니다. 아래 버튼을 눌러 다시 실행해 주세요.");
            message.setTextColor(Color.rgb(25, 71, 101));
            message.setTextSize(17);
            message.setGravity(android.view.Gravity.CENTER);
            message.setPadding(0, padding, 0, padding);

            Button retry = new Button(this);
            retry.setText("멜라루카 사랑방 다시 열기");
            retry.setOnClickListener(v -> recreate());

            panel.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            panel.addView(message, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            panel.addView(retry, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            setContentView(panel);
        } catch (Throwable ignored) {
            Toast.makeText(this, "멜라루카 사랑방 시작 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }
}

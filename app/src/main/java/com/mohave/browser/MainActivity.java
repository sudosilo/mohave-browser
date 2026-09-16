package com.mohave.browser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class MainActivity extends Activity {

    // The page that opens when the browser starts.
    private static final String HOME = "https://start.duckduckgo.com";

    // The Gecko engine. Only one may exist per app, so it is static.
    private static GeckoRuntime runtime;

    // One session is one tab.
    private GeckoSession session;

    private EditText addressBar;
    private ProgressBar progressBar;
    private boolean canGoBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addressBar = findViewById(R.id.address_bar);
        progressBar = findViewById(R.id.progress_bar);
        GeckoView geckoView = findViewById(R.id.gecko_view);
        ImageButton backButton = findViewById(R.id.back_button);
        ImageButton reloadButton = findViewById(R.id.reload_button);

        if (runtime == null) {
            runtime = GeckoRuntime.create(getApplicationContext());
        }

        session = new GeckoSession();

        // Gecko tells us whether there is a page to go back to.
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(GeckoSession s, boolean value) {
                canGoBack = value;
            }
        });

        // Gecko tells us when a page starts, how far it has loaded, and when it stops.
        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onPageStart(GeckoSession s, String url) {
                if (!addressBar.hasFocus()) {
                    addressBar.setText(url);
                }
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProgressChange(GeckoSession s, int progress) {
                progressBar.setProgress(progress);
            }

            @Override
            public void onPageStop(GeckoSession s, boolean success) {
                progressBar.setVisibility(View.GONE);
            }
        });

        session.open(runtime);
        geckoView.setSession(session);

        // Pressing Go on the keyboard loads whatever is typed.
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_GO || enterKey) {
                go(addressBar.getText().toString());
                return true;
            }
            return false;
        });

        backButton.setOnClickListener(v -> {
            if (canGoBack) {
                session.goBack();
            }
        });

        reloadButton.setOnClickListener(v -> session.reload());

        // If another app opened a link with this browser, load that link.
        String startUrl = getIntent() != null ? getIntent().getDataString() : null;
        session.loadUri(startUrl != null ? startUrl : HOME);
    }

    // Turns typed text into an address or a search.
    private void go(String input) {
        String text = input.trim();
        if (text.isEmpty()) {
            return;
        }

        String target;
        if (text.startsWith("http://") || text.startsWith("https://")) {
            target = text;
        } else if (text.contains(".") && !text.contains(" ")) {
            target = "https://" + text;
        } else {
            try {
                target = "https://duckduckgo.com/?q=" + URLEncoder.encode(text, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                target = "https://duckduckgo.com/?q=" + text;
            }
        }

        session.loadUri(target);
        addressBar.clearFocus();
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String url = intent.getDataString();
        if (url != null) {
            session.loadUri(url);
        }
    }

    // The phone's back gesture goes back a page before leaving the app.
    @Override
    public void onBackPressed() {
        if (canGoBack) {
            session.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

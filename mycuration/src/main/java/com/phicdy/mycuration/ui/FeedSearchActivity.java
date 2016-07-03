package com.phicdy.mycuration.ui;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.phicdy.mycuration.R;
import com.phicdy.mycuration.db.DatabaseAdapter;
import com.phicdy.mycuration.rss.Feed;
import com.phicdy.mycuration.rss.UnreadCountManager;
import com.phicdy.mycuration.task.NetworkTaskManager;
import com.phicdy.mycuration.tracker.GATrackerHelper;
import com.phicdy.mycuration.util.UrlUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class FeedSearchActivity extends AppCompatActivity {

    private SearchView searchView;
    private WebView webView;

    private BroadcastReceiver receiver;
    private GATrackerHelper gaTrackerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_search);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Show back arrow icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        webView = (WebView)findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = webView.getUrl();
                if (TextUtils.isEmpty(url)) return;
                Intent intent = new Intent(FeedSearchActivity.this, FeedUrlHookActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        gaTrackerHelper = GATrackerHelper.getInstance(this);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed_search, menu);
        final MenuItem searchMenuItem = menu.findItem(R.id.search_feed);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform final search
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Text change. Apply filter
                return false;
            }
        });

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));

        // Open searchview
        searchView.setIconified(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (UrlUtil.isCorrectUrl(query)) {
                final ProgressDialog dialog = new ProgressDialog(this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage(getString(R.string.adding_rss));
                receiver = new BroadcastReceiver() {

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action.equals(NetworkTaskManager.FINISH_ADD_FEED)) {
                            DatabaseAdapter dbAdapter = DatabaseAdapter.getInstance(getApplicationContext());
                            Feed newFeed = dbAdapter.getFeedByUrl(intent.getStringExtra(NetworkTaskManager.ADDED_FEED_URL));
                            if (intent.hasExtra(NetworkTaskManager.ADD_FEED_ERROR_REASON) || newFeed == null) {
                                int errorMessage = R.string.add_rss_error_generic;
                                if (intent.getIntExtra(NetworkTaskManager.ADD_FEED_ERROR_REASON, -1)
                                        == NetworkTaskManager.ERROR_INVALID_URL) {
                                    errorMessage = R.string.add_rss_error_invalid_url;
                                }
                                Toast.makeText(getApplicationContext(),
                                        errorMessage,
                                        Toast.LENGTH_SHORT).show();
                                gaTrackerHelper.sendEvent(getString(R.string.add_rss_input_url_error));
                            } else {
                                UnreadCountManager.getInstance(context).addFeed(newFeed);
                                NetworkTaskManager.getInstance(context).updateFeed(newFeed);
                                Toast.makeText(getApplicationContext(),
                                        R.string.add_rss_success,
                                        Toast.LENGTH_SHORT).show();
                                gaTrackerHelper.sendEvent(getString(R.string.add_rss_input_url));
                            }
                            dialog.dismiss();
                            unregisterReceiver(this);
                            finish();
                        }
                    }
                };
                IntentFilter filter = new IntentFilter();
                filter.addAction(NetworkTaskManager.FINISH_ADD_FEED);
                registerReceiver(receiver, filter);
                dialog.show();
                NetworkTaskManager.getInstance(getApplicationContext()).addNewFeed(query);
                return;
            }
            try {
                String encodedQuery = URLEncoder.encode(query, "utf-8");
                String url = "https://www.google.co.jp/search?q=" + encodedQuery;
                webView.loadUrl(url);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK){
            if (webView.canGoBack()) {
                webView.goBack();
            }else {
                finish();
            }
            return true;
        }
        return false;
    }
}

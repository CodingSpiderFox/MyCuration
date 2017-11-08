package com.phicdy.mycuration.rss;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.phicdy.mycuration.db.DatabaseAdapter;
import com.phicdy.mycuration.task.NetworkTaskManager;
import com.phicdy.mycuration.util.UrlUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RssParseExecutor {
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DatabaseAdapter adapter;
    private RssParser parser;

    public interface RssParseCallback {
        void succeeded(@NonNull String url);
        void failed(@RssParseResult.FailedReason int reason);
    }

    public RssParseExecutor(@NonNull RssParser parser, @NonNull DatabaseAdapter adapter) {
        this.parser = parser;
        this.adapter = adapter;
    }

    public void start(@NonNull final String url, @NonNull final RssParseCallback callback) {
        if (parser == null) return;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RssParseResult result = parser.parseRssXml(UrlUtil.removeUrlParameter(url), true);
                if (result != null && result.failedReason == RssParseResult.NOT_FAILED &&
                        result.feed != null) {
                    Feed feed = result.feed;
                    adapter.saveNewFeed(feed.getTitle(), feed.getUrl(), feed.getFormat(), feed.getSiteUrl());
                    callback.succeeded(url);
                } else {
                    callback.failed(result != null ? result.failedReason : RssParseResult.NOT_FOUND);
                }
            }
        });

    }
}
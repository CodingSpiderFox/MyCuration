package com.phicdy.mycuration.domain.task

import android.util.Log
import com.phicdy.mycuration.data.db.DatabaseAdapter
import com.phicdy.mycuration.data.rss.Feed
import com.phicdy.mycuration.domain.filter.FilterTask
import com.phicdy.mycuration.domain.rss.RssParser
import com.phicdy.mycuration.domain.rss.UnreadCountManager
import com.phicdy.mycuration.util.FileUtil
import com.phicdy.mycuration.util.TextUtil
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.IOException
import java.io.InputStream
import java.net.URI

object NetworkTaskManager {

    const val FINISH_UPDATE_ACTION = "FINISH_UPDATE"
    const val ERROR_INVALID_URL = 1
    const val ERROR_NON_RSS_HTML_CONTENT = 2
    const val ERROR_UNKNOWN = 3
    const val REASON_NOT_FOUND = -1

    val isUpdatingFeed: Boolean get() = false

    fun updateAllFeeds(feeds: ArrayList<Feed>): Flowable<Feed> {
        return Flowable.fromIterable<Feed>(feeds)
                .subscribeOn(Schedulers.io())
                .filter { feed -> feed.id > 0 }
                .flatMap({ data -> Flowable.just(data).subscribeOn(Schedulers.io()) })
                { _, newData ->
                    Log.d("NetworkTask", "BiFunction, Thread:" + Thread.currentThread().name + ", feed:" + newData.title)
                    updateFeed(newData)
                    newData
                }
    }

    private interface FeedRequestService {
        @GET
        fun feeds(@Url url: String): Call<ResponseBody>
    }

    fun updateFeed(feed: Feed): InputStream? {
        if (TextUtil.isEmpty(feed.url)) return null
        val uri = URI.create(feed.url)
        val retrofit = Retrofit.Builder()
                .baseUrl(uri.scheme + "://" + uri.host)
                .build()
        val service = retrofit.create(FeedRequestService::class.java)
        val call = service.feeds(uri.path)
        try {
            val response = call.execute()
            if (response.body() == null) {
                return null
            }
            val inputStream = response.body().byteStream()
            val parser = RssParser()
            val dbAdapter = DatabaseAdapter.getInstance()
            val latestDate = dbAdapter.getLatestArticleDate(feed.id);
            val articles = parser.parseXml(inputStream, latestDate)

            if (articles.size > 0) {
                dbAdapter.saveNewArticles(articles, feed.id)
                val getHatenaBookmark = GetHatenaBookmark(dbAdapter)
                var delaySec = 0
                for ((i, article) in articles.withIndex()) {
                    getHatenaBookmark.request(article.url, delaySec)
                    if (i % 10 == 0) delaySec += 2
                }
            }

            FilterTask().applyFiltering(feed.id)
            try {
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (feed.iconPath == null || feed.iconPath == Feed.DEDAULT_ICON_PATH) {
                val iconSaveFolderStr = FileUtil.iconSaveFolder()
                val task = GetFeedIconTask(iconSaveFolderStr)
                task.execute(feed.siteUrl)
            }
            UnreadCountManager.getInstance().refreshConut(feed.id)
        } catch (e: IOException) {

        } catch (e: RuntimeException) {

        }
        return null
    }
}

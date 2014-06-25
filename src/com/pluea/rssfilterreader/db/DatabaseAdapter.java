package com.pluea.rssfilterreader.db;

import java.util.ArrayList;

import com.pleua.rssfilterreader.rss.Article;
import com.pleua.rssfilterreader.rss.Feed;
import com.pluea.rssfilterreader.filter.Filter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DatabaseAdapter {
	static final String DATABASENAME = "rss_manage";
	static final int DATABASEVERSION = 1;

	private Context context;
	private static DatabaseHelper dbHelper;
	private static SQLiteDatabase db;
	private static final String LOG_TAG = "RSSReader."
			+ DatabaseAdapter.class.getName();

	public DatabaseAdapter(Context context) {
		this.context = context;
		dbHelper = new DatabaseHelper(this.context);
	}

	public void open(String readOrWrite) {
		if (db == null || !db.isOpen()) {
			if (readOrWrite.equals("write")) {
				db = dbHelper.getWritableDatabase();
			} else if (readOrWrite.equals("read")) {
				db = dbHelper.getReadableDatabase();
			}
		}
	}


	public void saveNewArticles(ArrayList<Article> articles, int feedId) {
		if(articles.isEmpty()) {
			return;
		}
		Log.i(LOG_TAG, "insertNewArticles starts");
		open("write");
		db.beginTransaction();
		try {
			// db.delete("articles", "title like '%PR%'", null);
			SQLiteStatement insertSt = db
					.compileStatement("insert into articles(title,url,status,point,date,feedId) values (?,?,?,?,?,?);");
			// articles passed isArticle(), so not need to check same article exist
			for (Article article : articles) {
				insertSt.bindString(1, sanitizing(article.getTitle()));
				insertSt.bindString(2, article.getUrl());
				insertSt.bindString(3, "unread");
				insertSt.bindString(4, "10");
				Log.d(LOG_TAG, "insert date:" + article.getPostedDate());
				insertSt.bindLong(5, article.getPostedDate());
				insertSt.bindString(6, String.valueOf(feedId));

				insertSt.executeInsert();
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "insertNewArticles finished");
	}

	public int calcNumOfUnreadArticles(int feedId) {
		Log.i(LOG_TAG, "calcNumOfUnreadArticles starts");
		open("write");
		int unreadArticlesCount = 0;
		db.beginTransaction();
		try {
			String getArticlesCountsql = "select _id from articles where status = \"unread\" and feedId = "
					+ feedId;
			Cursor countCursor = db.rawQuery(getArticlesCountsql, null);
			unreadArticlesCount = countCursor.getCount();
			countCursor.close();
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "calcNumOfUnreadArticles finished");
		return unreadArticlesCount;
	}

	public ArrayList<Feed> getAllFeeds() {
		Log.i(LOG_TAG, "getAllFeeds starts");
		ArrayList<Feed> feedList = new ArrayList<Feed>();
		open("write");
		String sql = "select _id,title,url from feeds order by title";
		Cursor cursor = db.rawQuery(sql, null);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				feedList.add(new Feed(id, title, url));
			}
		}
		cursor.close();
		
		Log.i(LOG_TAG, "getAllFeeds finished");
		return feedList;
	}

	public void saveStatusBeforeUpdate(int articleId) {
		Log.i(LOG_TAG, "saveStatusBeforeUpdate starts");
		open("write");
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("status", "toRead");
			db.update("articles", values, "_id = " + articleId, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "saveStatusBeforeUpdate finished");
	}
	
	public void saveStatus(int articleId, String status) {
		Log.i(LOG_TAG, "saveStatus starts");
		open("write");
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("status", status);
			db.update("articles", values, "_id = " + articleId, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "saveStatus finished");
	}
	
	public void saveHatenaPoint(int articleId, String point) {
		Log.i(LOG_TAG, "saveHatenaPoint starts");
		open("write");
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put("point", point);
			db.update("articles", values, "_id = " + articleId, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "saveHatenaPoint finished");
	}

	public String getStatus(int articleId) {
		Log.i(LOG_TAG, "getStatus starts");
		String status = null;
		open("write");
		db.beginTransaction();
		try {
			String sql = "select status from articles where _id = " + articleId;
			Cursor cur = db.rawQuery(sql, null);
			cur.moveToNext();
			status = cur.getString(0);
			cur.close();
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "getStatus finished");
		return status;
	}

	public void deleteFeed(int feedId) {
		Log.i(LOG_TAG, "deleteFeed starts");
		open("write");
		db.beginTransaction();
		try {
			db.delete("articles", "feedId = " + feedId, null);
			db.delete("filters", "feedId = " + feedId, null);
			// db.delete("priorities","feedId = "+feedId,null);
			db.delete("feeds", "_id = " + feedId, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "deleteFeed finished");
	}

	public Feed getFeedByUrl(String feedUrl) {
		Log.i(LOG_TAG, "getFeedByUrl starts");
		Feed feed = null;
		open("write");
		db.beginTransaction();
		try {
			// Get feed
			String getFeedSql = "select _id,title from feeds where url = \""
					+ feedUrl + "\"";
			Cursor cur = db.rawQuery(getFeedSql, null);
			if (cur.getCount() != 0) {
				cur.moveToNext();
				int feedId = cur.getInt(0);
				String feedTitle = cur.getString(1);

				feed = new Feed(feedId, feedTitle, feedUrl);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "getFeedByUrl finished");
		return feed;
	}

	public Feed saveNewFeed(String feedTitle, String feedUrl, String format) {
		// Log.i(LOG_TAG, "saveNewFeed starts");
		boolean sameFeedExist = false;

		// Use writeable DB
		open("write");
		db.beginTransaction();
		try {
			// Get same feeds from DB
			String sql = "select _id from feeds " + "where title=\""
					+ feedTitle + "\" and url=\"" + feedUrl
					+ "\" and format=\"" + format + "\"";
			Cursor cursor = db.rawQuery(sql, null);

			// If there aren't same feeds in DB,Insert into DB
			if (cursor.getCount() == 0) {
				ContentValues values = new ContentValues();
				values.put("title", feedTitle);
				values.put("url", feedUrl);
				values.put("format", format);
				if (db.insert("feeds", null, values) == -1) {
					Log.v("insert error", "error occurred");
				}
			} else {
				// Same feed already exists
				sameFeedExist = true;
			}
			cursor.close();
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		if (sameFeedExist) {
			return null;
		}
		// Log.i(LOG_TAG, "saveNewFeed finished");
		return getFeedByUrl(feedUrl);
	}

	public boolean changeArticlesStatusToRead() {
		Log.i(LOG_TAG, "changeArticlesStatusToRead starts");
		// Use writeable DB
		open("write");
		db.beginTransaction();
		try {
			// Update articles read status in the "readstatus" to DB
			ContentValues value = new ContentValues();
			value.put("status", "write");
			db.update("articles", value, "status = \"toRead\"", null);
			db.setTransactionSuccessful();
		} catch (Exception e) {
			return false;
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "changeArticlesStatusToRead finished");
		return true;
	}

	public int getNumOfUnreadArtilces(int feedId) {
		Log.i(LOG_TAG, "getNumOfUnreadArtilces starts");
		int num = 0;
		open("write");
		db.beginTransaction();
		try {
			// Get unread articles and set num of unread articles
			String sql = "select _id from articles where status = \"unread\" and feedId = "
					+ feedId;
			Cursor cursor = db.rawQuery(sql, null);
			num = cursor.getCount();
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			num = -1;
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "getNumOfUnreadArtilces finished");
		return num;
	}

	public ArrayList<Article> getUnreadArticlesInAFeed(int feedId) {
		Log.i(LOG_TAG, "getUnreadArticlesInAFeed starts");
		ArrayList<Article> articles = new ArrayList<Article>();
		open("write");
		db.beginTransaction();
		try {
			// Get unread articles
			String sql = "select _id,title,url,point,date from articles where status = \"unread\" and feedId = "
					+ feedId + " order by date desc";
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = "unread";
				String point = cursor.getString(3);
				long dateLong = cursor.getLong(4);
				Article article = new Article(id, title, url, status, point,
						dateLong, feedId);
				articles.add(article);
			}
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			
			return articles;
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "getUnreadArticlesInAFeed finished");
		return articles;
	}

	public ArrayList<Filter> getFiltersOfFeed(int feedId) {
		Log.i(LOG_TAG, "getFiltersOfFeed starts");
		ArrayList<Filter> filterList = new ArrayList<Filter>();
		open("write");

		db.beginTransaction();
		try {
			// Get all filters which feed ID is "feedId"
			String[] columns = { "_id", "title", "keyword", "url" };
			String condition = "feedId = " + feedId;
			Cursor cur = db.query("filters", columns, condition, null, null,
					null, null);
			// Change to ArrayList
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				String title = cur.getString(1);
				String keyword = cur.getString(2);
				String url = cur.getString(3);
				filterList.add(new Filter(id, title, keyword, url, feedId));
			}
			cur.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			return null;
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "getFiltersOfFeed finished");
		return filterList;
	}

	public boolean applyFiltersOfFeed(ArrayList<Filter> filterList, int feedId) {
		Log.i(LOG_TAG, "applyFiltersOfFeed starts");
		// If articles are hit in condition, Set articles status to "read"
		ContentValues value = new ContentValues();
		value.put("status", "read");
		for (Filter filter : filterList) {
			open("write");
			db.beginTransaction();
			try {
				// Initialize condition
				String condition = "feedId = " + feedId;

				// If keyword or url exists, add condition
				String keyword = filter.getKeyword();
				String url = filter.getUrl();
				if (keyword.equals("") && url.equals("")) {
					Log.w("Set filtering conditon",
							"keyword and url don't exist fileter ID ="
									+ filter.getId());
					continue;
				}
				if (!keyword.equals("")) {
					condition = condition + " and title like '%" + keyword
							+ "%'";
				}
				if (!url.equals("")) {
					condition = condition + " and url like '%" + url + "%'";
				}
				db.update("articles", value, condition, null);
				db.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("Apply Filtering", "Article can't be updated.Feed ID = "
						+ feedId);
				
				return false;
			} finally {
				db.endTransaction();
			}
			
		}
		Log.i(LOG_TAG, "applyFiltersOfFeed finished");
		return true;
	}

	public void deleteFilter(int filterId) {
		Log.i(LOG_TAG, " deleteFilter starts");
		open("write");
		db.beginTransaction();
		try {
			db.delete("filters", "_id = " + filterId, null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "deleteFilter finished");
	}

	public int getNumOfFeeds() {
		Log.i(LOG_TAG, "getNumOfFeeds starts");
		int num = 0;
		open("write");
		db.beginTransaction();
		try {
			// Get unread feeds and set num of unread feeds
			String sql = "select _id from feeds";
			Cursor cursor = db.rawQuery(sql, null);
			num = cursor.getCount();
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			num = -1;
		} finally {
			db.endTransaction();
		}
		
		Log.i(LOG_TAG, "getNumOfFeeds finished");
		return num;
	}

	public void saveNewFilter(String title, int selectedFeedId, String keyword,
			String filterUrl) {

		open("write");
		db.beginTransaction();
		try {
			// Check same fileter exists in DB
			String getSameFilterSql = "select _id from filters "
					+ "where title='" + title + "' and " + "feedId = "
					+ selectedFeedId + " and " + "keyword = '" + keyword
					+ "' and " + "url = '" + filterUrl + "'";
			Cursor cur = db.rawQuery(getSameFilterSql, null);
			if (cur.getCount() != 0) {
				Log.i("Register Filter", "Same Filter Exist");
			} else {
				// Register filter
				ContentValues values = new ContentValues();
				values.put("title", title);
				values.put("url", filterUrl);
				values.put("keyword", keyword);
				values.put("feedId", selectedFeedId);
				db.insert("filters", null, values);
				db.setTransactionSuccessful();
			}
		} catch (Exception e) {
			Log.e("insert error", "error occurred");
		} finally {
			db.endTransaction();
		}
		
	}

	public static String sanitizing(String str) {

		if (str == null || "".equals(str)) {
			return str;
		}
		str = str.replaceAll("'", "''");
		str = str.replaceAll("%", "\\%");
		return str;
	}

	public void addManyFeeds() {
		// Log.i(LOG_TAG, "saveNewFeed starts");

		ArrayList<Feed> feeds = new ArrayList<Feed>();
		//RSS 2.0
		feeds.add(new Feed(0, "スポーツナビ - ピックアップ　ゲーム",
				"http://sports.yahoo.co.jp/rss/pickup_game/pc"));
		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - トップ",
				"http://rss.dailynews.yahoo.co.jp/fc/rss.xml"));
		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - 海外",
				"http://rss.dailynews.yahoo.co.jp/fc/world/rss.xml"));
		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - 経済",
				"http://rss.dailynews.yahoo.co.jp/fc/economy/rss.xml"));
		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - エンターテインメント",
				"http://rss.dailynews.yahoo.co.jp/fc/entertainment/rss.xml"));
		//atom
		feeds.add(new Feed(0, "TweetBuzz - 注目エントリー",
				"http://feeds.feedburner.com/tb-hotentry"));
		//RDF
		feeds.add(new Feed(0, "二十歳街道まっしぐら",
				"http://20kaido.com/index.rdf"));
		
		// Use writeable DB
		open("write");
		db.beginTransaction();
		try {

			// If there aren't same feeds in DB,Insert into DB
			for (Feed feed : feeds) {
				ContentValues values = new ContentValues();
				values.put("title", feed.getTitle());
				values.put("url", feed.getUrl());
				values.put("format", "RSS2.0");
				if (db.insert("feeds", null, values) == -1) {
					Log.v("insert error", "error occurred");
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		
		// Log.i(LOG_TAG, "saveNewFeed finished");
	}

	public boolean isArticle(Article article) {
		Log.i(LOG_TAG, "isArticle starts");
		int num = 0;
		open("write");
		db.beginTransaction();
		try {
			// Get same article
			String sql = "select _id from articles where title = '"
					+ article.getTitle() + "' and " + "url = '"
					+ article.getUrl() + "' and " + "date = '"
					+ article.getPostedDate() + "';";
			Log.d(LOG_TAG, "SQL:" + sql);
			
			Cursor cursor = db.rawQuery(sql, null);
			num = cursor.getCount();
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			num = -1;
		} finally {
			db.endTransaction();
		}
		
		if(num > 0) {
			Log.d(LOG_TAG, "article exists!");
			Log.d(LOG_TAG, "title:" + article.getTitle());
			return true;
		}
		return false;
	}
}

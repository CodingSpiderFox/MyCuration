package com.phicdy.mycuration.data.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;

import com.phicdy.mycuration.data.filter.Filter;
import com.phicdy.mycuration.data.filter.FilterFeedRegistration;
import com.phicdy.mycuration.data.rss.Article;
import com.phicdy.mycuration.data.rss.Curation;
import com.phicdy.mycuration.data.rss.CurationCondition;
import com.phicdy.mycuration.data.rss.CurationSelection;
import com.phicdy.mycuration.data.rss.Feed;
import com.phicdy.mycuration.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DatabaseAdapter {
	
    private static DatabaseAdapter sharedDbAdapter;
	private SQLiteDatabase db;

	private static final String BACKUP_FOLDER = "filfeed_backup";
	public static final int NOT_FOUND_ID = -1;
    private static final int INSERT_ERROR_ID = -1;
    private static final int MIN_TABLE_ID = 1;

	private static final String LOG_TAG = "MyCuration."
			+ DatabaseAdapter.class.getSimpleName();

	private DatabaseAdapter() {
	}

	public static void setUp(@NonNull DatabaseHelper dbHelper) {
		if (sharedDbAdapter == null) {
			synchronized (DatabaseAdapter.class) {
				if (sharedDbAdapter == null) {
					sharedDbAdapter = new DatabaseAdapter();
					sharedDbAdapter.db = dbHelper.getWritableDatabase();
				}
			}
		}
	}
	
	public static DatabaseAdapter getInstance() {
		if (sharedDbAdapter == null) {
		    throw new IllegalStateException("Not setup yet");
		}
		return sharedDbAdapter;
	}

    /**
     * Save method for new articles
     *
     * @param articles Article array to save
     * @param feedId Feed ID of the articles
     */
	public void saveNewArticles(ArrayList<Article> articles, int feedId) {
		if(articles.isEmpty()) {
			return;
		}
		SparseArray<ArrayList<String>> curationWordMap = getAllCurationWords();
		SQLiteStatement insertArticleSt = db.compileStatement(
				"insert into articles(title,url,status,point,date,feedId) values (?,?,?,?,?,?);");
		SQLiteStatement insertCurationSelectionSt = db.compileStatement(
				"insert into " + CurationSelection.TABLE_NAME +
				"(" + CurationSelection.ARTICLE_ID + "," + CurationSelection.CURATION_ID + ") values (?,?);");
		db.beginTransaction();
		try {
			for (Article article : articles) {
				insertArticleSt.bindString(1, article.getTitle());
				insertArticleSt.bindString(2, article.getUrl());
				insertArticleSt.bindString(3, article.getStatus());
				insertArticleSt.bindString(4, article.getPoint());
				Log.d(LOG_TAG, "insert date:" + article.getPostedDate());
				insertArticleSt.bindLong(5, article.getPostedDate());
				insertArticleSt.bindString(6, String.valueOf(feedId));

				long articleId = insertArticleSt.executeInsert();
                for (int i = 0; i < curationWordMap.size(); i++) {
                    int curationId = curationWordMap.keyAt(i);
                    ArrayList<String> words = curationWordMap.valueAt(i);
					for (String word : words) {
						if (article.getTitle().contains(word)) {
							insertCurationSelectionSt.bindString(1, String.valueOf(articleId));
							insertCurationSelectionSt.bindString(2, String.valueOf(curationId));
							insertCurationSelectionSt.executeInsert();
							break;
						}
					}
				}
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

    /**
     * Check method of article existence of specified ID.
     *
     * @param feedId Feed ID to check
     * @return {@code true} if exists.
     */
    public boolean isExistArticle(int feedId) {
		boolean isExist = false;
		db.beginTransaction();
		try {
			String getArticlesCountsql = "select _id from articles where feedId = "
					+ feedId + " limit 1";
			Cursor countCursor = db.rawQuery(getArticlesCountsql, null);
			isExist = (countCursor.getCount() > 0);
			countCursor.close();
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		return isExist;
	}

    /**
     * Check method of article existence.
     *
     * @return {@code true} if there is an article or more.
     */
	public boolean isExistArticle() {
		boolean isExist = false;
		db.beginTransaction();
		try {
			String getArticlesCountsql = "select _id from articles limit 1";
			Cursor countCursor = db.rawQuery(getArticlesCountsql, null);
			isExist = (countCursor.getCount() > 0);
			countCursor.close();
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		return isExist;
	}

    /**
     * Get method to feed array with unread count of articles.
     *
     * @return Feed array with unread count of articles
     */
	public ArrayList<Feed> getAllFeedsWithNumOfUnreadArticles() {
		ArrayList<Feed> feedList = new ArrayList<>();
		db.beginTransaction();
        Cursor cursor = null;
		try {
			String[] columns = {Feed.ID,Feed.TITLE,Feed.URL,Feed.ICON_PATH,Feed.SITE_URL,Feed.UNREAD_ARTICLE};
			String orderBy = Feed.TITLE;
			cursor = db.query(Feed.TABLE_NAME, columns, null, null, null, null, orderBy);
			if (cursor != null) {
				while (cursor.moveToNext()) {
					int id = cursor.getInt(0);
					String title = cursor.getString(1);
					String url = cursor.getString(2);
					String iconPath = cursor.getString(3);
					String siteUrl = cursor.getString(4);
					int unreadAriticlesCount = cursor.getInt(5);
					feedList.add(new Feed(id, title, url, iconPath, siteUrl, unreadAriticlesCount));
				}
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
            if (cursor != null) {
                cursor.close();
            }
			db.endTransaction();
		}
		
		if (feedList.size() == 0) {
			feedList = getAllFeedsWithoutNumOfUnreadArticles();
		}
		return feedList;
	}

    /**
     * Get method to feed array without unread count of articles.
     *
     * @return Feed array without unread count of articles
     */
	public ArrayList<Feed> getAllFeedsWithoutNumOfUnreadArticles() {
		ArrayList<Feed> feedList = new ArrayList<>();
		String[] columns = {Feed.ID,Feed.TITLE,Feed.URL,Feed.ICON_PATH,Feed.SITE_URL};
		String orderBy = Feed.TITLE;
		db.beginTransaction();
        Cursor cursor = null;
		try {
			cursor = db.query(Feed.TABLE_NAME, columns, null, null, null, null, orderBy);
			if (cursor != null) {
				while (cursor.moveToNext()) {
					int id = cursor.getInt(0);
					String title = cursor.getString(1);
					String url = cursor.getString(2);
					String iconPath = cursor.getString(3);
					String siteUrl = cursor.getString(4);
					feedList.add(new Feed(id, title, url, iconPath, siteUrl, 0));
				}
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
            if (cursor != null) {
                cursor.close();
            }
			db.endTransaction();
		}
		
		return feedList;
	}

    /**
     * Update method for all of the articles of feed ID to read status.
     *
     * @param feedId Feed ID for articles to change status to read
     */
    public void saveStatusToRead(int feedId) {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Article.STATUS, Article.READ);
			String whereClause = Article.FEEDID + " = " + feedId;
			db.update(Article.TABLE_NAME, values, whereClause, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		
	}

    /**
     * Update method for all of the articles to read status.
     */
	public void saveAllStatusToRead() {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Article.STATUS, Article.READ);
			db.update(Article.TABLE_NAME, values, null, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		
	}

    /**
     * Update method from "to read" to "read" for all of the articles.
     */
    public void saveAllStatusToReadFromToRead() {
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(Article.STATUS, Article.READ);
            String condition = Article.STATUS + " = '" + Article.TOREAD + "'";
            db.update(Article.TABLE_NAME, values, condition, null);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
			e.printStackTrace();
		} finally {
            db.endTransaction();
        }
    }

    /**
     * Update method for article read/unread status.
     *
     * @param articleId Artilce ID to change status
     * @param status New status
     */
	public void saveStatus(int articleId, String status) {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Article.STATUS, status);
			db.update(Article.TABLE_NAME, values, Article.ID + " = " + articleId, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

    /**
     * Update method for unread article count of the feed.
     *
     * @param feedId Feed ID to change
     * @param unreadCount New article unread count
     */
	public void updateUnreadArticleCount(int feedId, int unreadCount) {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Feed.UNREAD_ARTICLE, unreadCount);
			db.update(Feed.TABLE_NAME, values, "_id = " + feedId, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

    /**
     * Update method for feed icon path.
     *
     * @param siteUrl Site URL of the feed to change
     * @param iconPath New icon path
     */
	public void saveIconPath(String siteUrl, String iconPath) {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Feed.ICON_PATH, iconPath);
			db.update(Feed.TABLE_NAME, values, Feed.SITE_URL + " = '" + siteUrl + "'", null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

    /**
     * Update method for hatena point of the article.
     *
     * @param url Article URL to update
     * @param point New hatena point
     */
	public void saveHatenaPoint(String url, String point) {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Article.POINT, point);
			db.update(Article.TABLE_NAME, values, Article.URL + " = '" + url + "'", null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

    /**
     * Update method for feed title.
     *
     * @param feedId Feed ID to update
     * @param newTitle New feed title
     * @return Num of updated feeds
     */
	public int saveNewTitle(int feedId, String newTitle) {
		int numOfUpdated = 0;
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Feed.TITLE, newTitle);
			numOfUpdated = db.update(Feed.TABLE_NAME, values, Feed.ID + " = " + feedId, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		return numOfUpdated;
	}

    /**
     * Delete method for feed and related data.
     *
     * @param feedId Feed ID to delete
     * @return result of delete
     */
    public boolean deleteFeed(int feedId) {
		int numOfDeleted = 0;
		ArrayList<Article> allArticles = getAllArticlesInAFeed(feedId, true);
		try {
            db.beginTransaction();
			for (Article article : allArticles) {
				db.delete(CurationSelection.TABLE_NAME, CurationSelection.ARTICLE_ID + " = " + article.getId(), null);
			}
            db.setTransactionSuccessful();
            db.endTransaction();

            // Delete related filter
            ArrayList<Filter> filters = getAllFilters();
            db.beginTransaction();
            db.delete(FilterFeedRegistration.TABLE_NAME, FilterFeedRegistration.FEED_ID + " = " + feedId, null);
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
            for (Filter filter : filters) {
                ArrayList<Feed> feeds = filter.feeds();
                if (feeds != null && feeds.size() == 1 && feeds.get(0).getId() == feedId) {
                    // This filter had relation with this feed only
                    db.delete(Filter.TABLE_NAME, Filter.ID + " = " + filter.getId(), null);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
			db.delete(Article.TABLE_NAME, Article.FEEDID + " = " + feedId, null);
			numOfDeleted = db.delete(Feed.TABLE_NAME, Feed.ID + " = " + feedId, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
        return numOfDeleted == 1;
    }

	public Feed getFeedByUrl(String feedUrl) {
		Feed feed = null;
		db.beginTransaction();
        Cursor cur = null;
		try {
			// Get feed
			String getFeedSql = "select _id,title,iconPath,siteUrl from feeds where url = \""
					+ feedUrl + "\"";
			cur = db.rawQuery(getFeedSql, null);
			if (cur.getCount() != 0) {
				cur.moveToNext();
				int feedId = cur.getInt(0);
				String feedTitle = cur.getString(1);
				String iconPath = cur.getString(2);
				String siteUrl = cur.getString(3);

				feed = new Feed(feedId, feedTitle, feedUrl, iconPath, siteUrl, 0);
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
            if (cur != null) {
                cur.close();
            }
		}
		return feed;
	}

	public Feed getFeedById(int feedId) {
		Feed feed = null;
		db.beginTransaction();
        Cursor cur = null;
		try {
			// Get feed
			String[] culumn = {Feed.TITLE, Feed.URL, Feed.ICON_PATH, Feed.SITE_URL};
			String selection = Feed.ID + " = " + feedId;
			cur = db.query(Feed.TABLE_NAME, culumn, selection, null, null, null, null);
			if (cur.getCount() != 0) {
				cur.moveToNext();
				String feedTitle = cur.getString(0);
				String feedUrl = cur.getString(1);
				String iconPath = cur.getString(2);
				String siteUrl = cur.getString(3);

				feed = new Feed(feedId, feedTitle, feedUrl, iconPath, siteUrl, 0);
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
            if (cur != null) {
                cur.close();
            }
			db.endTransaction();
		}
		return feed;
	}

	public Feed saveNewFeed(String feedTitle, String feedUrl, String format, String siteUrl) {
		boolean sameFeedExist = false;
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
				values.put(Feed.ICON_PATH, Feed.DEDAULT_ICON_PATH);
				values.put("siteUrl", siteUrl);
				if (db.insert(Feed.TABLE_NAME, null, values) == -1) {
					Log.v("insert error", "error occurred");
				}
			} else {
				// Same feed already exists
				sameFeedExist = true;
			}
			cursor.close();
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			 e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		
		if (sameFeedExist) {
			return null;
		}
		return getFeedByUrl(feedUrl);
	}

    public int getNumOfUnreadArtilces(int feedId) {
		int num = 0;
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
		
		return num;
	}

	public ArrayList<Article> getAllUnreadArticles(boolean isNewestArticleTop) {
		ArrayList<Article> articles = new ArrayList<>();
		db.beginTransaction();
		try {
			// Get unread articles
			String sql = "select articles._id,articles.title,articles.url,articles.point,articles.date,articles.feedId,feeds.title,feeds.iconPath " +
					"from articles inner join feeds " +
					"where articles.status = \"unread\" and articles.feedId = feeds._id " +
					"order by date ";
			if(isNewestArticleTop) {
				sql += "desc";
			}else {
				sql += "asc";
			}
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = Article.UNREAD;
				String point = cursor.getString(3);
				long dateLong = cursor.getLong(4);
				int feedId = cursor.getInt(5);
				String feedTitle = cursor.getString(6);
				String feedIconPath = cursor.getString(7);
				Article article = new Article(id, title, url, status, point,
						dateLong, feedId, feedTitle, feedIconPath);
				articles.add(article);
			}
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			return articles;
		} finally {
			db.endTransaction();
		}
		
		return articles;
	}
	
	public ArrayList<Article> getTop300Articles(boolean isNewestArticleTop) {
		ArrayList<Article> articles = new ArrayList<>();
		db.beginTransaction();
		try {
			// Get unread articles
			String sql = "select articles._id,articles.title,articles.url,articles.status,articles.point,articles.date,articles.feedId,feeds.title,feeds.iconPath " +
					"from articles inner join feeds " +
					"where articles.feedId = feeds._id " +
					"order by articles._id ";
			if(isNewestArticleTop) {
				sql += "desc";
			}else {
				sql += "asc";
			}
			sql += " limit 300";
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = cursor.getString(3);
				String point = cursor.getString(4);
				long dateLong = cursor.getLong(5);
				int feedId = cursor.getInt(6);
				String feedTitle = cursor.getString(7);
				String feedIconPath = cursor.getString(8);
				Article article = new Article(id, title, url, status, point,
						dateLong, feedId, feedTitle, feedIconPath);
				articles.add(article);
			}
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		return articles;
	}
	
	public ArrayList<Article> searchArticles(String keyword, boolean isNewestArticleTop) {
		ArrayList<Article> articles = new ArrayList<>();
		db.beginTransaction();
		try {
			if(keyword.contains("%")) {
				keyword = keyword.replace("%", "$%");
			}
			if(keyword.contains("_")) {
				keyword = keyword.replace("_", "$_");
			}
			String sql = "select articles._id,articles.title,articles.url,articles.status,articles.point,articles.date,feeds.title,feeds.iconPath " +
					"from articles inner join feeds " +
					"where articles.title like '%" + keyword + "%' escape '$' and " +
					"articles.feedId = feeds._id " +
					"order by date";
			if(isNewestArticleTop) {
				sql += " desc";
			}else {
				sql += " asc";
			}
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = cursor.getString(3);
				String point = cursor.getString(4);
				long dateLong = cursor.getLong(5);
				String feedTitle = cursor.getString(6);
				String feedIconPath = cursor.getString(7);
				Article article = new Article(id, title, url, status, point,
						dateLong, 0, feedTitle, feedIconPath);
				articles.add(article);
			}
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		
		return articles;
	}
	
	public ArrayList<Article> getUnreadArticlesInAFeed(int feedId, boolean isNewestArticleTop) {
		ArrayList<Article> articles = new ArrayList<>();
		db.beginTransaction();
		try {
			// Get unread articles
			String sql = "select _id,title,url,point,date from articles where status = \"unread\" and feedId = "
					+ feedId + " order by date ";
			if(isNewestArticleTop) {
				sql += "desc";
			}else {
				sql += "asc";
			}
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = "unread";
				String point = cursor.getString(3);
				long dateLong = cursor.getLong(4);
				Article article = new Article(id, title, url, status, point,
						dateLong, feedId, null, null);
				articles.add(article);
			}
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			return articles;
		} finally {
			db.endTransaction();
		}
		
		return articles;
	}
	
	public ArrayList<Article> getAllArticlesInAFeed(int feedId, boolean isNewestArticleTop) {
		ArrayList<Article> articles = new ArrayList<>();
		db.beginTransaction();
		try {
			// Get unread articles
			String sql = "select " + Article.ID + ", " + Article.TITLE +  ", " + Article.URL + ", " + Article.STATUS + "" +
					", " + Article.POINT + ", " + Article.DATE + " from " + Article.TABLE_NAME + " where " + Article.FEEDID + " = "
					+ feedId + " order by " + Article.DATE;
			if(isNewestArticleTop) {
				sql += " desc";
			}else {
				sql += " asc";
			}
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = cursor.getString(3);
				String point = cursor.getString(4);
				long dateLong = cursor.getLong(5);
				Article article = new Article(id, title, url, status, point,
						dateLong, feedId, null, null);
				articles.add(article);
			}
			cursor.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			return articles;
		} finally {
			db.endTransaction();
		}
		
		return articles;
	}

	public long getLatestArticleDate(int feedId) {
		long latestDate = 0;
		try {
			String sql = "select " + Article.DATE + " from " + Article.TABLE_NAME +
					" where " + Article.FEEDID + " = " + feedId +
					" order by " + Article.DATE + " desc limit 1";
			Cursor cur = db.rawQuery(sql, null);
			if (cur.getCount() > 0) {
				cur.moveToFirst();
				latestDate = cur.getLong(0);
			}
			cur.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
        return latestDate;
	}

	public ArrayList<Filter> getEnabledFiltersOfFeed(int feedId) {
		ArrayList<Filter> filterList = new ArrayList<>();
		db.beginTransaction();
		try {
			// Get all filters which feed ID is "feedId"
			String[] columns = {
                    Filter.TABLE_NAME + "." + Filter.ID,
                    Filter.TABLE_NAME + "." + Filter.TITLE,
                    Filter.TABLE_NAME + "." + Filter.KEYWORD,
                    Filter.TABLE_NAME + "." + Filter.URL,
                    Filter.TABLE_NAME + "." + Filter.ENABLED
            };
			String condition =
                    FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FEED_ID + " = " + feedId + " and " +
                    FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FILTER_ID + " = " + Filter.TABLE_NAME + "." + Filter.ID + " and " +
                    Filter.TABLE_NAME + "." + Filter.ENABLED + " = " + Filter.TRUE;
			Cursor cur = db.query(Filter.TABLE_NAME + " inner join " + FilterFeedRegistration.TABLE_NAME, columns, condition, null, null,
					null, null);
			// Change to ArrayList
			while (cur.moveToNext()) {
				int id = cur.getInt(0);
				String title = cur.getString(1);
				String keyword = cur.getString(2);
				String url = cur.getString(3);
				int enabled = cur.getInt(4);
				filterList.add(new Filter(id, title, keyword, url, enabled));
			}
			cur.close();
			db.setTransactionSuccessful();
		} catch (Exception e) {
			return null;
		} finally {
			db.endTransaction();
		}
		
		return filterList;
	}

	public Filter getFilterById(int filterId) {
		Filter filter = null;
		db.beginTransaction();
		try {
			String[] columns = {
					Filter.TABLE_NAME + "." + Filter.ID,
                    Filter.TABLE_NAME + "." + Filter.KEYWORD,
                    Filter.TABLE_NAME + "." + Filter.URL,
                    Filter.TABLE_NAME + "." + Filter.TITLE,
                    Filter.TABLE_NAME + "." + Filter.ENABLED,
                    Feed.TABLE_NAME   + "." + Feed.ID,
                    Feed.TABLE_NAME   + "." + Feed.TITLE,
            };
			String condition = Filter.TABLE_NAME + "." + Filter.ID + " = " + filterId + " and " +
                    FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FILTER_ID + " = " +
                    filterId + " and " +
                    FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FEED_ID + " = " +
                    Feed.TABLE_NAME + "." + Feed.ID;
            String table = Filter.TABLE_NAME + " inner join " +
                    FilterFeedRegistration.TABLE_NAME + " inner join " +
                    Feed.TABLE_NAME;
			Cursor cur = db.query(table, columns, condition, null, null, null, null);
            ArrayList<Feed> feeds = new ArrayList<>();
            int id = 0;
            String keyword = null;
            String url = null;
            String title = null;
            int enabled = 0;
            while (cur.moveToNext()) {
                id = cur.getInt(0);
                keyword = cur.getString(1);
                url = cur.getString(2);
                title = cur.getString(3);
                enabled = cur.getInt(4);
                int feedId = cur.getInt(5);
                String feedTitle = cur.getString(6);
                Feed feed = new Feed(feedId, feedTitle);
                feeds.add(feed);
            }
			cur.close();
			db.setTransactionSuccessful();
			filter = new Filter(id, title, keyword, url, feeds, enabled);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}

		return filter;
	}

	public void applyFiltersOfFeed(ArrayList<Filter> filterList, int feedId) {
		// If articles are hit in condition, Set articles status to "read"
		ContentValues value = new ContentValues();
		value.put(Article.STATUS, "read");
		for (Filter filter : filterList) {
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
				db.update(Article.TABLE_NAME, value, condition, null);
				db.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("Apply Filtering", "Article can't be updated.Feed ID = "
						+ feedId);
				db.endTransaction();
				return;
			} finally {
				db.endTransaction();
			}
			
		}
	}

    /**
     * Delete method for specified filter
     *
     * @param filterId Filter ID to delete
     */
	public void deleteFilter(int filterId) {
		db.beginTransaction();
		try {
            String relationWhere = FilterFeedRegistration.FILTER_ID + " = " + filterId;
            db.delete(FilterFeedRegistration.TABLE_NAME, relationWhere, null);
            String filterWhere = Filter.ID + " = " + filterId;
			db.delete(Filter.TABLE_NAME, filterWhere, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

	public int getNumOfFeeds() {
		int num = 0;
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
		
		return num;
	}

    /**
     *
     * Save method for new filter.
     *
     * @param title Filter title
     * @param selectedFeeds Feed set to register the filter
     * @param keyword Filter keyword
     * @param filterUrl Filter URL
     * @return result of all of the database insert
     */
	public boolean saveNewFilter(@NonNull String title, @NonNull ArrayList<Feed> selectedFeeds,
                                 String keyword, String filterUrl) {
		boolean result = true;
		db.beginTransaction();
        Cursor cur = null;
        long newFilterId = INSERT_ERROR_ID;
		try {
			// Check same filter exists in DB
            String[] columns = {
                    Filter.ID,
            };
            String condition = Filter.TITLE + " = '" + title + "' and " +
                    Filter.KEYWORD + " = '" + keyword + "' and " +
                    Filter.URL + " = '" + filterUrl + "'";
            String table = Filter.TABLE_NAME;
            cur = db.query(table, columns, condition, null, null, null, null);
			if (cur.getCount() != 0) {
				Log.i("Register Filter", "Same Filter Exist");
			} else {
				// Register filter
				ContentValues filterVal = new ContentValues();
				filterVal.put(Filter.TITLE, title);
				filterVal.put(Filter.URL, filterUrl);
				filterVal.put(Filter.KEYWORD, keyword);
                filterVal.put(Filter.ENABLED, true);
				newFilterId = db.insert(Filter.TABLE_NAME, null, filterVal);
                if (newFilterId == INSERT_ERROR_ID) {
                    result = false;
                } else {
                    db.setTransactionSuccessful();
                }
			}
		} catch (Exception e) {
			Log.e("insert error", "error occurred");
            e.printStackTrace();
            result = false;
		} finally {
            if (cur != null) cur.close();
			db.endTransaction();
		}
        if (result) {
			db.beginTransaction();
            result = saveFilterFeedRegistration(newFilterId, selectedFeeds);
			if (result) db.setTransactionSuccessful();
			db.endTransaction();
        }

		return result;
	}

    /**
     *
     * Save method for relation between filter and feed set into database.
	 * This method does not have transaction.
     *
     * @param filterId Filter ID
     * @param feeds Feed set to register the filter
     * @return result of all of the database insert
     */
    private boolean saveFilterFeedRegistration(long filterId, @NonNull ArrayList<Feed> feeds) {
        if (filterId < MIN_TABLE_ID) return false;
        boolean result = true;
        for (Feed selectedFeed : feeds) {
            int feedId = selectedFeed.getId();
            if (feedId < MIN_TABLE_ID) {
                result = false;
                break;
            }
            ContentValues val = new ContentValues();
            val.put(FilterFeedRegistration.FEED_ID, feedId);
            val.put(FilterFeedRegistration.FILTER_ID, filterId);
            long id = db.insert(FilterFeedRegistration.TABLE_NAME, null, val);
            if (id == INSERT_ERROR_ID) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Update method for filter.
     *
     * @param filterId Filter ID to update
     * @param title New title
     * @param keyword New keyword
     * @param url New URL
     * @param feeds New feeds to filter
     * @return update result
     */
	public boolean updateFilter(int filterId, String title, String keyword, String url, ArrayList<Feed> feeds) {
		boolean result = false;
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Filter.ID, filterId);
			values.put(Filter.KEYWORD, keyword);
			values.put(Filter.URL, url);
			values.put(Filter.TITLE, title);
			int affectedNum = db.update(Filter.TABLE_NAME, values, Filter.ID + " = " + filterId, null);
			// Same ID filter should not exist and 0 means fail to update
			result = (affectedNum == 1);

			// Delete existing relation between filter and feed
			if (result) {
				String where = FilterFeedRegistration.FILTER_ID + " = " + filterId;
				affectedNum = db.delete(FilterFeedRegistration.TABLE_NAME, where, null);
				result = (affectedNum > 0);
			}

			// Insert new relations
            if (result) {
                result = saveFilterFeedRegistration(filterId, feeds);
            }

            if (result) db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
            result = false;
		} finally {
			db.endTransaction();
		}
		return result;
	}

    public void addManyFeeds() {
		ArrayList<Feed> feeds = new ArrayList<>();
		//RSS 2.0
//		feeds.add(new Feed(0, "スポーツナビ - ピックアップ　ゲーム",
//				"http://sports.yahoo.co.jp/rss/pickup_game/pc"));
//		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - トップ",
//				"http://rss.dailynews.yahoo.co.jp/fc/rss.xml"));
//		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - 海外",
//				"http://rss.dailynews.yahoo.co.jp/fc/world/rss.xml"));
//		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - 経済",
//				"http://rss.dailynews.yahoo.co.jp/fc/economy/rss.xml"));
//		feeds.add(new Feed(0, "Yahoo!ニュース・トピックス - エンターテインメント",
//				"http://rss.dailynews.yahoo.co.jp/fc/entertainment/rss.xml"));
		feeds.add(new Feed(0, "IT速報",
				"http://blog.livedoor.jp/itsoku/index.rdf","", "http://blog.livedoor.jp/itsoku/", 0));
		feeds.add(new Feed(0, "あじゃじゃしたー",
				"http://blog.livedoor.jp/chihhylove/index.rdf","", "http://blog.livedoor.jp/chihhylove/", 0));
		feeds.add(new Feed(0, "はてなブログ人気エントリー",
				"http://b.hatena.ne.jp/hotentry.rss","", "http://b.hatena.ne.jp", 0));
		feeds.add(new Feed(0, "はてなブックマーク - 人気エントリー - テクノロジー",
				"http://b.hatena.ne.jp/hotentry/it.rss","", "http://b.hatena.ne.jp/hotentry", 0));
		feeds.add(new Feed(0, "暇人速報",
				"http://himasoku.com/index.rdf","", "http://himasoku.com", 0));
		feeds.add(new Feed(0, "ドメサカブログ",
				"http://blog.livedoor.jp/domesoccer/index.rdf","", "http://blog.livedoor.jp/domesoccer/", 0));
		feeds.add(new Feed(0, "きんどう",
				"http://kindou.info/feed","", "http://kindou.info", 0));
		feeds.add(new Feed(0, "GGSOKU - ガジェット速報",
				"http://ggsoku.com/feed","", "http://ggsoku.com", 0));
		feeds.add(new Feed(0, "Act as Professional",
				"http://hiroki.jp/feed/","", "http://hiroki.jp", 0));
		feeds.add(new Feed(0, "Developers.IO",
				"http://dev.classmethod.jp/feed/","", "http://dev.classmethod.jp", 0));
		feeds.add(new Feed(0, "GREE Engineers' Blog",
				"http://labs.gree.jp/blog/feed","", "http://labs.gree.jp/blog", 0));
		feeds.add(new Feed(0, "HTC速報",
				"http://htcsoku.info/feed/","", "http://htcsoku.info", 0));
		feeds.add(new Feed(0, "Hatena Developer Blog",
				"http://developer.hatenastaff.com/rss","", "http://developer.hatenastaff.com/", 0));
		feeds.add(new Feed(0, "ITmedia 総合記事一覧",
				"http://rss.rssad.jp/rss/itmtop/2.0/itmedia_all.xml","", "http://www.itmedia.co.jp/", 0));
		feeds.add(new Feed(0, "Publickey",
				"http://www.publickey1.jp/atom.xml","", "http://www.publickey1.jp/", 0));
		feeds.add(new Feed(0, "Tech Booster",
				"http://techbooster.jpn.org/feed/","", "http://techbooster.jpn.org", 0));
		feeds.add(new Feed(0, "TechCrunch Japan",
				"http://jp.techcrunch.com/feed/","", "http://jp.techcrunch.com", 0));
		feeds.add(new Feed(0, "あんどろいど速報",
				"http://androidken.blog119.fc2.com/?xml","", "http://androidken.blog119.fc2.com/", 0));
		feeds.add(new Feed(0, "＠IT 全フォーラム 最新記事一覧",
				"http://www.atmarkit.co.jp/","", "http://rss.rssad.jp/rss/itmatmarkit/rss.xml", 0));
		//atom
//		feeds.add(new Feed(0, "TweetBuzz - 注目エントリー",
//				"http://feeds.feedburner.com/tb-hotentry"));
		//RDF
//		feeds.add(new Feed(0, "二十歳街道まっしぐら",
//				"http://20kaido.com/index.rdf"));

		db.beginTransaction();
		try {

			// If there aren't same feeds in DB,Insert into DB
			for (Feed feed : feeds) {
				saveNewFeed(feed.getTitle(), feed.getUrl(), "RSS2.0", feed.getSiteUrl());
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

	public boolean isArticle(Article article) {
		int num;
		try {
			// Get same article
			String[] columns = {"_id"};
			String selection = "url = ?";
			String[] selectionArgs = {article.getUrl()};
			Cursor cursor = db.query(Article.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
			num = cursor.getCount();
			cursor.close();
		} catch (Exception e) {
			num = -1;
		}

        return num > 0;
    }

	public void deleteAllArticles() {
		db.beginTransaction();
		try {
			db.delete(Article.TABLE_NAME, "", null);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

    /**
     * Helper method to delete all of the feeds.
     * This method also deletes relation between filter and feed.
     */
	public void deleteAll() {
		try {
            db.beginTransaction();
            db.delete(CurationCondition.TABLE_NAME, null, null);
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
            db.delete(CurationSelection.TABLE_NAME, null, null);
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
            db.delete(Article.TABLE_NAME, null, null);
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
            db.delete(FilterFeedRegistration.TABLE_NAME, null, null);
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
			db.delete(Filter.TABLE_NAME, null, null);
			db.setTransactionSuccessful();
			db.endTransaction();

            db.beginTransaction();
            db.delete(Curation.TABLE_NAME, null, null);
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
			db.delete(Feed.TABLE_NAME, null, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

    /**
     * Helper method to retrieve all of the filters.
     *
     * @return all of the filters in the database
     */
	public ArrayList<Filter> getAllFilters() {
		ArrayList<Filter> filters = new ArrayList<>();
		String[] columns = {
				Filter.TABLE_NAME + "." + Filter.ID,
				Filter.TABLE_NAME + "." + Filter.TITLE,
				Filter.TABLE_NAME + "." + Filter.KEYWORD,
				Filter.TABLE_NAME + "." + Filter.URL,
				Filter.TABLE_NAME + "." + Filter.ENABLED,
                Feed.TABLE_NAME + "." + Feed.ID,
				Feed.TABLE_NAME + "." + Feed.TITLE};
		String selection = Filter.TABLE_NAME + "." + Filter.ID + "=" +
				FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FILTER_ID + " and " +
                FilterFeedRegistration.TABLE_NAME + "." + FilterFeedRegistration.FEED_ID + "=" +
                Feed.TABLE_NAME + "." + Feed.ID;
		db.beginTransaction();
		try {
			String table = Filter.TABLE_NAME + " inner join " +
                    FilterFeedRegistration.TABLE_NAME + " inner join " + Feed.TABLE_NAME;
            Cursor cursor = db.query(table, columns, selection, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                Filter filter;
                ArrayList<Feed> feeds = new ArrayList<>();
                int filterId = cursor.getInt(0);
                String title = cursor.getString(1);
                String keyword = cursor.getString(2);
                String url = cursor.getString(3);
                int enabled = cursor.getInt(4);
                int feedId = cursor.getInt(5);
                String feedTitle = cursor.getString(6);
                feeds.add(new Feed(feedId, feedTitle));
				while (cursor.moveToNext()) {
                    int cursorFilterId = cursor.getInt(0);
                    if (filterId != cursorFilterId) {
                        filter = new Filter(filterId, title, keyword, url, feeds, enabled);
                        filters.add(filter);
                        filterId = cursorFilterId;
                        feeds = new ArrayList<>();
                    }
                    title = cursor.getString(1);
                    keyword = cursor.getString(2);
                    url = cursor.getString(3);
                    enabled = cursor.getInt(4);
                    feedId = cursor.getInt(5);
                    feedTitle = cursor.getString(6);
                    feeds.add(new Feed(feedId, feedTitle));
				}
                filter = new Filter(filterId, title, keyword, url, feeds, enabled);
                filters.add(filter);
				cursor.close();
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
		return filters;
	}

	public void updateFilterEnabled(int id, boolean isEnabled) {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(Filter.ENABLED, isEnabled ? Filter.TRUE : Filter.FALSE);
			db.update(Filter.TABLE_NAME, values, Filter.ID + " = " + id, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

	public void exportDb(@NonNull File currentDB) {
		try {
			File backupStrage;
			String sdcardRootPath = FileUtil.INSTANCE.getSdCardRootPath();
            Log.d(LOG_TAG, "SD Card path: " + sdcardRootPath);
			if (FileUtil.INSTANCE.isSDCardMouted(sdcardRootPath)) {
				Log.d(LOG_TAG, "SD card is mounted");
				backupStrage = new File(FileUtil.INSTANCE.getSdCardRootPath());
			}else {
				Log.d(LOG_TAG, "not mounted");
				backupStrage = Environment.getExternalStorageDirectory();
			}
			if (backupStrage.canWrite()) {
				Log.d(LOG_TAG, "Backup storage is writable");

				String backupDBFolderPath = BACKUP_FOLDER + "/";
				File backupDBFolder = new File(backupStrage, backupDBFolderPath);
                if (backupDBFolder.exists()) {
                    if (backupDBFolder.delete()) {
                        Log.d(LOG_TAG, "Succeeded to delete backup directory");
                    } else {
                        Log.d(LOG_TAG, "Failed to delete backup directory");
                    }
                }
                if (backupDBFolder.mkdir()) {
                    Log.d(LOG_TAG, "Succeeded to make directory");
                } else {
                    Log.d(LOG_TAG, "Failed to make directory");
                }
				File backupDB = new File(backupStrage, backupDBFolderPath + DatabaseHelper.DATABASE_NAME);

				// Copy database
				FileChannel src = new FileInputStream(currentDB).getChannel();
				FileChannel dst = new FileOutputStream(backupDB).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			} else {
                // TODO Runtime Permission
                Log.d(LOG_TAG, "SD Card is not writabble, enable storage permission in Android setting");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void importDB(@NonNull File currentDB) {
		try {
			File backupStrage;
			String sdcardRootPath = FileUtil.INSTANCE.getSdCardRootPath();
			if (FileUtil.INSTANCE.isSDCardMouted(sdcardRootPath)) {
				Log.d(LOG_TAG, "SD card is mounted");
				backupStrage = new File(FileUtil.INSTANCE.getSdCardRootPath());
			}else {
				Log.d(LOG_TAG, "not mounted");
				backupStrage = Environment.getExternalStorageDirectory();
				Log.d(LOG_TAG, "path:" + backupStrage.getAbsolutePath());
			}
			if (backupStrage.canRead()) {
				Log.d(LOG_TAG, "Backup storage is readable");

				String backupDBPath = BACKUP_FOLDER + "/" + DatabaseHelper.DATABASE_NAME;
				File newDB  = new File(backupStrage, backupDBPath);
				if (!newDB.exists()) {
					return;
				}

				FileChannel src = new FileInputStream(newDB).getChannel();
				FileChannel dst = new FileOutputStream(currentDB).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			} else {
                // TODO Runtime Permission
                Log.d(LOG_TAG, "SD Card is not readabble, enable storage permission in Android setting");
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean updateCuration(int curationId, String name, ArrayList<String> words) {
		boolean result = true;
		db.beginTransaction();
		try {
			// Update curation name
			ContentValues values = new ContentValues();
			values.put(Curation.NAME, name);
			db.update(Curation.TABLE_NAME, values, Curation.ID + " = " + curationId, null);

			// Delete old curation conditions and insert new one
			db.delete(CurationCondition.TABLE_NAME, CurationCondition.CURATION_ID + " = " + curationId, null);
			for (String word : words) {
				ContentValues condtionValue = new ContentValues();
				condtionValue.put(CurationCondition.CURATION_ID, curationId);
				condtionValue.put(CurationCondition.WORD, word);
				db.insert(CurationCondition.TABLE_NAME, null, condtionValue);
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
			result = false;
		} finally {
			db.endTransaction();
		}
		return result;
	}

	public boolean saveNewCuration(String name, ArrayList<String> words) {
		if(words.isEmpty()) {
			return false;
		}
		db.beginTransaction();
		boolean result = true;
		try {
			ContentValues values = new ContentValues();
			values.put(Curation.NAME, name);
			long addedCurationId = db.insert(Curation.TABLE_NAME, null, values);
			for (String word : words) {
				ContentValues condtionValue = new ContentValues();
				condtionValue.put(CurationCondition.CURATION_ID, addedCurationId);
				condtionValue.put(CurationCondition.WORD, word);
				db.insert(CurationCondition.TABLE_NAME, null, condtionValue);
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
			result = false;
		} finally {
			db.endTransaction();
		}
		return result;
	}

	public boolean adaptCurationToArticles(String curationName, ArrayList<String> words) {
		int curationId = getCurationIdByName(curationName);
		if (curationId == NOT_FOUND_ID) {
			return false;
		}

		boolean result = true;
		SQLiteStatement insertSt = db
				.compileStatement("insert into " + CurationSelection.TABLE_NAME +
						"(" + CurationSelection.ARTICLE_ID + "," + CurationSelection.CURATION_ID + ") values (?," + curationId + ");");
		db.beginTransaction();
        Cursor cursor = null;
		try {
			// Delete old curation selection
			db.delete(CurationSelection.TABLE_NAME, CurationSelection.CURATION_ID + " = " + curationId, null);

			// Get all articles
			String[] columns = {Article.ID, Article.TITLE};
			cursor = db.query(Article.TABLE_NAME, columns, null, null, null, null, null);

			// Adapt
			while (cursor.moveToNext()) {
				int articleId = cursor.getInt(0);
				String articleTitle = cursor.getString(1);
				for (String word : words) {
					if (articleTitle.contains(word)) {
						insertSt.bindString(1, String.valueOf(articleId));
						insertSt.executeInsert();
						break;
					}
				}
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
			result = false;
		} finally {
            if (cursor != null) {
                cursor.close();
            }
			db.endTransaction();
		}
		return result;
	}

	public ArrayList<Curation> getAllCurations() {
		ArrayList<Curation> curationList = new ArrayList<>();
		db.beginTransaction();
		try {
			String[] columns = {Curation.ID, Curation.NAME};
			String orderBy = Curation.NAME;
			Cursor cursor = db.query(Curation.TABLE_NAME, columns, null, null, null, null, orderBy);
			if (cursor != null) {
				while (cursor.moveToNext()) {
					int id = cursor.getInt(0);
					String name = cursor.getString(1);
					curationList.add(new Curation(id, name));
				}
				cursor.close();
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}

		return curationList;
	}

	public boolean deleteCuration(int curationId) {
		int numOfDeleted = 0;
		db.beginTransaction();
		try {
			db.delete(CurationCondition.TABLE_NAME, CurationCondition.CURATION_ID + " = " + curationId, null);
			db.delete(CurationSelection.TABLE_NAME, CurationSelection.CURATION_ID + " = " + curationId, null);
			numOfDeleted = db.delete(Curation.TABLE_NAME, Curation.ID + " = " + curationId, null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
        return numOfDeleted == 1;
    }

	public boolean deleteAllCuration() {
		boolean result = true;
		db.beginTransaction();
		try {
			db.delete(CurationCondition.TABLE_NAME, "", null);
			db.delete(CurationSelection.TABLE_NAME, "", null);
			db.delete(Curation.TABLE_NAME, "", null);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			e.printStackTrace();
			result = false;
		} finally {
			db.endTransaction();
		}
		return result;
	}

	public boolean isExistSameNameCuration(String name) {
		int num = 0;
		try {
			String[] columns = {Curation.ID};
			String selection = Curation.NAME + " = ?";
			String[] selectionArgs = {name};
			Cursor cursor = db.query(Curation.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
			num = cursor.getCount();
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return num > 0;
	}

	public int getCurationIdByName(String name) {
		int id = NOT_FOUND_ID;
		try {
			String[] columns = {Curation.ID};
			String selection = Curation.NAME + " = '' || ? || ''";
			String[] selectionArgs = {name};
			Cursor cursor = db.query(Curation.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
			cursor.moveToFirst();
			id = cursor.getInt(0);
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return id;
	}

	public String getCurationNameById(int curationId) {
		String name = "";
		try {
			String[] columns = {Curation.NAME};
			String selection = Curation.ID + " = ?";
			String[] selectionArgs = {String.valueOf(curationId)};
			Cursor cursor = db.query(Curation.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
			cursor.moveToFirst();
			name = cursor.getString(0);
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return name;
	}

	public ArrayList<Article> getAllUnreadArticlesOfCuration(int curationId, boolean isNewestArticleTop) {
		ArrayList<Article> articles = new ArrayList<>();
		String sql = "select " + Article.TABLE_NAME + "." + Article.ID + "," +
				Article.TABLE_NAME + "." + Article.TITLE + "," +
				Article.TABLE_NAME + "." + Article.URL + "," +
				Article.TABLE_NAME + "." + Article.STATUS + "," +
				Article.TABLE_NAME + "." + Article.POINT + "," +
				Article.TABLE_NAME + "." + Article.DATE + "," +
				Article.TABLE_NAME + "." + Article.FEEDID + "," +
				Feed.TABLE_NAME + "." + Feed.TITLE + "," +
				Feed.TABLE_NAME + "." + Feed.ICON_PATH +
				" from (" + Article.TABLE_NAME + " inner join " + CurationSelection.TABLE_NAME +
				" on " + CurationSelection.CURATION_ID + " = " + curationId + " and " +
				Article.TABLE_NAME + "." + Article.STATUS + " = '" + Article.UNREAD + "' and " +
				Article.TABLE_NAME + "." + Article.ID + " = " + CurationSelection.TABLE_NAME + "." + CurationSelection.ARTICLE_ID + ")" +
				" inner join " + Feed.TABLE_NAME +
				" on " + Article.TABLE_NAME + "." + Article.FEEDID + " = " + Feed.TABLE_NAME + "." + Feed.ID +
				" order by " + Article.DATE;
		if(isNewestArticleTop) {
			sql += " desc";
		}else {
			sql += " asc";
		}
		try {
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = cursor.getString(3);
				String point = cursor.getString(4);
				long dateLong = cursor.getLong(5);
				int feedId = cursor.getInt(6);
				String feedTitle = cursor.getString(7);
				String feedIconPath = cursor.getString(8);
				Article article = new Article(id, title, url, status, point,
						dateLong, feedId, feedTitle, feedIconPath);
				articles.add(article);
			}
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return articles;
	}
	public int calcNumOfAllUnreadArticlesOfCuration(int curationId) {
		int num = 0;
		String sql = "select " + Article.TABLE_NAME + "." + Article.ID + "," +
				Article.TABLE_NAME + "." + Article.TITLE + "," +
				Article.TABLE_NAME + "." + Article.URL + "," +
				Article.TABLE_NAME + "." + Article.STATUS + "," +
				Article.TABLE_NAME + "." + Article.POINT + "," +
				Article.TABLE_NAME + "." + Article.DATE + "," +
				Article.TABLE_NAME + "." + Article.FEEDID +
				" from " + Article.TABLE_NAME + " inner join " + CurationSelection.TABLE_NAME +
				" where " + CurationSelection.CURATION_ID + " = " + curationId + " and " +
				Article.TABLE_NAME + "." + Article.STATUS + " = '" + Article.UNREAD + "' and " +
				Article.TABLE_NAME + "." + Article.ID + " = " + CurationSelection.TABLE_NAME + "." + CurationSelection.ARTICLE_ID +
				" order by " + Article.DATE;
		try {
			Cursor cursor = db.rawQuery(sql, null);
			num = cursor.getCount();
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return num;
	}

	public ArrayList<String> getCurationWords(int curationId) {
		ArrayList<String> words = new ArrayList<>();
		String[] columns = {CurationCondition.WORD};
		String selection = CurationCondition.CURATION_ID + " = ?";
		String[] selectionArgs = {String.valueOf(curationId)};
		try {
			Cursor cursor = db.query(CurationCondition.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
			while (cursor.moveToNext()) {
				words.add(cursor.getString(0));
			}
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return words;
	}

	public ArrayList<Article> getAllArticlesOfCuration(int curationId, boolean isNewestArticleTop) {
		ArrayList<Article> articles = new ArrayList<>();
		String sql = "select " + Article.TABLE_NAME + "." + Article.ID + "," +
				Article.TABLE_NAME + "." + Article.TITLE + "," +
				Article.TABLE_NAME + "." + Article.URL + "," +
				Article.TABLE_NAME + "." + Article.STATUS + "," +
				Article.TABLE_NAME + "." + Article.POINT + "," +
				Article.TABLE_NAME + "." + Article.DATE + "," +
				Article.TABLE_NAME + "." + Article.FEEDID + "," +
				Feed.TABLE_NAME + "." + Feed.TITLE + "," +
				Feed.TABLE_NAME + "." + Feed.ICON_PATH +
				" from (" + Article.TABLE_NAME + " inner join " + CurationSelection.TABLE_NAME +
				" on " + CurationSelection.CURATION_ID + " = " + curationId + " and " +
				Article.TABLE_NAME + "." + Article.ID + " = " + CurationSelection.TABLE_NAME + "." + CurationSelection.ARTICLE_ID + ")" +
				" inner join " + Feed.TABLE_NAME +
				" on " + Article.TABLE_NAME + "." + Article.FEEDID + " = " + Feed.TABLE_NAME + "." + Feed.ID +
				" order by " + Article.DATE;
		if(isNewestArticleTop) {
			sql += " desc";
		}else {
			sql += " asc";
		}
		try {
			Cursor cursor = db.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int id = cursor.getInt(0);
				String title = cursor.getString(1);
				String url = cursor.getString(2);
				String status = cursor.getString(3);
				String point = cursor.getString(4);
				long dateLong = cursor.getLong(5);
				int feedId = cursor.getInt(6);
				String feedTitle = cursor.getString(7);
				String feedIconPath = cursor.getString(8);
				Article article = new Article(id, title, url, status, point,
						dateLong, feedId, feedTitle, feedIconPath);
				articles.add(article);
			}
			cursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        return articles;
	}

    public SparseArray<ArrayList<String>> getAllCurationWords() {
//	public Map<Integer, ArrayList<String>> getAllCurationWords() {
//		Map<Integer, ArrayList<String>> curationWordsMap = new HashMap<>();
        SparseArray<ArrayList<String>> curationWordsMap = new SparseArray<>();
		String sql = "select " + Curation.TABLE_NAME + "." + Curation.ID + "," +
				CurationCondition.TABLE_NAME + "." + CurationCondition.WORD +
				" from " + Curation.TABLE_NAME + " inner join " + CurationCondition.TABLE_NAME +
				" where " + Curation.TABLE_NAME + "." + Curation.ID + " = " + CurationCondition.TABLE_NAME + "." + CurationCondition.CURATION_ID +
				" order by " + Curation.TABLE_NAME + "." + Curation.ID;
		Cursor cursor = null;
		try {
			cursor = db.rawQuery(sql, null);
			final int defaultCurationId = -1;
			int curationId = defaultCurationId;
			ArrayList<String> words = new ArrayList<>();
			while (cursor.moveToNext()) {
				int newCurationId = cursor.getInt(0);
				if (curationId == defaultCurationId) {
					curationId = newCurationId;
				}
				// Add words of curation to map when curation ID changes
				if (curationId != newCurationId) {
					curationWordsMap.put(curationId, words);
					curationId = newCurationId;
					words = new ArrayList<>();
				}
				String word = cursor.getString(1);
				words.add(word);
			}
			// Add last words of curation
			if (curationId != defaultCurationId) {
				curationWordsMap.put(curationId, words);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return curationWordsMap;
	}
}

package com.phicdy.mycuration.data.filter;


import com.phicdy.mycuration.data.rss.Feed;

public class FilterFeedRegistration {
	public static final String TABLE_NAME = "filterFeedRegistrations";
	private static final String ID = "_id";
	public static final String FEED_ID = "feedId";
	public static final String FILTER_ID = "filterId";

	public static final String CREATE_TABLE_SQL =
			"create table " + TABLE_NAME + "(" +
					ID + " integer primary key autoincrement," +
					FILTER_ID + " integer," +
					FEED_ID + " integer," +
					"foreign key(" + FILTER_ID + ") references " + Filter.TABLE_NAME + "(" + Filter.ID + ")," +
					"foreign key(" + FEED_ID + ") references " + Feed.TABLE_NAME + "(" + Feed.ID + ")" +
					")";

    public static final String DROP_TABLE_SQL = "DROP TABLE " + TABLE_NAME;
}

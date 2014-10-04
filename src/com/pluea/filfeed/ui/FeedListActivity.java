package com.pluea.filfeed.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.pluea.filfeed.R;
import com.pluea.filfeed.alarm.AlarmManagerTaskManager;
import com.pluea.filfeed.db.DatabaseAdapter;
import com.pluea.filfeed.rss.Feed;
import com.pluea.filfeed.task.GetFeedIconTask;
import com.pluea.filfeed.task.InsertNewFeedTask;
import com.pluea.filfeed.task.UpdateTaskManager;

public class FeedListActivity extends Activity {

	private ArrayList<Feed> feeds = new ArrayList<Feed>();
	private DatabaseAdapter dbAdapter = new DatabaseAdapter(this);
	private PullToRefreshListView feedsListView;
	private TextView showNoUnread;
	private RssFeedListAdapter rssFeedListAdapter;
	private BroadcastReceiver receiver;
	private Intent intent;
	private UpdateTaskManager updateTaskManager;

	private static final int DELETEFEEDMENUID = 0;
	public static final int BAD_RECIEVED_VALUE = -1;
	public static final String FEED_ID = "FEED_ID";
	public static final String FEED_URL = "FEED_URL";
	public static final String UPDATE_NUM_OF_ARTICLES = "UPDATE_NUM_OF_ARTICLES";
	private static final String LOG_TAG = "RSSREADER."
			+ FeedListActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		feedsListView = (PullToRefreshListView) findViewById(R.id.feedList);
		
		setAllListener();
		setBroadCastReceiver();
		setAlarmManager();
		
		if(dbAdapter.getNumOfFeeds() == 0) {
			dbAdapter.addManyFeeds();
		}
		feeds = dbAdapter.getAllFeeds();
		getFeedIconIfNeeded(feeds);
		
		registerForContextMenu(feedsListView.getRefreshableView());
	}

	private void setAllListener() {
		// When an feed selected, display unread articles in the feed
		feedsListView
				.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						intent = new Intent(FeedListActivity.this,
								ArticlesListActivity.class);
						intent.putExtra(FEED_ID, feeds.get(position-1).getId());
						intent.putExtra(FEED_URL, feeds.get(position-1).getUrl());
						startActivity(intent);
					}

				});
		feedsListView.setOnRefreshListener(new OnRefreshListener<ListView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				updateAllFeeds();
			}
		});
		
		TextView allUnread = (TextView)findViewById(R.id.allUnreadFeed);
		allUnread.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				intent = new Intent(FeedListActivity.this,
						ArticlesListActivity.class);
				startActivity(intent);
			}
		});
		
		showNoUnread = (TextView)findViewById(R.id.showNoUnread);
		showNoUnread.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				feeds = dbAdapter.getAllFeeds();
				updateNumOfUnreadArticles(false);

				// Set ListView
				rssFeedListAdapter = new RssFeedListAdapter(feeds);
				feedsListView.setAdapter(rssFeedListAdapter);
			}
		});
	}

	private void setBroadCastReceiver() {
		// receive num of unread articles from Update Task
		receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// Set num of unread articles and update UI
				if (intent.getAction().equals(UPDATE_NUM_OF_ARTICLES)) {
					Log.d(LOG_TAG, "onReceive");
					if(!UpdateTaskManager.getInstance(getApplicationContext()).isUpdating()) {
						feedsListView.onRefreshComplete();
						updateNumOfUnreadArticles(true);
					}
				}
			}
		};

		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_NUM_OF_ARTICLES);
		registerReceiver(receiver, filter);

	}

	private void setAlarmManager() {
		// Start auto update alarmmanager
		AlarmManagerTaskManager.setNewAlarm(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.addFeed:
			addFeed();
			break;
		case R.id.addFilter:
			intent = new Intent(FeedListActivity.this, FilterList.class);
			startActivity(intent);
			break;
		case R.id.setting:
			startActivity(new Intent(getApplicationContext(), SettingActivity.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		// Menu.add(int groupId, int itemId, int order, CharSequence title)
		menu.add(0, DELETEFEEDMENUID, 0, R.string.delete_feed);
	}

	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		switch (item.getItemId()) {
		case DELETEFEEDMENUID:
			Feed selectedFeed = feeds.get(info.position-1);
			dbAdapter.deleteFeed(selectedFeed.getId());
			feeds.remove(info.position-1);
			rssFeedListAdapter.notifyDataSetChanged();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
//		updateAllFeeds();
		updateNumOfUnreadArticles(true);

		// Set ListView
		rssFeedListAdapter = new RssFeedListAdapter(feeds);
		feedsListView.setAdapter(rssFeedListAdapter);
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}

	private void addFeed() {
		final View addView = getLayoutInflater().inflate(R.layout.add_feed,
				null);

		new AlertDialog.Builder(this)
				.setTitle(R.string.add_feed)
				.setView(addView)
				.setPositiveButton(R.string.register,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Set feed URL and judge whether feed URL is
								// RSS format
								EditText feedUrl = (EditText) addView
										.findViewById(R.id.addFeedUrl);
								String feedUrlStr = feedUrl.getText()
										.toString();

								InsertNewFeedTask task = new InsertNewFeedTask(
										getApplicationContext());
								task.execute(feedUrlStr);

								// Update feed list
								Feed newFeed;
								try {
									newFeed = task.get();
									if (newFeed == null) {
										Log.w("add a new feed",
												"Can't get feed url = "
														+ feedUrlStr);
										Toast.makeText(FeedListActivity.this,
												R.string.add_feed_error,
												Toast.LENGTH_SHORT).show();
									} else {
										// add new feed and notify to adapter
										feeds.add(newFeed);
										rssFeedListAdapter
												.notifyDataSetChanged();

										//TODO Don't show message
									}
								} catch (InterruptedException e) {
									e.printStackTrace();
								} catch (ExecutionException e) {
									e.printStackTrace();
								}

							}

						}).setNegativeButton(R.string.cancel, null).show();
	}

	private void updateAllFeeds() {
		if (feeds.isEmpty()) {
			feedsListView.onRefreshComplete();
			return;
		} 
		updateTaskManager = UpdateTaskManager.getInstance(getApplicationContext());

		// Get feeds from DB if other update task is not running
		if (updateTaskManager.updateAllFeeds(feeds)) {
		} else {
			feedsListView.onRefreshComplete();
		}
	}

	private void updateNumOfUnreadArticles(boolean isHide) {
		feeds = dbAdapter.getAllFeeds();
		if (feeds.isEmpty()) {
			return;
		}
		ArrayList<Feed> hideList = new ArrayList<Feed>(); 
		for (Feed feed : feeds) {
			int numOfUnreadArticles = dbAdapter.getNumOfUnreadArtilces(feed
					.getId());
			if(numOfUnreadArticles == 0) {
				hideList.add(feed);
			}else {
				feed.setUnreadArticlesCount(numOfUnreadArticles);
			}
		}
		if(feeds.size() == hideList.size()) {
			showNoUnread.setVisibility(View.GONE);
		}else if(isHide) {
			showNoUnread.setVisibility(View.VISIBLE);
			for(Feed feed : hideList) {
				feeds.remove(feed);
			}
		}
		
		if(rssFeedListAdapter != null) {
			rssFeedListAdapter.notifyDataSetChanged();
		}
	}

	private void getFeedIconIfNeeded(ArrayList<Feed> feeds) {
		for (Feed feed : feeds) {
			if(feed.getIconPath() == null || feed.getIconPath().equals(Feed.DEDAULT_ICON_PATH)) {
				GetFeedIconTask task = new GetFeedIconTask(getApplicationContext());
				task.execute(feed.getSiteUrl());
			}
		}
	}
	
	/**
	 * 
	 * @author kyamaguchi Display RSS Feeds List
	 */
	class RssFeedListAdapter extends ArrayAdapter<Feed> {
		public RssFeedListAdapter(ArrayList<Feed> feeds) {
			super(FeedListActivity.this, R.layout.feeds_list, feeds);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			// Use contentView
			View row = convertView;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.feeds_list, parent, false);
			}

			Feed feed = this.getItem(position);

			ImageView feedIcon = (ImageView)row.findViewById(R.id.feedIcon);
			String iconPath = feed.getIconPath();
			if(iconPath != null && !iconPath.equals(Feed.DEDAULT_ICON_PATH)) {
				File file = new File(iconPath);
			    if (file.exists()) {
		            Bitmap bmp = BitmapFactory.decodeFile(file.getPath());
		            feedIcon.setImageBitmap(bmp); 
			    }
			}
			
			// set RSS Feed title
			TextView feedTitle = (TextView) row.findViewById(R.id.feedTitle);
			feedTitle.setText(feed.getTitle());

			// set RSS Feed unread article count
			TextView feedCount = (TextView) row.findViewById(R.id.feedCount);
			feedCount.setText(String.valueOf(feed.getUnreadAriticlesCount()));

			return (row);
		}

	}
}
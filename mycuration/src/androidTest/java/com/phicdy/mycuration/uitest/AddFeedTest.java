package com.phicdy.mycuration.uitest;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import com.phicdy.mycuration.BuildConfig;
import com.phicdy.mycuration.db.DatabaseAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class AddFeedTest {

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(context);
        adapter.deleteAll();
    }

    @After
    public void tearDown() {
        Context context = InstrumentationRegistry.getTargetContext();
        DatabaseAdapter adapter = DatabaseAdapter.getInstance(context);
        adapter.deleteAll();
    }

    @Test
    public void addYahooNews() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        // Launch MainActivity
        Context context = InstrumentationRegistry.getContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        // Go to feed tab
        List<UiObject2> tabs = device.wait(Until.findObjects(
                By.clazz(android.support.v7.app.ActionBar.Tab.class)), 5000);
        if (tabs == null) fail("Tab was not found");
        if (tabs.size() != 3) fail("Tab size was invalid, size: " + tabs.size());
        tabs.get(1).click();

        // Click plus button
        UiObject2 plusButton = device.findObject(By.res(BuildConfig.APPLICATION_ID, "add"));
        if (plusButton == null) fail("Plus button was not found");
        plusButton.click();

        // Open yahoo RSS URL
        UiObject2 urlEditText = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "search_src_text")), 5000);
        if (urlEditText == null) fail("URL edit text was not found");
        urlEditText.setText("http://news.yahoo.co.jp/pickup/rss.xml");
        device.pressEnter();

        // Assert yahoo RSS was added
        List<UiObject2> feedTitles = device.wait(Until.findObjects(
                By.res(BuildConfig.APPLICATION_ID, "feedTitle")), 5000);
        if (feedTitles == null) fail("Feed was not found");
        // Feed title list includes show/hide option row, the size is 2
        if (feedTitles.size() != 2) fail("Feed was not added");
        assertThat(feedTitles.get(0).getText(), is("Yahoo!ニュース・トピックス - トップ"));
        assertThat(feedTitles.get(1).getText(), is("全てのRSSを表示"));

        // Assert articles of yahoo RSS were added
        List<UiObject2> feedUnreadCountList = device.wait(Until.findObjects(
                By.res(BuildConfig.APPLICATION_ID, "feedCount")), 5000);
        if (feedUnreadCountList == null) fail("Feed count was not found");
        // Feed count list does not include show/hide option row, the size is 1
        if (feedUnreadCountList.size() != 1) fail("Feed count was not added");
        assertThat(Integer.valueOf(feedUnreadCountList.get(0).getText()), greaterThan(0));
    }

    @Test
    public void tryInvalidUrl() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        // Launch MainActivity
        Context context = InstrumentationRegistry.getContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        // Go to feed tab
        List<UiObject2> tabs = device.wait(Until.findObjects(
                By.clazz(android.support.v7.app.ActionBar.Tab.class)), 5000);
        if (tabs == null) fail("Tab was not found");
        if (tabs.size() != 3) fail("Tab size was invalid, size: " + tabs.size());
        tabs.get(1).click();

        // Click plus button
        UiObject2 plusButton = device.findObject(By.res(BuildConfig.APPLICATION_ID, "add"));
        if (plusButton == null) fail("Plus button was not found");
        plusButton.click();

        // Open invalid RSS URL
        UiObject2 urlEditText = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "search_src_text")), 5000);
        if (urlEditText == null) fail("URL edit text was not found");
        urlEditText.setText("http://ghaorgja.co.jp/rss.xml");
        device.pressEnter();

        UiObject2 emptyView = device.findObject(By.res(BuildConfig.APPLICATION_ID, "emptyView"));
        assertThat(emptyView.getText(), is("まずはRSSを登録しましょう！"));
    }

    @Test
    public void clickFabWithoutUrlOpen() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        // Launch MainActivity
        Context context = InstrumentationRegistry.getContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(BuildConfig.APPLICATION_ID);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        // Go to feed tab
        List<UiObject2> tabs = device.wait(Until.findObjects(
                By.clazz(android.support.v7.app.ActionBar.Tab.class)), 5000);
        if (tabs == null) fail("Tab was not found");
        if (tabs.size() != 3) fail("Tab size was invalid, size: " + tabs.size());
        tabs.get(1).click();

        // Click plus button
        UiObject2 plusButton = device.findObject(By.res(BuildConfig.APPLICATION_ID, "add"));
        if (plusButton == null) fail("Plus button was not found");
        plusButton.click();

        // Open invalid RSS URL
        UiObject2 fab = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "fab")), 5000);
        if (fab == null) fail("Fab was not found");
        fab.click();

        // Fab still exists
        fab = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "fab")), 5000);
        assertNotNull(fab);
    }
}
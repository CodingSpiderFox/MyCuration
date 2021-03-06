package com.phicdy.mycuration.uitest;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.StaleObjectException;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.phicdy.mycuration.BuildConfig;
import com.phicdy.mycuration.presentation.view.activity.TopActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class SettingTest extends UiTest {

    @Rule
    public ActivityTestRule<TopActivity> activityTestRule = new ActivityTestRule<>(TopActivity.class);

    @Before
    public void setup() {
        super.setup(activityTestRule.getActivity());

        Context context = InstrumentationRegistry.getTargetContext();
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.wait(Until.findObject(By.res(BuildConfig.APPLICATION_ID, "add")), 5000);

        // Go to feed tab
        @SuppressWarnings("deprecation")
        List<UiObject2> tabs = device.findObjects(
                By.clazz(android.support.v7.app.ActionBar.Tab.class));
        if (tabs == null) fail("Tab was not found");
        if (tabs.size() != 3) fail("Tab size was invalid, size: " + tabs.size());
        takeScreenshot(device);
        tabs.get(1).click();

        TopActivityControl.clickAddRssButton();

        // Show edit text for URL if needed
        UiObject2 searchButton = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "search_button")), 5000);
        if (searchButton != null) searchButton.click();

        // Open yahoo RSS URL
        UiObject2 urlEditText = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "search_src_text")), 5000);
        if (urlEditText == null) fail("URL edit text was not found");
        urlEditText.setText("http://news.yahoo.co.jp/pickup/rss.xml");
        device.pressEnter();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void openWithInternalBrowser() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Click setting button
        UiObject2 settingButton = device.wait(
                Until.findObject(By.res(BuildConfig.APPLICATION_ID, "setting_top_activity")), 5000);
        if (settingButton == null) fail("Setting button was not found");
        settingButton.clickAndWait(Until.newWindow(), 5000);

        // Enable internal browser
        UiObject2 settingsList = device.findObject(By.clazz(ListView.class));
        List<UiObject2> settings = settingsList.findObjects(By.clazz(LinearLayout.class).depth(1));
        for (UiObject2 setting : settings) {
            UiObject2 text = setting.findObject(
                    By.res("android:id/title"));
            if (text.getText().equals("内蔵ブラウザで開く")) {
                UiObject2 browserSwitch = setting.findObject(
                        By.res("android:id/switch_widget"));
                if (browserSwitch == null)
                    browserSwitch = setting.findObject(
                            By.res("android:id/switchWidget"));
                if (browserSwitch != null && !browserSwitch.isChecked()) {
                    browserSwitch.click();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        device.pressBack();

        // Click first feed
        List<UiObject2> feedTitles = device.wait(Until.findObjects(
                By.res(BuildConfig.APPLICATION_ID, "feedTitle")), 5000);
        if (feedTitles == null) fail("Feed was not found");
        feedTitles.get(0).clickAndWait(Until.newWindow(), 5000);

        // Click first article
        UiObject2 articleList = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "rv_article")), 5000);
        UiObject2 firstArticle = articleList.findObject(By.clazz(LinearLayout.class));
        firstArticle.click();

        // Assert share button in internal browser exist
        UiObject2 shareButton = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "menu_item_share")), 5000);
        assertNotNull(shareButton);
    }

    @Test
    public void openWithExternalBrowser() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Click setting button
        UiObject2 settingButton = device.wait(
                Until.findObject(By.res(BuildConfig.APPLICATION_ID, "setting_top_activity")), 5000);
        if (settingButton == null) fail("Setting button was not found");
        settingButton.clickAndWait(Until.newWindow(), 5000);

        // Disable internal browser
        UiObject2 settingsList = device.findObject(By.clazz(ListView.class));
        List<UiObject2> settings;
        try {
            settings = settingsList.wait(Until.findObjects(By.clazz(LinearLayout.class).depth(1)), 5000);
        } catch (StaleObjectException e) {
            settings = settingsList.findObjects(By.clazz(LinearLayout.class).depth(1));
        }
        for (UiObject2 setting : settings) {
            UiObject2 text = setting.findObject(
                    By.res("android:id/title"));
            if (text.getText().equals("内蔵ブラウザで開く")) {
                UiObject2 browserSwitch = setting.findObject(
                        By.res("android:id/switch_widget"));
                if (browserSwitch == null)
                    browserSwitch = setting.findObject(
                            By.res("android:id/switchWidget"));
                if (browserSwitch != null && browserSwitch.isChecked()) {
                    browserSwitch.click();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        device.pressBack();

        // Click first feed
        List<UiObject2> feedTitles = device.wait(Until.findObjects(
                By.res(BuildConfig.APPLICATION_ID, "feedTitle")), 5000);
        if (feedTitles == null) fail("Feed was not found");
        feedTitles.get(0).clickAndWait(Until.newWindow(), 5000);

        UiObject2 articleList = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "rv_article")), 5000);
        UiObject2 firstArticle = articleList.findObject(
                By.clazz(LinearLayout.class));
        firstArticle.click();

        // Assert share button in internal browser does not exist
        UiObject2 shareButton = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "menu_item_share")), 5000);
        assertNull(shareButton);
    }

    @Test
    public void goBackToTopWhenFinishAllOfTheArticles() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Click setting button
        UiObject2 settingButton = device.wait(
                Until.findObject(By.res(BuildConfig.APPLICATION_ID, "setting_top_activity")), 5000);
        if (settingButton == null) fail("Setting button was not found");
        settingButton.clickAndWait(Until.newWindow(), 5000);

        // Enable option to go back to top
        UiObject2 settingsList = device.findObject(By.clazz(ListView.class));
        List<UiObject2> settings = settingsList.findObjects(By.clazz(LinearLayout.class).depth(1));
        for (UiObject2 setting : settings) {
            UiObject2 text = setting.findObject(
                    By.res("android:id/title"));
            if (text.getText().equals("全ての記事を既読にした時の動作")) {
                UiObject2 summary = setting.findObject(
                        By.res("android:id/summary"));
                if (summary.getText().equals("RSS一覧に戻らない")) {
                    setting.clickAndWait(Until.newWindow(), 5000);
                    UiObject2 check = device.findObject(
                            By.res("android:id/text1").text("RSS一覧に戻る"));
                    check.click();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        device.pressBack();

        // Click first feed
        List<UiObject2> feedTitles = device.wait(Until.findObjects(
                By.res(BuildConfig.APPLICATION_ID, "feedTitle")), 5000);
        if (feedTitles == null) {
            takeScreenshot(device);
            fail("Feed was not found");
        }
        feedTitles.get(0).click();

        // Click fab
        UiObject2 fab = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "fab_article_list")), 5000);
        fab.click();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fab.click();

        // Assert top activity is foreground
        @SuppressWarnings("deprecation")
        List<UiObject2> tabs = device.wait(Until.findObjects(
                By.clazz(android.support.v7.app.ActionBar.Tab.class)), 5000);
        assertNotNull(tabs);
    }

    @Test
    public void notGoBackToTopWhenFinishAllOfTheArticles() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Click setting button
        UiObject2 settingButton = device.wait(
                Until.findObject(By.res(BuildConfig.APPLICATION_ID, "setting_top_activity")), 5000);
        if (settingButton == null) {
            takeScreenshot(device);
            fail("Setting button was not found");
        }
        settingButton.clickAndWait(Until.newWindow(), 5000);

        // Disable option to go back to top
        UiObject2 settingsList = device.findObject(By.clazz(ListView.class));
        List<UiObject2> settings = settingsList.findObjects(By.clazz(LinearLayout.class).depth(1));
        for (UiObject2 setting : settings) {
            UiObject2 text = setting.findObject(
                    By.res("android:id/title"));
            if (text.getText().equals("全ての記事を既読にした時の動作")) {
                UiObject2 summary = setting.findObject(
                        By.res("android:id/summary"));
                if (summary.getText().equals("RSS一覧に戻る")) {
                    setting.clickAndWait(Until.newWindow(), 5000);
                    UiObject2 check = device.findObject(
                            By.res("android:id/text1").text("RSS一覧に戻らない"));
                    check.click();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        device.pressBack();

        // Click first feed
        List<UiObject2> feedTitles = device.wait(Until.findObjects(
                By.res(BuildConfig.APPLICATION_ID, "feedTitle")), 5000);
        if (feedTitles == null) fail("Feed was not found");
        feedTitles.get(0).click();

        // Click fab
        UiObject2 fab = device.wait(Until.findObject(
                By.res(BuildConfig.APPLICATION_ID, "fab_article_list")), 5000);
        fab.click();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fab.click();

        // Assert article list is still foreground
        UiObject2 allReadButton = device.findObject(
                By.res(BuildConfig.APPLICATION_ID, "all_read"));
        assertNotNull(allReadButton);
    }

    @Test
    public void goLicenseActivity() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Click setting button
        UiObject2 settingButton = device.wait(
                Until.findObject(By.res(BuildConfig.APPLICATION_ID, "setting_top_activity")), 5000);
        if (settingButton == null) fail("Setting button was not found");
        settingButton.clickAndWait(Until.newWindow(), 5000);

        // Click license info
        UiObject2 settingsList = device.findObject(By.clazz(ListView.class));
        List<UiObject2> settings = settingsList.findObjects(By.clazz(LinearLayout.class).depth(1));
        for (UiObject2 setting : settings) {
            UiObject2 text = setting.findObject(
                    By.res("android:id/title"));
            if (text.getText().equals("ライセンス情報")) {
                setting.click();
                break;
            }
        }

        // Assert title
        UiObject2 title = device.wait(Until.findObject(
                By.clazz(TextView.class).text("ライセンス")), 5000);
        assertNotNull(title);
    }
}

package com.phicdy.mycuration.presentation.view.activity

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem

import com.phicdy.mycuration.R
import com.phicdy.mycuration.presentation.presenter.AddCurationPresenter
import com.phicdy.mycuration.tracker.GATrackerHelper
import com.phicdy.mycuration.presentation.view.fragment.AddCurationFragment

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

class AddCurationActivity : AppCompatActivity() {

    private var wordListFragment: AddCurationFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_curation)
        initView()
        initToolbar()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_add_curation, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.add_curation -> wordListFragment!!.onAddMenuClicked()
            // For arrow button on toolbar
            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        GATrackerHelper.sendScreen(title.toString())
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    private fun initView() {
        wordListFragment = supportFragmentManager.findFragmentById(R.id.fr_curation_condition) as AddCurationFragment
    }

    private fun initToolbar() {
        val toolbar = findViewById(R.id.toolbar_add_curation) as Toolbar
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            // Show back arrow icon
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
            val id = intent.getIntExtra(AddCurationFragment.EDIT_CURATION_ID, -1)
            if (id == -1) {
                actionBar.title = getString(R.string.title_activity_add_curation)
            } else {
                actionBar.title = getString(R.string.title_activity_edit_curation)
            }
        }
    }

}

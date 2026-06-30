package com.woshiwangnima.healthdietpro.base

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.woshiwangnima.healthdietpro.R

abstract class BaseBackActivity : BaseActivity() {

    abstract fun getTitleText(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getTitleText()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

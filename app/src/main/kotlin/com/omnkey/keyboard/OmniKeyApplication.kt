package com.omnkey.keyboard

import android.app.Application
import com.omnkey.keyboard.core.database.OmniKeyDatabase
import com.omnkey.keyboard.core.preferences.PreferenceManager

class OmniKeyApplication : Application() {

    val database: OmniKeyDatabase by lazy {
        OmniKeyDatabase.getDatabase(this)
    }

    val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OmniKeyApplication
            private set
    }
}

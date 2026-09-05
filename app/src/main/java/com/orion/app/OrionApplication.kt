package com.orion.app

import android.app.Application

class OrionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Reserved for process-wide init: crash reporting, WorkManager, etc.
    }
}

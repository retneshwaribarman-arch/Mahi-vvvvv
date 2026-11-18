package com.mahi.assistant

import android.app.Application

class MahiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        lateinit var context: android.content.Context
            private set
    }
}

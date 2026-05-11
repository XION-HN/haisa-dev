package com.haisa.des

import android.app.Application
import com.haisa.sdk.HaisaEnvironment

class HaisaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        HaisaEnvironment.getInstance(this)
    }
}

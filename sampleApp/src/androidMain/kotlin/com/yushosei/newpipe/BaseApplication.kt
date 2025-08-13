/*
 * Yushosei 
*/

package com.yushosei.newpipe

import android.app.Application
import android.content.Context
import com.yushosei.newpipe.player.playerModule
import com.yushosei.newpipe.presentation.di.presentationModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

var appContext: Context? = null

open class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        appContext = this

        startKoin {
            androidContext(this@BaseApplication)
            modules(presentationModules + playerModule)
        }
    }
}

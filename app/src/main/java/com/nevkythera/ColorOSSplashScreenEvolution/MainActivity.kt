package com.Nevkythera.ColorOSSplashScreenEvolution

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.MainScreen
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme.AppTheme
import com.Nevkythera.ColorOSSplashScreenEvolution.util.LocaleManager

class MainActivity : ComponentActivity() {

    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 登记本 Activity，供 LocaleManager 切换语言时重建。
        // 用一个静态 flag 保证回调只注册一次（Activity 可能被重建多次）。
        if (!callbacksRegistered) {
            callbacksRegistered = true
            application.registerActivityLifecycleCallbacks(ActivityCallbacks)
        }
        LocaleManager.ActivityTracker.register(this)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        LocaleManager.ActivityTracker.unregister(this)
        super.onDestroy()
    }

    private companion object {
        /** 生命周期回调只需注册一次，避免 Activity 重建时重复注册。 */
        var callbacksRegistered = false

        val ActivityCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                LocaleManager.ActivityTracker.register(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                LocaleManager.ActivityTracker.unregister(activity)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        }
    }
}

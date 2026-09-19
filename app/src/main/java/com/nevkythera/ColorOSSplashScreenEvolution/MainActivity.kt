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

    /**
     * ★ 语言覆盖必须在这里做，而且**必须早于** `super.attachBaseContext()`——
     *   否则 Resources 已经按旧语言初始化，Compose 拿到的就是旧文案。
     *
     * 为什么不能交给 AppCompatDelegate 自动处理：见 `LocaleManager` 类注释，
     * API 33+ 它只把语言写进框架 LocaleManager，等 Activity 重建时才由
     * ResourcesManager 应用；我们的 ComponentActivity + Compose 不会自己重建。
     * 所以这里既**手动覆盖**，`LocaleManager.apply()` 里也**手动重建**。
     */
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

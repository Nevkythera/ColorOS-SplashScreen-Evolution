package com.Nevkythera.ColorOSSplashScreenEvolution

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.ExitAnimationPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme.AppTheme
import com.Nevkythera.ColorOSSplashScreenEvolution.util.LocaleManager
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SystemUiRestarter


class ExitAnimationActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = ConfigStore(this)

        val restart: () -> Unit = {
            Thread {
                SystemUiRestarter.restartSystemUi(applicationContext)
            }.start()
        }

        setContent {
            AppTheme {
                var config by remember { mutableStateOf(store.read()) }

                ExitAnimationPage(
                    config = config,
                    store = store,
                    onConfigChange = { config = it },
                    onBackClick = { finish() },
                    onRestartClick = restart
                )
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val needsNewTask = (context as? Activity) == null
            context.startActivity(
                Intent(context, ExitAnimationActivity::class.java).apply {
                    if (needsNewTask) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            )
        }
    }
}

package com.Nevkythera.ColorOSSplashScreenEvolution

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.navgation.Destination
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.AnimationPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.BackgroundPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.IconPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.MiscPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme.AppTheme
import com.Nevkythera.ColorOSSplashScreenEvolution.util.LocaleManager
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SystemUiRestarter


class PanelActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 从 Intent 解析根页面；解析失败（如进程重建后参数异常）直接结束，
        // 不做兜底跳转 —— 静默 finish 比跳到一个错误页面更好排查。
        val initialDest = Destination.fromRoute(intent?.getStringExtra(EXTRA_DEST))
        if (initialDest == null) {
            finish()
            return
        }

        // 配置：与 MainActivity 共用同一份 ConfigStore，改动实时落盘。
        val store = ConfigStore(this)

        val restart: () -> Unit = {
            Thread {
                SystemUiRestarter.restartSystemUi(applicationContext)
            }.start()
        }

        setContent {
            AppTheme {
                //   普通 var 赋值不触发重组 → 开关点击后值写进 SharedPreferences
                //   但界面永不刷新，用户看到的是「所有开关按了没有反应」。
                var config by remember { mutableStateOf(store.read()) }

                // 页栈：栈底是入口页，栈顶是当前显示页。
                val pageStack = remember { mutableStateListOf(initialDest) }
                val current = pageStack.last()

                val goBack: () -> Unit = {
                    if (pageStack.size > 1) pageStack.removeAt(pageStack.lastIndex) else finish()
                }
                // 栈内还有上级才接管返回；单页时交给系统，
                // 这样返回手势能走平台预测式返回动画。
                BackHandler(enabled = pageStack.size > 1) { goBack() }

                when (current) {
                    Destination.ICON -> IconPage(
                        config = config,
                        store = store,
                        onConfigChange = { config = it },
                        masterEnabled = config.masterSwitch,
                        onBackClick = goBack,
                        onRestartClick = restart
                    )

                    Destination.BACKGROUND -> BackgroundPage(
                        config = config,
                        store = store,
                        onConfigChange = { config = it },
                        masterEnabled = config.masterSwitch,
                        onBackClick = goBack,
                        onRestartClick = restart
                    )

                    Destination.ANIMATION -> AnimationPage(
                        onBackClick = goBack,
                        onRestartClick = restart
                    )

                    Destination.MISC -> MiscPage(
                        config = config,
                        store = store,
                        onConfigChange = { config = it },
                        masterEnabled = config.masterSwitch,
                        onBackClick = goBack,
                        onRestartClick = restart
                    )
                }
            }
        }
    }

    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_DEST = "cse_extra_destination"

        
        fun start(context: Context, dest: Destination) {
            val needsNewTask = (context as? Activity) == null
            context.startActivity(
                Intent(context, PanelActivity::class.java).apply {
                    putExtra(EXTRA_DEST, dest.route)
                    if (needsNewTask) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            )
        }
    }
}

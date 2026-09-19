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
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.ExitAnimationPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.IconPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens.MiscPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme.AppTheme
import com.Nevkythera.ColorOSSplashScreenEvolution.util.LocaleManager
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SystemUiRestarter

/**
 * 二级 / 三级功能页宿主（图标 / 背景 / 动画 / 退出动画 / 杂项）。
 *
 * ────────────────────────────────────────────────────────────────────────
 * ★ 为什么这些页面是独立 Activity，而不是 Compose 的 NavHost 目标
 * ────────────────────────────────────────────────────────────────────────
 *
 * 用户要求「跨页面动画用系统原生的，直接打开一个新的 Activity」。
 * NavHost 的转场本质是「同一 Activity 内对可组合项做动画」：不参与系统
 * 「过渡动画」缩放设置，也没有平台的预测式返回等能力。改成独立 Activity 后，
 * 转场完全交给系统（遵循动画缩放 / 减少动态效果设置），返回由平台栈管理。
 *
 * ★ 为什么只用一个泛型 Activity + 参数，而不是每页一个 Activity：
 *   页面骨架同构（顶栏 + 返回 + 重启按钮），拆开会重复样板。
 *
 * ────────────────────────────────────────────────────────────────────────
 * ★★ 页栈（本版新增）：设置页 → 动画 → 退出动画
 * ────────────────────────────────────────────────────────────────────────
 *
 * `PanelActivity` 是 `launchMode="singleTop"`，若「动画」页再次 `startActivity`
 * 打开「退出动画」，系统**复用同一实例**、只回调 `onNewIntent()`（不重建），
 * 表现为「点了没反应」。因此改为**外部入口只用 Intent，页内导航走内部页栈**：
 * 用 [mutableStateListOf] 维护 `pageStack`，压栈/弹栈即原地换内容，返回键与
 * 返回箭头统一走 [goBack]。这样多级页面在同一个 Activity 窗口内、复用同一份
 * 配置状态，也不会破坏系统转场。
 *
 * ★ 语言覆盖与 [MainActivity] 完全一致：`attachBaseContext` 里 wrap，
 *   否则二级页会显示成系统语言而非用户选择。
 */
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
                // ★★ 必须用 Compose 的 State，不能是普通局部 `var`（血泪教训）：
                //   普通 var 赋值不触发重组 → 开关点击后值写进 SharedPreferences
                //   但界面永不刷新，用户看到的是「所有开关按了没有反应」。
                var config by remember { mutableStateOf(store.read()) }

                // 页栈：栈底是入口页，栈顶是当前显示页。
                val pageStack = remember { mutableStateListOf(initialDest) }
                val current = pageStack.last()

                val goBack: () -> Unit = {
                    if (pageStack.size > 1) pageStack.removeAt(pageStack.lastIndex) else finish()
                }
                // 统一接管返回：栈内还有上级就弹栈，否则走默认（结束本 Activity）。
                BackHandler { goBack() }

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
                        onRestartClick = restart,
                        onOpenChild = { pageStack.add(it) }
                    )

                    Destination.EXIT_ANIMATION -> ExitAnimationPage(
                        config = config,
                        store = store,
                        onConfigChange = { config = it },
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

    /**
     * `launchMode="singleTop"` 下重复点同一个入口时会走这里。
     *
     * ★ 必须把新 Intent 交给框架：`setIntent(intent)` 之后，若将来某个页面需要读
     *   `intent.extras`，拿到的才是最新一次的参数。当前入口页只认 [EXTRA_DEST]，
     *   且页内导航已改为内部页栈，所以这里只转发、不重建。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_DEST = "cse_extra_destination"

        /**
         * 打开某个入口页（栈底页）。
         *
         * ────────────────────────────────────────────────────────────────────
         * ★★ 为什么**不能**用 FLAG_ACTIVITY_NEW_TASK（血泪教训，别改回去）
         * ────────────────────────────────────────────────────────────────────
         * NEW_TASK 会把这层 entry 放进一个新 task，ActivityManager 于是走
         * `taskOpen*` / `taskClose*` 那组动画（Android 15 上就是纯淡入淡出），
         * 而不是同 task 内的 `activityOpen*` / `activityClose*`（带水平位移的
         * 「推入」动画）。所以这里**不传任何 flag**，直接落在调用方所在的 task。
         *
         * ★ 前提：调用方 Context 必须是 Activity（Compose 里 `LocalContext.current`
         *   解析到的就是 MainActivity），否则才退回 NEW_TASK（防御，正常不会走到）。
         *
         * ★ 防同页叠栈改用 Manifest 的 `launchMode="singleTop"`（比 CLEAR_TOP 精确，
         *   后者会把它上面的页面全弹掉）。
         */
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

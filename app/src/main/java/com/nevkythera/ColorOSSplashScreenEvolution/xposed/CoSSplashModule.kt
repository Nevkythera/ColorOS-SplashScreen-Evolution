package com.Nevkythera.ColorOSSplashScreenEvolution.xposed

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.palette.graphics.Palette
import androidx.compose.ui.graphics.toArgb
import com.Nevkythera.ColorOSSplashScreenEvolution.util.GraphicUtils
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method


class CoSSplashModule : XposedModule() {

    private companion object {
        const val TAG = "CSE"

        /** system_server 的进程名固定为 "android" */
        const val PROCESS_SYSTEM = "android"

        /** 本模块唯一的 Hook 目标进程：SystemUI */
        const val PROCESS_SYSTEMUI = "com.android.systemui"

        // ---- StartingWindow 类型常量（android.window.StartingWindowInfo） ----
        const val TYPE_SPLASH_SCREEN = 1          // 原生 SplashScreen（大图标 + ripple）
        const val TYPE_SNAPSHOT = 2               // 截图（snapshot）
        const val TYPE_SPLASH_SCREEN_SOLID = 3    // 原生 SplashScreen（纯色，可能带 XML icon）
        const val TYPE_LEGACY_SPLASH_SCREEN = 4   // legacy SC（ColorOS XML 预览图）
        const val TYPE_WINDOWLESS = 5             // 无窗口

        // ---- 退出动画类型（SplashScreenExitAnimationUtils） ----
        const val EXIT_ANIM_RIPPLE = 0            // TYPE_RADIAL_VANISH_SLIDE_UP
        const val EXIT_ANIM_FADE_OUT = 1          // TYPE_FADE_OUT

        // ---- 类名 ----
        const val CLS_PHONE_TYPE_ALGORITHM =
            "com.android.wm.shell.startingsurface.phone.PhoneStartingWindowTypeAlgorithm"
        const val CLS_OPLUS_MANAGER =
            "com.android.wm.shell.startingsurface.OplusShellStartingWindowManager"

        const val CLS_CONTENT_DRAWER =
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer"

        const val CLS_SPLASH_VIEW_BUILDER =
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$SplashViewBuilder"

        /**
         * Android 15 起 AOSP 把 SplashViewBuilder 改名为 StartingWindowViewBuilder
         * （参考项目 RestoreSplashScreen 也是二选一 fallback）。
         * 若这里只认旧名，Android 15+ 上 C 层（定型层）与"缩小图标"会一起静默失效。
         */
        const val CLS_STARTING_WINDOW_VIEW_BUILDER =
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$StartingWindowViewBuilder"

        
        const val CLS_EXIT_ANIM_UTILS =
            "com.android.wm.shell.shared.startingsurface.SplashScreenExitAnimationUtils"

        const val CLS_EXIT_ANIM =
            "com.android.wm.shell.startingsurface.SplashScreenExitAnimation"

        /** 图标 Hook 用：Launcher 图标提供器（替换图标获取方式）。 */
        const val CLS_ICON_PROVIDER = "com.android.launcher3.icons.IconProvider"

        /** 图标 Hook 用：ColorOS / AOSP 高分辨率图标提供器（getIcon 的真正入口）。 */
        const val CLS_HIGH_RES_ICON_PROVIDER =
            "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$HighResIconProvider"

        /** 图标/背景 Hook 用：SplashScreenView.Builder（build / createIconDrawable / 圆角）。 */
        const val CLS_SPLASH_VIEW_BUILDER_FRAMEWORK = "android.window.SplashScreenView\$Builder"

        /** 真正的 SplashScreenView（装饰只允许作用在它身上）。 */
        const val CLS_SPLASH_SCREEN_VIEW = "android.window.SplashScreenView"

        /** 背景 Hook 用：从缓存取背景色（缓存 mTmpAttrs）。 */
        const val CLS_GET_BG_COLOR_FROM_CACHE = "getBGColorFromCache"

        /** 强制不缩小 Hook 用：阻止 SystemUI 对非自适应图标二次缩放。 */
        const val CLS_BASE_ICON_FACTORY = "com.android.launcher3.icons.BaseIconFactory"

        // ---- 功能开关 ----
        // 这些是"远程偏好读取失败时"的兜底默认值。
        // 正常情况下 readRemoteConfig() 会在 onModuleLoaded / onPackageReady 阶段
        // 从 LSPosed 远程偏好（cse_prefs）读取用户实际设置并覆盖它们。
        //
        // 本次大改：
        //  - FORCE_NATIVE 升级为「总开关」，所有功能 Hook 前必须先验证它；
        //  - ripple 不再独立，跟随总开关（E 层用 FORCE_NATIVE 控制）；
        //  - 新增图标（绘制圆角 / 缩小 / 替换获取方式）与背景（替换颜色）配置。
        @Volatile
        var FORCE_NATIVE = true

        @Volatile
        var DISABLE_PREVIEW = true

        @Volatile
        var DRAW_ROUND_CORNER = false

        @Volatile
        var SHRINK_ICON = 0   // 0=不缩小 / 2=全部（值 1 已废弃，见 SHRINK_* 常量注释）

        @Volatile
        var REPLACE_ICON = false

        @Volatile
        var CHANGE_BG_COLOR_TYPE = 0   // 0=不替换 / 1=从图标取色 / 2=莫奈取色 / 3=自定义

        @Volatile
        var ENABLE_ICON_BLUR_BG = false  // 图标背景模糊（尺寸由 BLUR_BG_SCALE 控制）

        /** 模糊背景相对图标的放大倍率（百分比，50~300）。 */
        @Volatile
        var BLUR_BG_SCALE = 100

        /** 单个粒子自身的运动时长（毫秒）。 */
        @Volatile
        var PARTICLE_TIME_MS = 600

        /**
         * MD3 Expressive 几何形变加载动画（默认关闭）。
         *
         * 在 SplashScreen 图标背后放一个持续形变 + 旋转的几何色块
         * （圆 → 四角星 → 圆 → 花瓣 → 圆，见 [MorphShapeView]）。
         * 开启后**跳过静态模糊背景**：两者都贴在图标背后，叠在一起很脏。
         */
        @Volatile
        var ENABLE_MORPH_SHAPE = false

        /**
         * 指示器大小：相对"缩小后的图标"的倍率百分比（50% ~ 300%，默认 100%）。
         * 100% 表示色块直径 = 图标的 2 倍（与录屏实测比例一致）。
         */
        @Volatile
        var MORPH_SHAPE_SCALE = 100

        /**
         * 指示器取色方式：0 = 莫奈取色（primary），1 = 跟随应用图标取色（Palette）。
         */
        @Volatile
        var MORPH_SHAPE_COLOR_TYPE = 0

        /**
         * 移除图标：强制隐藏 SplashScreen 上的所有图标（应用图标 + 品牌图）。
         * 与"关闭截图覆盖"（DISABLE_PREVIEW）互相独立，可同时开启。
         */
        @Volatile
        var REMOVE_ICON = false

        
        @Volatile
        var ENABLE_HOT_START_SPLASH = false

        /**
         * 退出动画效果：0 = 默认（保持 Android 原生 ripple）/ 1 = 粒子消失。
         * 粒子模式会拦截 `SplashScreenExitAnimationUtils#startAnimations`，
         * 替换为 [SplashParticleView] 的整屏粒子消散。
         */
        @Volatile
        var EXIT_ANIM_MODE = 0

        /** 粒子消失动画总时长（毫秒）。 */
        @Volatile
        var EXIT_PARTICLE_DURATION_MS = 600

        @Volatile
        var BG_COLOR_MODE = 2          // 0=浅色 / 1=暗色 / 2=跟随系统

        @Volatile
        var CUSTOM_BG_COLOR = "#FFFFFF"

        @Volatile
        var CUSTOM_BG_COLOR_NIGHT = "#000000"

        /** 远程偏好仓库名，需与 App 侧 XposedRepo.REMOTE_PREFS_NAME 保持一致。 */
        const val REMOTE_PREFS_NAME = "cse_prefs"

        const val KEY_FORCE_NATIVE = "force_native"
        const val KEY_DISABLE_PREVIEW = "disable_preview"
        const val KEY_DRAW_ROUND_CORNER = "draw_round_corner"
        const val KEY_SHRINK_ICON = "shrink_icon"
        const val KEY_REPLACE_ICON = "replace_icon"
        const val KEY_CHANGE_BG_COLOR_TYPE = "change_bg_color_type"
        const val KEY_BG_COLOR_MODE = "bg_color_mode"
        const val KEY_CUSTOM_BG_COLOR = "custom_bg_color"
        const val KEY_CUSTOM_BG_COLOR_NIGHT = "custom_bg_color_night"
        const val KEY_ENABLE_ICON_BLUR_BG = "enable_icon_blur_bg"
        const val KEY_BLUR_BG_SCALE = "blur_bg_scale"
        const val KEY_PARTICLE_TIME_MS = "exit_particle_time_ms"
        const val KEY_ENABLE_MORPH_SHAPE = "enable_morph_shape"
        const val KEY_MORPH_SHAPE_SCALE = "morph_shape_scale"
        const val KEY_MORPH_SHAPE_COLOR_TYPE = "morph_shape_color_type"
        const val KEY_REMOVE_ICON = "remove_icon"

        /** 装饰层标识：模糊背景 / MD3E 几何形变。 */
        const val TAG_BLUR_BG = "cse_blur_bg"
        const val TAG_MORPH_SHAPE = "cse_morph_shape"

        /** 指示器取色方式。 */
        const val MORPH_COLOR_FROM_MONET = 0
        const val MORPH_COLOR_FROM_ICON = 1

        /** 指示器色块的不透明度（略透明，避免完全压住背景遮罩颜色）。 */
        const val TINT_ALPHA = 0xE6

        /** 图标取色用的单线程池（Palette 是同步计算，不能跑主线程）。 */
        val ICON_COLOR_EXECUTOR: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newSingleThreadExecutor()
        const val KEY_ENABLE_HOT_START_SPLASH = "enable_hot_start_splash"
        const val KEY_EXIT_ANIM_MODE = "exit_anim_mode"
        const val KEY_EXIT_PARTICLE_DURATION_MS = "exit_particle_duration_ms"

        
        const val AR_STARTING_WINDOW_TYPE_NONE = 0
        const val AR_STARTING_WINDOW_TYPE_SNAPSHOT = 1
        const val AR_STARTING_WINDOW_TYPE_SPLASH_SCREEN = 2

        /** system_server 里承载热启动判断的类。 */
        const val CLS_ACTIVITY_RECORD = "com.android.server.wm.ActivityRecord"

        // ---- 缩小图标类型 ----
        //   因实用性极低（绝大多数应用的图标本身就是自适应图标，判定条件
        //   `intrinsicWidth < iconSize / 1.5` 几乎永远不成立）已被移除，
        //   相关 UI 选项、字符串资源、判定分支全部删除。
        //   ⚠️ 值 1 现在成为**历史遗留的无效值**：若老用户配置里存着 1，
        //      读取时会被 `coerceSHRINK` 归一化为 0（不缩小），见 loadRemoteConfig。
        const val SHRINK_NONE = 0
        const val SHRINK_ALL = 2

        // ---- 退出动画模式 ----
        const val EXIT_MODE_DEFAULT = 0
        const val EXIT_MODE_DIFFUSE = 1
        const val EXIT_MODE_DISSOLVE = 2

        /** 粒子动画时长范围与默认值（毫秒），需与 App 侧 CseConfig 保持一致。 */
        const val EXIT_DURATION_MIN = 200
        const val EXIT_DURATION_MAX = 2000
        const val EXIT_DURATION_DEFAULT = 600

        /**
         * 粒子采样前的屏幕内容降采样倍数（1/CAPTURE_SCALE）。
         * 取 1（原分辨率）：铺底原图不缩放 → 未被锋面扫到的区域保持清晰；
         * 粒子密度则靠 SplashParticleView 的采样步长控制。
         */
        const val CAPTURE_SCALE = 1

        // ---- 替换背景颜色类型 ----
        const val BG_TYPE_NONE = 0
        const val BG_TYPE_FROM_ICON = 1
        const val BG_TYPE_FROM_MONET = 2
        const val BG_TYPE_FROM_CUSTOM = 3

        // ---- 背景颜色模式 ----
        const val BG_MODE_LIGHT = 0
        const val BG_MODE_DARK = 1
        const val BG_MODE_FOLLOW_SYSTEM = 2
    }

    private val routedPackages = mutableSetOf<String>()

    // ---- 图标/背景 Hook 的运行时状态（随每个 SplashScreen 生命周期刷新） ----
    @Volatile
    private var currentPackageName: String = ""

    /** 当前应用的图标主色（供背景"从图标取色"使用）。 */
    @Volatile
    private var currentIconDominantColor: Int? = null

    /** 当前应用的图标 Drawable（供模糊背景 / 从图标取色 fallback 使用）。 */
    @Volatile
    private var currentIconDrawable: Drawable? = null

    
    @Volatile
    private var currentIconBitmap: android.graphics.Bitmap? = null

    /** 当前应用图标的**最终**主色（优先由 [currentIconBitmap] 采样得到）。 */
    @Volatile
    private var currentFinalIconAccentColor: Int? = null

    /** 当前图标是否需要缩小（供 createIconDrawable 改 mFinalIconSize）。 */
    @Volatile
    private var currentIsNeedShrinkIcon: Boolean = false

    /** 本次生命周期是否已执行过缩小（防止 createIconDrawable 被多次调用导致重复缩小）。 */
    @Volatile
    private var iconShrinkApplied: Boolean = false

    
    @Volatile
    private var iconReplaced: Boolean = false

    /** 本次生命周期是否已计算过「是否需要缩小」（getIconExt 命中时由它算，否则由 createIconDrawable 补算）。 */
    @Volatile
    private var iconStateDecided: Boolean = false

    /**
     * 本次生命周期是否已经在某个 build() 出口做过"圆角 / 模糊"装饰。
     *
     * 同一张 SplashScreenView 会先后经过两个 build()：
     *   1) com.android.wm.shell.startingsurface.SplashscreenContentDrawer$SplashViewBuilder#build()
     *   2) android.window.SplashScreenView$Builder#build()
     * 两者都 hook 了（双保险），这个标记保证只装饰一次，避免叠两层模糊。
     */
    @Volatile
    private var iconDecorApplied: Boolean = false

    @Volatile
    private var mTmpAttrsInstance: Any? = null

    /** 图标圆角程度（百分比，0~100）。沿用 RestoreSplashScreen 的 dev 默认值 25。 */
    private val iconRoundCornerRate: Int = 25

    /** 图标处理去重标记：阻止 getIconExt 4 参 → 6 参的级联重复处理。 */
    private val iconProcessing = ThreadLocal<Boolean>()

    // ================================================================ 生命周期

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        log(
            Log.INFO, TAG,
            "event=module_loaded result=ok process=${param.processName} " +
                "api=${getApiVersion()} framework=${getFrameworkName()} version=${getFrameworkVersion()}"
        )
        // 从远程偏好拉取用户配置。
        //
        // 旧版这里是"往模块 App 私有目录写一个 JSON 激活信号"，现已废弃：
        // 设置界面改为通过 XposedServiceHelper 绑定框架服务来判定激活状态，
        // 不再需要 Hook 侧落盘任何信号文件。
        readRemoteConfig()
    }

    
    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        log(
            Log.INFO, TAG,
            "event=module_loaded result=ok process=system_server " +
                "api=${getApiVersion()} framework=${getFrameworkName()} version=${getFrameworkVersion()}"
        )
        readRemoteConfig()
        hotStartInstalled = true
        installStage("J_hot_start") { installHotStartSplashHook(param.classLoader) }
    }

    /** 热启动 Hook 是否已安装（避免 onPackageReady 的兜底分支重复安装）。 */
    @Volatile
    private var hotStartInstalled: Boolean = false

    /**
     * 读取 LSPosed 远程偏好，覆盖功能开关。
     *
     * 远程偏好由 App 侧通过 `XposedService.getRemotePreferences("cse_prefs")` 写入，
     * 这里读到的是用户实时调整后的值，**无需重启 SystemUI** 即可生效。
     * 任一环节失败都静默回落到编译期默认值，保证 Hook 不会因为读配置而崩。
     */
    private fun readRemoteConfig() {
        refreshConfigSilently(logResult = true)
    }

    /**
     * 实时刷新配置（不打印日志，供每次 intercept 高频调用）。
     *
     * getRemotePreferences 返回的是框架缓存的 SharedPreferences 实例，
     * 其 getXxx() 每次都会读到底层最新数据，因此这里每次调用都能拿到
     * 用户刚在设置界面里改的值 —— 这就是"实时生效"的关键。
     */
    private fun refreshConfigSilently(logResult: Boolean = false) {
        try {
            val prefs = getRemotePreferences(REMOTE_PREFS_NAME) ?: return
            FORCE_NATIVE = prefs.getBoolean(KEY_FORCE_NATIVE, true)
            DISABLE_PREVIEW = prefs.getBoolean(KEY_DISABLE_PREVIEW, true)
            DRAW_ROUND_CORNER = prefs.getBoolean(KEY_DRAW_ROUND_CORNER, false)
            //   老用户配置里若残留 1，映射回 0（不缩小），否则 when 分支会走到
            //   else -> false 虽然也是"不缩小"，但日志/远程配置里会出现无效值，
            //   显式归一让状态干净可追踪。
            val rawShrink = prefs.getInt(KEY_SHRINK_ICON, SHRINK_NONE)
            SHRINK_ICON = if (rawShrink == SHRINK_ALL) SHRINK_ALL else SHRINK_NONE
            REPLACE_ICON = prefs.getBoolean(KEY_REPLACE_ICON, false)
            CHANGE_BG_COLOR_TYPE = prefs.getInt(KEY_CHANGE_BG_COLOR_TYPE, 0)
            BG_COLOR_MODE = prefs.getInt(KEY_BG_COLOR_MODE, 2)
            CUSTOM_BG_COLOR = prefs.getString(KEY_CUSTOM_BG_COLOR, "#FFFFFF") ?: "#FFFFFF"
            CUSTOM_BG_COLOR_NIGHT =
                prefs.getString(KEY_CUSTOM_BG_COLOR_NIGHT, "#000000") ?: "#000000"
            ENABLE_ICON_BLUR_BG = prefs.getBoolean(KEY_ENABLE_ICON_BLUR_BG, false)
            BLUR_BG_SCALE = prefs.getInt(KEY_BLUR_BG_SCALE, 100).coerceIn(50, 300)
            PARTICLE_TIME_MS = prefs.getInt(KEY_PARTICLE_TIME_MS, 600).coerceIn(100, 2000)
            ENABLE_MORPH_SHAPE = prefs.getBoolean(KEY_ENABLE_MORPH_SHAPE, false)
            MORPH_SHAPE_SCALE = prefs.getInt(KEY_MORPH_SHAPE_SCALE, 100).coerceIn(50, 300)
            MORPH_SHAPE_COLOR_TYPE = prefs.getInt(KEY_MORPH_SHAPE_COLOR_TYPE, 0)
            REMOVE_ICON = prefs.getBoolean(KEY_REMOVE_ICON, false)
            ENABLE_HOT_START_SPLASH = prefs.getBoolean(KEY_ENABLE_HOT_START_SPLASH, false)
            EXIT_ANIM_MODE = when (prefs.getInt(KEY_EXIT_ANIM_MODE, EXIT_MODE_DEFAULT)) {
                EXIT_MODE_DIFFUSE, EXIT_MODE_DISSOLVE ->
                    prefs.getInt(KEY_EXIT_ANIM_MODE, EXIT_MODE_DEFAULT)
                else -> EXIT_MODE_DEFAULT
            }
            EXIT_PARTICLE_DURATION_MS = prefs
                .getInt(KEY_EXIT_PARTICLE_DURATION_MS, EXIT_DURATION_DEFAULT)
                .coerceIn(EXIT_DURATION_MIN, EXIT_DURATION_MAX)
            if (logResult) {
                log(
                    Log.INFO, TAG,
                    "event=remote_config result=ok force_native=$FORCE_NATIVE " +
                        "disable_preview=$DISABLE_PREVIEW round_corner=$DRAW_ROUND_CORNER " +
                        "shrink_icon=$SHRINK_ICON replace_icon=$REPLACE_ICON " +
                        "remove_icon=$REMOVE_ICON " +
                        "morph=$ENABLE_MORPH_SHAPE morph_scale=$MORPH_SHAPE_SCALE " +
                        "morph_color=$MORPH_SHAPE_COLOR_TYPE " +
                        "bg_type=$CHANGE_BG_COLOR_TYPE bg_mode=$BG_COLOR_MODE " +
                        "blur_bg=$ENABLE_ICON_BLUR_BG hot_start=$ENABLE_HOT_START_SPLASH " +
                        "exit_anim=$EXIT_ANIM_MODE exit_duration=$EXIT_PARTICLE_DURATION_MS"
                )
            }
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "event=remote_config result=fail reason=exception", t)
        }
    }

    /**
     * system_server 侧只装**热启动**这一条 Hook。
     *
     * 历史：早期版本在此安装 A/B/C/D/E 全套 Hook，结果全部 ClassNotFoundException ——
     * system_server 的类路径里只有 services.jar / oplus-services.jar，而
     * `com.android.wm.shell.*` 自 Android 12 起已迁到独立的 com.android.systemui 进程。
     *
     * 但 `com.android.server.wm.ActivityRecord`（热启动判定）**确实在 services.jar**，
     * 且只有 system_server 的类加载器能加载它 —— 所以这里正好是它的唯一正确入口。
     * 其余 Hook 仍全部装在 SystemUI 进程（见 onPackageReady）。
     */

    /**
     * SystemUI 进程：A/B/C/D/E 五层主体 Hook 全部装在这里。
     *
     * 注意：SystemUI 会被 lspd 以「包」的形式多次回调（每个包一次），
     * 因此必须用 routedPackages 去重，避免同一进程内重复安装。
     * 但去重键要带上进程名 —— 系统里可能存在 com.android.systemui:xxx 子进程，
     * 它们类路径不同，漏装会导致某些场景失效。
     */
    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        val pkg = param.packageName

        //
        // 去找 ActivityRecord，刷一大堆 ClassNotFoundException。
        // 现在改为：先**真的试着加载一次**该类，加载不到就静默跳过。
        if (pkg == PROCESS_SYSTEM && !hotStartInstalled) {
            if (runCatching { param.classLoader.loadClass(CLS_ACTIVITY_RECORD) }.isSuccess) {
                if (!routedPackages.add("$pkg#system")) {
                    log(Log.INFO, TAG, "event=install_skipped result=skip reason=already_installed package=$pkg")
                    return
                }
                val sysCl = param.classLoader
                readRemoteConfig()
                log(
                    Log.INFO, TAG,
                    "event=route_match result=ok route=system_server package=$pkg hooks=J(hot_start) via=fallback"
                )
                hotStartInstalled = true
                installStage("J_hot_start") { installHotStartSplashHook(sysCl) }
            } else {
                log(
                    Log.INFO, TAG,
                    "event=route_skip result=skip reason=not_system_server package=$pkg id=android_hot_start"
                )
            }
            return
        }

        if (pkg != PROCESS_SYSTEMUI) {
            log(Log.INFO, TAG, "event=route_skip result=skip reason=package_mismatch package=$pkg")
            return
        }
        if (!routedPackages.add(pkg)) {
            log(Log.INFO, TAG, "event=install_skipped result=skip reason=already_installed package=$pkg")
            return
        }

        val cl = param.classLoader

        // 每个新进程都重新读一次远程配置，保证用户改完开关后
        // 新拉起的 SystemUI 进程立刻使用新值。
        readRemoteConfig()

        log(
            Log.INFO, TAG,
            "event=route_match result=ok route=systemui package=$pkg " +
                "hooks=A(force_enable_splash_screen)+B+C+D+E+F+G(icon)+H(bg)+I(build) high_risk=true"
        )

        installStage("A_force_suggest_type") { installForceSuggestTypeHooks(cl) }
        installStage("B_reset_window_attrs") { installResetWindowAttrsHook(cl) }
        installStage("C_clean_builder_fields") { installCleanBuilderFieldsHook(cl) }
        installStage("D_block_content_background") { installBlockContentBackgroundHook(cl) }
        installStage("E_restore_ripple") { installRestoreRippleHooks(cl) }
        installStage("E2_exit_particle") { installExitParticleHook(cl) }
        installStage("F_legacy_type_rewrite") { installLegacyTypeRewriteHooks(cl) }
        installStage("G_icon_features") { installIconHooks(cl) }
        installStage("H_background_features") { installBackgroundHooks(cl) }
        installStage("I_splash_view_build") { installSplashViewBuildHook(cl) }
        // I2：wm.shell 侧 build() 出口，负责圆角 + 模糊
        installStage("I2_splash_view_decor") { installSplashViewDecorHook(cl) }
        //     与 build() 是否被 ROM 绕过、返回的 view 是不是最终那一个都无关。
        installStage("I3_splash_view_attached") { installSplashViewAttachedHook(cl) }
    }

    // ================================================================ 热重载
    //
    // 热重载（onHotReloading / onHotReloaded）已随 libxposed API 102 的
    // hotReloadModule 功能一并移除——实测无效。设置改动后通过
    // 「重启系统界面」使新配置生效（readRemoteConfig 在每个新进程里都会重新拉取）。

    // ====== A. 决策层：强制开启启动遮罩（对齐 RestoreSplashScreen 的
    //        「显示 → 强制开启启动遮罩 / FORCE_ENABLE_SPLASH_SCREEN」）

    
    private fun installForceSuggestTypeHooks(cl: ClassLoader) {
        //   是否"强制开启启动遮罩"在每次 intercept 里实时读取。
        try {
            val cls = cl.loadClass(CLS_CONTENT_DRAWER)
            val methods = cls.declaredMethods.filter { m ->
                m.name == "makeSplashScreenContentView" &&
                    m.parameterTypes.any { it == Int::class.javaPrimitiveType }
            }
            if (methods.isEmpty()) {
                throw NoSuchMethodException(
                    "$CLS_CONTENT_DRAWER#makeSplashScreenContentView(... int ...)"
                )
            }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("drawer_make_content_view")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()

                        val args = chain.args.toTypedArray()

                        // 新的一次启动：先清掉上一轮的图标/背景状态，再解析包名
                        resetSplashState()
                        extractCurrentPackageName(args)

                        // 只有总开关打开才强制改写窗口类型
                        if (FORCE_NATIVE) {
                            val idx = args.indexOfFirst { it is Int }
                            if (idx >= 0) {
                                val original = args[idx] as Int
                                args[idx] = TYPE_SPLASH_SCREEN
                                if (original != TYPE_SPLASH_SCREEN) {
                                    log(
                                        Log.INFO, TAG,
                                        "event=hook_hit id=drawer_make_content_view " +
                                            "original=$original argIndex=$idx " +
                                            "decision=force_enable_splash_screen -> $TYPE_SPLASH_SCREEN"
                                    )
                                }
                                return@intercept chain.proceed(args)
                            }
                        }
                        chain.proceed()
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=drawer_make_content_view " +
                    "target=SplashscreenContentDrawer#makeSplashScreenContentView " +
                    "strategy=first_int_arg count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("drawer_make_content_view", t)
        }
    }

    // ================================ B. 属性层：getWindowAttrsIfPresent = false

    
    private fun installResetWindowAttrsHook(cl: ClassLoader) {
        try {
            val cls = cl.loadClass(CLS_OPLUS_MANAGER)
            val methods = cls.declaredMethods.filter {
                it.name == "getWindowAttrsIfPresent" && it.returnType == Boolean::class.javaPrimitiveType
            }
            if (methods.isEmpty()) throw NoSuchMethodException("$CLS_OPLUS_MANAGER#getWindowAttrsIfPresent")

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("oplus_window_attrs_if_present")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val raw = chain.proceed() as? Boolean ?: false
                        if (FORCE_NATIVE && raw) {
                            log(
                                Log.INFO, TAG,
                                "event=hook_hit id=oplus_window_attrs_if_present original=true " +
                                    "decision=force_reparse_native_attrs -> false"
                            )
                            false
                        } else {
                            raw
                        }
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=oplus_window_attrs_if_present " +
                    "target=OplusShellStartingWindowManager#getWindowAttrsIfPresent count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("oplus_window_attrs_if_present", t)
        }
    }

    // ============================= C. 定型层：build() 前的字段三重清理

    
    private fun installCleanBuilderFieldsHook(cl: ClassLoader) {
        try {
            val cls = loadSplashViewBuilderClass(cl)
            val methods = cls.declaredMethods.filter { it.name == "build" && it.parameterCount == 0 }
            if (methods.isEmpty()) throw NoSuchMethodException("${cls.name}#build()")

            val fSuggest = cls.declaredFields.firstOrNull { it.name == "mSuggestType" }
                ?.apply { isAccessible = true }
            val fOverlay = cls.declaredFields.firstOrNull { it.name == "mOverlayDrawable" }
                ?.apply { isAccessible = true }
            val fContext = cls.declaredFields.firstOrNull { it.name == "mContext" }
                ?.apply { isAccessible = true }
            //   （真实 smali 第 1423 行 setWindowBGColor 就是写这个字段，
            //     fillViewWithIcon 第 289 行读它 → setBackgroundColor）
            val fThemeColor = cls.declaredFields.firstOrNull { it.name == "mThemeColor" }
                ?.apply { isAccessible = true }
            //   返回 true，从而 getIconExt 走 getIconResource（应用自身 icon）而非
            //   loadIcon（默认 icon），实现"已适配 splashscreen API 但用自定义图片
            //   的应用也替换成应用图标"。
            val fForceBigIcon = cls.declaredFields.firstOrNull { it.name == "mForceBigIcon" }
                ?.apply { isAccessible = true }
            //   让 needKeepStyleWithLauncherIcon 满足 `!preview && forceBigIcon` 条件。
            val fSupportPreview = cls.declaredFields.firstOrNull { it.name == "mIsSupportSplashScreenPreview" }
                ?.apply { isAccessible = true }

            if (fSuggest == null) {
                log(Log.WARN, TAG, "event=install_hook result=partial code=CSE-FLD-001 id=builder_build reason=mSuggestType_not_found")
            }
            if (fOverlay == null) {
                log(Log.WARN, TAG, "event=install_hook result=partial code=CSE-FLD-001 id=builder_build reason=mOverlayDrawable_not_found")
            }
            if (fContext == null) {
                log(Log.WARN, TAG, "event=install_hook result=partial code=CSE-FLD-001 id=builder_build reason=mContext_not_found")
            }
            if (fThemeColor == null) {
                log(Log.WARN, TAG, "event=install_hook result=partial code=CSE-FLD-001 id=builder_build reason=mThemeColor_not_found")
            }
            if (fForceBigIcon == null) {
                log(Log.WARN, TAG, "event=install_hook result=partial code=CSE-FLD-001 id=builder_build reason=mForceBigIcon_not_found")
            }
            if (fSupportPreview == null) {
                log(Log.WARN, TAG, "event=install_hook result=partial code=CSE-FLD-001 id=builder_build reason=mIsSupportSplashScreenPreview_not_found")
            }

            // 抽屉类（this$0 的声明类）上的 getWindowAttrs(Context, attrs) 方法，供重新解析用。
            // 优先静态方法（AOSP 里是 SplashscreenContentDrawer#getWindowAttrs 静态方法），
            // 找不到再回退到实例方法（部分 ROM 实现为实例方法）。
            val drawerGetWindowAttrs: Pair<Method?, Boolean>? =
                cls.declaredFields.firstOrNull { it.name == "this\$0" }?.let { f ->
                    val drawerCls = f.type
                    val statics = drawerCls.declaredMethods.firstOrNull { m ->
                        m.name == "getWindowAttrs" && m.parameterCount == 2 &&
                            java.lang.reflect.Modifier.isStatic(m.modifiers)
                    }?.apply { isAccessible = true }
                    if (statics != null) {
                        statics to true
                    } else {
                        val instanceM = drawerCls.declaredMethods.firstOrNull { m ->
                            m.name == "getWindowAttrs" && m.parameterCount == 2
                        }?.apply { isAccessible = true }
                        if (instanceM != null) instanceM to false else null
                    }
                }
            val drawerGetWindowAttrsMethod = drawerGetWindowAttrs?.first
            val drawerGetWindowAttrsStatic = drawerGetWindowAttrs?.second ?: false

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("builder_build")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val self = chain.thisObject

                        // 总开关关闭 → 不做任何"强制原生"动作，直接放行
                        if (!FORCE_NATIVE) {
                            return@intercept chain.proceed()
                        }

                        // --- mSuggestType -> 1 ---
                        if (fSuggest != null) {
                            runCatching {
                                val cur = fSuggest.get(self)
                                if (cur != TYPE_SPLASH_SCREEN) {
                                    log(Log.INFO, TAG, "event=hook_hit id=builder_build field=mSuggestType original=$cur decision=force_native -> $TYPE_SPLASH_SCREEN")
                                    fSuggest.set(self, TYPE_SPLASH_SCREEN)
                                }
                            }.onFailure { log(Log.WARN, TAG, "event=hook_error id=builder_build field=mSuggestType", it) }
                        }

                        // --- mOverlayDrawable -> null ---
                        if (fOverlay != null) {
                            runCatching {
                                val cur = fOverlay.get(self)
                                if (cur != null) {
                                    log(Log.INFO, TAG, "event=hook_hit id=builder_build field=mOverlayDrawable original=${cur.javaClass.simpleName} decision=clear -> null")
                                    fOverlay.set(self, null)
                                }
                            }.onFailure { log(Log.WARN, TAG, "event=hook_error id=builder_build field=mOverlayDrawable", it) }
                        }

                        // --- 截图覆盖：mForceBigIcon -> true（让自定义图标应用也走原生大图标） ---
                        if (fForceBigIcon != null) {
                            runCatching {
                                val cur = fForceBigIcon.get(self)
                                if (cur != true) {
                                    fForceBigIcon.set(self, true)
                                    log(Log.INFO, TAG, "event=hook_hit id=builder_build field=mForceBigIcon original=$cur decision=force_big_icon -> true")
                                }
                            }.onFailure { log(Log.WARN, TAG, "event=hook_error id=builder_build field=mForceBigIcon", it) }
                        }

                        // --- 截图覆盖：mIsSupportSplashScreenPreview -> false ---
                        //     （配合 mForceBigIcon=true，触发 needKeepStyleWithLauncherIcon）
                        if (fSupportPreview != null) {
                            runCatching {
                                val cur = fSupportPreview.get(self)
                                if (cur != false) {
                                    fSupportPreview.set(self, false)
                                    log(Log.INFO, TAG, "event=hook_hit id=builder_build field=mIsSupportSplashScreenPreview original=$cur decision=force_no_preview -> false")
                                }
                            }.onFailure { log(Log.WARN, TAG, "event=hook_error id=builder_build field=mIsSupportSplashScreenPreview", it) }
                        }

                        // --- 背景色替换：直接写 mThemeColor（兜底，覆盖所有场景） ---
                        //   fillViewWithIcon() 在 build() 末尾读 mThemeColor → setBackgroundColor，
                        //   而 mThemeColor 是由 makeSplashScreenContentView 里的
                        //   setWindowBGColor(v7) 提前写好的。我们在这里直接覆盖它，
                        //   就能保证无论 getThemeBackgroundColor 是否覆盖过 v7，
                        //   最终背景色都是用户要的颜色。
                        if (fThemeColor != null && CHANGE_BG_COLOR_TYPE != BG_TYPE_NONE) {
                            runCatching {
                                val ctx = fContext?.get(self) as? android.content.Context
                                getBackgroundColor(cl, ctx ?: hostAppContext())?.let { color ->
                                    val old = fThemeColor.get(self)
                                    if (old != color) {
                                        fThemeColor.set(self, color)
                                        log(
                                            Log.INFO, TAG,
                                            "event=hook_hit id=builder_build field=mThemeColor " +
                                                "original=$old decision=replace_bg -> $color"
                                        )
                                    }
                                }
                            }.onFailure {
                                log(Log.WARN, TAG, "event=hook_error id=builder_build field=mThemeColor", it)
                            }
                        }

                        // --- 重新解析 mTmpAttrs：修复"背景透明" ---
                        //   再清空 mSplashScreenIcon，逼出原生大图标路径。
                        var drawer: Any? = null
                        var attrs: Any? = null
                        runCatching {
                            drawer = cls.getDeclaredField("this$0")
                                .apply { isAccessible = true }
                                .get(self)
                            if (drawer != null) {
                                attrs = drawer.javaClass.getDeclaredField("mTmpAttrs")
                                    .apply { isAccessible = true }
                                    .get(drawer)
                            }
                        }.onFailure {
                            log(Log.WARN, TAG, "event=hook_error id=builder_build field=mTmpAttrs", it)
                        }

                        if (drawer != null && attrs != null && drawerGetWindowAttrsMethod != null && fContext != null) {
                            runCatching {
                                val context = fContext.get(self)
                                if (drawerGetWindowAttrsStatic) {
                                    drawerGetWindowAttrsMethod.invoke(null, context, attrs)
                                } else {
                                    drawerGetWindowAttrsMethod.invoke(drawer, context, attrs)
                                }
                                log(
                                    Log.INFO, TAG,
                                    "event=hook_hit id=builder_build decision=reparse_window_attrs -> mTmpAttrs"
                                )
                            }.onFailure {
                                log(Log.WARN, TAG, "event=hook_error id=builder_build decision=reparse_window_attrs", it)
                            }
                        }

                        // --- 清空 mTmpAttrs.mSplashScreenIcon ---
                        //   也强制替换成应用图标。虽然 mSuggestType 已强制为 1（build() 不会进
                        //   3/4 的 XML 图标分支），但清空 mSplashScreenIcon 能兜底：
                        //   万一某 ROM 版本在 mSuggestType==1 时仍读取 mSplashScreenIcon，
                        //   这里也保证它走 HighResIconProvider.getIcon() 拿应用图标。
                        //   （"从图标取色"已废弃，不再依赖 mSplashScreenIcon 取色。）
                        runCatching {
                            val fSplashIcon = attrs?.javaClass?.getDeclaredField("mSplashScreenIcon")
                                ?.apply { isAccessible = true }
                            if (fSplashIcon != null && attrs != null) {
                                val cur = fSplashIcon.get(attrs)
                                if (cur != null) {
                                    fSplashIcon.set(attrs, null)
                                    log(Log.INFO, TAG, "event=hook_hit id=builder_build field=mSplashScreenIcon decision=clear -> null")
                                }
                            }
                        }.onFailure {
                            log(Log.WARN, TAG, "event=hook_error id=builder_build field=mSplashScreenIcon", it)
                        }

                        // --- 清空 mTmpAttrs.mBrandingImage（"关闭截图覆盖"打开时） ---
                        //   会 setBrandingDrawable(...) 把品牌图贴在底部（smali 第 340-391 行）。
                        //   用户要求：打开"关闭截图覆盖"后不显示任何图片，包括底部品牌图。
                        if (DISABLE_PREVIEW) {
                            runCatching {
                                val fBranding = attrs?.javaClass?.getDeclaredField("mBrandingImage")
                                    ?.apply { isAccessible = true }
                                if (fBranding != null && attrs != null) {
                                    val cur = fBranding.get(attrs)
                                    if (cur != null) {
                                        fBranding.set(attrs, null)
                                        log(Log.INFO, TAG, "event=hook_hit id=builder_build field=mBrandingImage decision=clear -> null")
                                    }
                                }
                            }.onFailure {
                                log(Log.WARN, TAG, "event=hook_error id=builder_build field=mBrandingImage", it)
                            }
                        }

                        chain.proceed()
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=builder_build " +
                    "target=SplashViewBuilder#build count=${methods.size} reparseAttrs=${drawerGetWindowAttrsMethod != null}"
            )
        } catch (t: Throwable) {
            logHookFailure("builder_build", t)
        }
    }

    // ===================== D. 兜底层：setContentViewBackground 阻断

    /**
     * OplusShellStartingWindowManager#setContentViewBackground(SplashScreenView, Drawable)
     *
     * 该方法内部只是把 lambda$setContentViewBackground$0（唯一语句 SplashScreenView.setBackground）
     * 投递到 RemoteCallExecutor 执行。阻断它，则 XML 图永远不会被 setBackground() 贴上去。
     *
     * 参考项目同样是 before + result = null。
     */
    private fun installBlockContentBackgroundHook(cl: ClassLoader) {
        //
        //   参照仓库 ColorOSHookHandler 是**无条件**拦截 setContentViewBackground
        //   （result = null，没有 DISABLE_PREVIEW 之类的门控）。
        //   原因：只要 FORCE_NATIVE 强制走原生 AOSP SplashScreen 路径，
        //   XML 预览图就**永远不该**被 setBackground() 贴上去——因为
        //   build() 里已经通过 fillViewWithIcon → setBackgroundColor(mThemeColor)
        //   把背景色写好了，setContentViewBackground 再 setBackground(drawable)
        //   会**整体替换**掉这个纯色背景。而部分应用的 XML preview bitmap
        //   是透明的，于是"关闭截图覆盖"（DISABLE_PREVIEW=false，不拦截）
        //   时这些应用的遮罩背景就变成了透明。
        //
        //   因此：只要 FORCE_NATIVE，就无条件拦截（对齐参照仓库）。
        try {
            val cls = cl.loadClass(CLS_OPLUS_MANAGER)
            val methods = cls.declaredMethods.filter {
                it.name == "setContentViewBackground" && it.returnType == Void.TYPE
            }
            if (methods.isEmpty()) throw NoSuchMethodException("$CLS_OPLUS_MANAGER#setContentViewBackground")

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("oplus_set_content_bg")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        if (FORCE_NATIVE) {
                            log(Log.INFO, TAG, "event=hook_hit id=oplus_set_content_bg decision=block_xml_overlay")
                            null
                        } else {
                            chain.proceed()
                        }
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=oplus_set_content_bg " +
                    "target=OplusShellStartingWindowManager#setContentViewBackground count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("oplus_set_content_bg", t)
        }
    }

    // ============================================== E. 动画层：恢复 ripple

    
    private fun installRestoreRippleHooks(cl: ClassLoader) {
        // ---- 1) SplashScreenExitAnimationUtils#startAnimations 第 0 参强制 0 ----
        try {
            val cls = cl.loadClass(CLS_EXIT_ANIM_UTILS)
            val methods = cls.declaredMethods.filter { m ->
                m.name == "startAnimations" &&
                    m.parameterCount >= 2 &&
                    m.parameterTypes[0] == Int::class.javaPrimitiveType
            }
            if (methods.isEmpty()) {
                throw NoSuchMethodException("$CLS_EXIT_ANIM_UTILS#startAnimations(int, ...)")
            }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("exit_anim_utils")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val args = chain.args.toTypedArray()
                        val original = args.getOrNull(0)
                        if (FORCE_NATIVE && original is Int && original != EXIT_ANIM_RIPPLE) {
                            log(
                                Log.INFO, TAG,
                                "event=hook_hit id=exit_anim_utils original=$original " +
                                    "decision=force_ripple -> $EXIT_ANIM_RIPPLE"
                            )
                            args[0] = EXIT_ANIM_RIPPLE
                            return@intercept chain.proceed(args)
                        }
                        chain.proceed()
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=exit_anim_utils " +
                    "target=SplashScreenExitAnimationUtils#startAnimations count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("exit_anim_utils", t)
        }

        // ---- 2) SplashScreenExitAnimation#<init> 后写回 mAnimationType = 0 ----
        try {
            val cls = cl.loadClass(CLS_EXIT_ANIM)
            val ctor: Executable = cls.declaredConstructors.maxByOrNull { it.parameterCount }
                ?: throw NoSuchMethodException("$CLS_EXIT_ANIM has no constructor")

            val field: Field? = runCatching {
                cls.getDeclaredField("mAnimationType").apply { isAccessible = true }
            }.getOrNull()
            if (field == null) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-FLD-001 id=exit_anim_ctor reason=mAnimationType_not_found")
            }

            ctor.isAccessible = true
            hook(ctor)
                .setId("exit_anim_ctor")
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    chain.proceed()
                    refreshConfigSilently()
                    if (FORCE_NATIVE && field != null) {
                        runCatching {
                            val current = field.get(chain.thisObject)
                            if (current != EXIT_ANIM_RIPPLE) {
                                log(
                                    Log.INFO, TAG,
                                    "event=hook_hit id=exit_anim_ctor original=$current " +
                                        "decision=force_ripple -> $EXIT_ANIM_RIPPLE"
                                )
                                field.set(chain.thisObject, EXIT_ANIM_RIPPLE)
                            }
                        }.onFailure {
                            log(Log.WARN, TAG, "event=hook_error id=exit_anim_ctor fallback=true reason=field_write_failed", it)
                        }
                    }
                    null
                }
            log(Log.INFO, TAG, "event=install_hook result=ok id=exit_anim_ctor target=SplashScreenExitAnimation#<init>")
        } catch (t: Throwable) {
            logHookFailure("exit_anim_ctor", t)
        }
    }

    // ================== F. 补充：窗口类型改写（兜住截图/legacy 的其它入口）

    /**
     * 这些都是 wmshell 内部的类，和 A/B/C/D/E 一样位于 SystemUI 进程。
     */
    private fun installLegacyTypeRewriteHooks(cl: ClassLoader) {
        hookIntReturning(
            cl, CLS_PHONE_TYPE_ALGORITHM, "getSuggestedWindowType",
            id = "phone_suggest_type",
            label = "PhoneStartingWindowTypeAlgorithm#getSuggestedWindowType"
        ) { original ->
            if (original == TYPE_SNAPSHOT || original == TYPE_LEGACY_SPLASH_SCREEN) TYPE_SPLASH_SCREEN else original
        }

        hookIntReturning(
            cl, CLS_OPLUS_MANAGER, "getReviseStartingWindowType",
            id = "oplus_revise_type",
            label = "OplusShellStartingWindowManager#getReviseStartingWindowType"
        ) { original ->
            if (original == TYPE_LEGACY_SPLASH_SCREEN) TYPE_SPLASH_SCREEN else original
        }

        hookIntReturning(
            cl, CLS_OPLUS_MANAGER, "adjustSuggestedWindowType",
            id = "oplus_adjust_type",
            label = "OplusShellStartingWindowManager#adjustSuggestedWindowType"
        ) { original ->
            if (original == TYPE_LEGACY_SPLASH_SCREEN) TYPE_SPLASH_SCREEN else original
        }
    }

    // ======================= G. 图标栏：圆角 / 缩小 / 替换获取方式
    //
    //   导致「替换图标获取方式」「缩小图标」在 ColorOS 上全部失效。真正的生效点只有一个：
    //
    //     SplashscreenContentDrawer$HighResIconProvider#getIcon(ActivityInfo, int, int, ...)
    //     / com.android.launcher3.icons.IconProvider#getIcon
    //
    //   其**第一个参数就是 ActivityInfo**，包名/Activity 直接从 args[0] 拿，
    //   在 after 阶段对返回值 Drawable 做「替换获取方式 → 缩小 → 圆角」三连处理，
    //   再用 BitmapDrawable 回填 result。这一步与 ColorOS 的真实调用链完全吻合。

    
    private fun installIconHooks(cl: ClassLoader) {
        val appContext = hostAppContext()

        //   参照仓库 ColorOSHookHandler 只 hook 了这一个方法（getIconExt_OplusShellStartingWindowManager）。
        //
        //   HighResIconProvider#getIcon / IconProvider#getIcon 内部最终都会回调到 getIconExt，
        //   而 getIconExt 的 4 参重载内部又会调 6 参重载。若同时 hook 多个入口，
        //   同一张图标会被 processIconDrawable 反复 bitmap 化（圆角叠加、缩小叠加），
        //
        //   因此这里**只 hook getIconExt 一个入口**，并在内部用去重标记（ThreadLocal）
        //   阻止 4 参 → 6 参 的级联重复处理。
        installGetIconExtHook(cl, appContext)

        // ---- 图标背景保护：强制 IconColor.mIsBgComplex = true ----
        //   防止系统对"背景简单"的自适应图标走无背景放大路径（图标巨大、背景被抹掉）。
        installIconColorComplexHook(cl)

        // ---- 缩小图标 + 替换图标兜底：hook createIconDrawable 的 before ----
        //   改 mFinalIconSize 字段（÷1.5），并在 getIconExt 未命中时兜底替换图标、
        //   补算「是否需要缩小」（实测本 ROM 上 getIconExt 从不命中）。
        installCreateIconDrawableShrinkHook(cl, appContext)

        // ---- 缩小图标兜底：hook fillViewWithIcon 的 p1（createIconDrawable 未生效时补刀） ----
        installFillViewWithIconShrinkHook(cl)

        // ---- 阻止 SystemUI 对非自适应图标二次缩放（修复"强制不缩小"不生效） ----
        installBaseIconFactoryHooks(cl, appContext)
    }

    
    private fun installIconColorComplexHook(cl: ClassLoader) {
        try {
            val cls = cl.loadClass(
                "com.android.wm.shell.startingsurface.SplashscreenContentDrawer\$ColorCache\$IconColor"
            )
            // 精确匹配 <init>(IIIZZF)：6 参且第 4 个参数是 boolean（对应 mIsBgComplex）
            val ctor = cls.declaredConstructors.firstOrNull { c ->
                c.parameterCount == 6 &&
                    c.parameterTypes[3] == Boolean::class.javaPrimitiveType
            } ?: throw NoSuchMethodException("${cls.name}.<init>(IIIZZF)")

            ctor.isAccessible = true
            hook(ctor)
                .setId("icon_color_is_bg_complex")
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    val args = chain.args.toTypedArray()
                    // 参数顺序 (IIIZZF)：index 3 = isBgComplex（smali p4）
                    if (FORCE_NATIVE && args.size > 3 && args[3] != true) {
                        args[3] = true
                        log(
                            Log.INFO, TAG,
                            "event=hook_hit id=icon_color_is_bg_complex " +
                                "decision=keep_icon_background isBgComplex=false -> true"
                        )
                    }
                    chain.proceed(args)
                }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=icon_color_is_bg_complex target=IconColor#<init>"
            )
        } catch (t: Throwable) {
            logHookFailure("icon_color_is_bg_complex", t)
        }
    }

    
    private fun installCreateIconDrawableShrinkHook(
        cl: ClassLoader,
        appContext: android.content.Context?
    ) {
        try {
            val cls = loadSplashViewBuilderClass(cl)
            val methods = cls.declaredMethods.filter { m ->
                m.name == "createIconDrawable" &&
                    m.parameterTypes.firstOrNull() == Drawable::class.java
            }
            if (methods.isEmpty()) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-SIG-001 id=builder_create_icon_drawable reason=method_not_found")
                return
            }

            // 缓存 mFinalIconSize 字段
            val fFinalIconSize = cls.declaredFields.firstOrNull { it.name == "mFinalIconSize" }
                ?.apply { isAccessible = true }

            if (fFinalIconSize == null) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-FLD-001 id=builder_create_icon_drawable reason=mFinalIconSize_not_found")
                return
            }

            //   用于兜底替换图标（不再依赖 getIconExt 是否命中）。
            val fActivityInfo = cls.declaredFields.firstOrNull { it.name == "mActivityInfo" }
                ?.apply { isAccessible = true }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("builder_create_icon_drawable")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val args = chain.args.toTypedArray()

                        if (FORCE_NATIVE) {
                            runCatching {
                                val self = chain.thisObject
                                val ai = fActivityInfo?.get(self) as? ActivityInfo
                                val pkg = ai?.packageName?.takeIf { it.isNotBlank() }
                                    ?: currentPackageName

                                // ---- 1) 替换图标获取方式（兜底：getIconExt 未命中时仍然生效） ----
                                if (REPLACE_ICON && !iconReplaced &&
                                    pkg.isNotBlank() && appContext != null
                                ) {
                                    val newIcon = runCatching {
                                        appContext.packageManager.getApplicationIcon(pkg)
                                    }.getOrNull()
                                    if (newIcon != null && args.getOrNull(0) is Drawable) {
                                        args[0] = newIcon
                                        iconReplaced = true
                                        currentIconDrawable = newIcon
                                        log(
                                            Log.INFO, TAG,
                                            "event=hook_hit id=builder_create_icon_drawable " +
                                                "decision=replace_icon pkg=$pkg " +
                                                "newClass=${newIcon.javaClass.simpleName}"
                                        )
                                    }
                                }

                                // ---- 2) 补算「是否需要缩小」（getIconExt 未命中时在此兜底） ----
                                if (!iconStateDecided) {
                                    val d = args.getOrNull(0) as? Drawable
                                    if (d != null) {
                                        val iconSize = appIconSize(cl)
                                        currentIsNeedShrinkIcon = when (SHRINK_ICON) {
                                            SHRINK_NONE -> false
                                            SHRINK_ALL -> true
                                            else -> false
                                        }
                                        iconStateDecided = true
                                        if (!iconReplaced) currentIconDrawable = d
                                        log(
                                            Log.INFO, TAG,
                                            "event=icon_state pkg=$pkg " +
                                                "shrinkMode=$SHRINK_ICON iconSize=$iconSize " +
                                                "intrinsic=${d.intrinsicWidth} " +
                                                "isAdaptive=${d is android.graphics.drawable.AdaptiveIconDrawable} " +
                                                "needShrink=$currentIsNeedShrinkIcon " +
                                                "source=create_icon_drawable"
                                        )
                                    }
                                }

                                // ---- 3) 缩小：改 mFinalIconSize（同一次生命周期只缩一次） ----
                                if (currentIsNeedShrinkIcon && !iconShrinkApplied) {
                                    val cur = fFinalIconSize.get(self) as? Int
                                    if (cur != null && cur > 0) {
                                        val newSize = (cur / 1.5).toInt()
                                        if (newSize > 0) {
                                            fFinalIconSize.set(self, newSize)
                                            iconShrinkApplied = true
                                            log(
                                                Log.INFO, TAG,
                                                "event=hook_hit id=builder_create_icon_drawable " +
                                                    "decision=shrink mFinalIconSize=$cur -> $newSize"
                                            )
                                        }
                                    }
                                }
                            }.onFailure {
                                log(Log.WARN, TAG, "event=hook_error id=builder_create_icon_drawable", it)
                            }
                        }
                        chain.proceed(args)
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=builder_create_icon_drawable " +
                    "target=${cls.name}#createIconDrawable count=${methods.size} " +
                    "mActivityInfo=${fActivityInfo != null}"
            )
        } catch (t: Throwable) {
            logHookFailure("builder_create_icon_drawable", t)
        }
    }

    
    private fun installFillViewWithIconShrinkHook(cl: ClassLoader) {
        try {
            val cls = loadSplashViewBuilderClass(cl)
            val methods = cls.declaredMethods.filter { m ->
                m.name == "fillViewWithIcon" &&
                    m.parameterTypes.firstOrNull() == Int::class.javaPrimitiveType
            }
            if (methods.isEmpty()) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-SIG-001 id=builder_fill_view_with_icon_shrink reason=method_not_found")
                return
            }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("builder_fill_view_with_icon_shrink")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()

                        if (FORCE_NATIVE && currentIsNeedShrinkIcon && !iconShrinkApplied) {
                            val args = chain.args.toTypedArray()
                            val cur = args.getOrNull(0) as? Int
                            if (cur != null && cur > 0) {
                                val newSize = (cur / 1.5).toInt()
                                if (newSize > 0) {
                                    args[0] = newSize
                                    iconShrinkApplied = true
                                    log(
                                        Log.INFO, TAG,
                                        "event=hook_hit id=builder_fill_view_with_icon_shrink " +
                                            "decision=shrink_fallback iconSize=$cur -> $newSize"
                                    )
                                    return@intercept chain.proceed(args)
                                }
                            }
                        }
                        chain.proceed()
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=builder_fill_view_with_icon_shrink " +
                    "target=${cls.name}#fillViewWithIcon count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("builder_fill_view_with_icon_shrink", t)
        }
    }

    
    private fun installGetIconExtHook(cl: ClassLoader, appContext: android.content.Context?) {
        try {
            val cls = cl.loadClass(CLS_OPLUS_MANAGER)
            //   6 参 (Context, ActivityInfo, int, ZZZ)。而 HighResIconProvider#getIcon
            //   内部通过 IOplusShellStartingWindowManager 接口调用的是 **6 参**版本。
            //   "替换图标获取方式"因此静默失效。
            //
            //   这里匹配所有 getIconExt 重载（3..6 参），用 ThreadLocal 去重阻断 4 参→6 参的级联。
            val methods = cls.declaredMethods.filter {
                it.name == "getIconExt" &&
                    it.returnType == Drawable::class.java &&
                    it.parameterCount in 3..6
            }
            if (methods.isEmpty()) throw NoSuchMethodException("$CLS_OPLUS_MANAGER#getIconExt")

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("oplus_get_icon_ext")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val original = chain.proceed() as? Drawable ?: return@intercept null

                        if (!FORCE_NATIVE) return@intercept original

                        //   同一图标会被处理两次（圆角/缩小叠加）。用 ThreadLocal 标记阻断级联。
                        if (iconProcessing.get() == true) return@intercept original
                        iconProcessing.set(true)
                        try {
                            val pkgName = extractPkgNameFromFirstArg(chain.args)
                            val pkgActivity = extractActivityFromFirstArg(chain.args)
                            //   processIconDrawable 的返回值**必须回填到 hook 结果**。
                            //   它内部就是"替换图标获取方式"的实际执行点（pm.getApplicationIcon
                            //   拿新图标），但它做的是 `currentIconDrawable = drawable; return drawable`
                            //   —— 只改了模块自己的状态，**没有把新图标交给系统**。
                            //   原代码只是调用了一下就不管返回值，于是：
                            //     · 系统继续用 original（旧图标）出图；
                            //     · 而 currentIconDrawable 已是新图标 —— 取色与显示彻底错位，
                            //       正是"跟随图标取色会显示成系统颜色 / 颜色对不上"的直接原因。
                            //   写上 return@intercept 才真正替换成功。
                            val processed = processIconDrawable(
                                cl, appContext, original, pkgName, pkgActivity
                            )
                            if (processed !== original) {
                                log(
                                    Log.INFO, TAG,
                                    "event=hook_hit id=oplus_get_icon_ext " +
                                        "decision=apply_replaced_drawable pkg=$pkgName " +
                                        "newClass=${processed.javaClass.simpleName}"
                                )
                            }
                            return@intercept processed
                        } finally {
                            iconProcessing.remove()
                        }
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=oplus_get_icon_ext " +
                    "target=OplusShellStartingWindowManager#getIconExt count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("oplus_get_icon_ext", t)
        }
    }

    /**
     * 挂一个「返回 Drawable 的 getIcon」提供器，after 阶段统一处理图标。
     */
    private fun installIconProviderHook(
        cl: ClassLoader,
        className: String,
        id: String,
        appContext: android.content.Context?
    ) {
        try {
            val cls = cl.loadClass(className)
            val methods = cls.declaredMethods.filter {
                it.name == "getIcon" && it.returnType == Drawable::class.java
            }
            if (methods.isEmpty()) throw NoSuchMethodException("$className#getIcon")

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId(id)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val original = chain.proceed() as? Drawable ?: return@intercept null

                        // 仅总开关打开时才处理图标（替换/缩小/圆角）
                        if (!FORCE_NATIVE) return@intercept original

                        val pkgName = extractPkgNameFromFirstArg(chain.args)
                        val pkgActivity = extractActivityFromFirstArg(chain.args)
                        //   原代码只调用、不返回，等于"替换图标获取方式"在这个入口上
                        //   永远没有真正生效（系统仍拿到 original）。
                        //   与 getIconExt 一处同源 bug，见那边的详细注释。
                        val processed = processIconDrawable(
                            cl, appContext, original, pkgName, pkgActivity
                        )
                        if (processed !== original) {
                            log(
                                Log.INFO, TAG,
                                "event=hook_hit id=$id " +
                                    "decision=apply_replaced_drawable pkg=$pkgName " +
                                    "newClass=${processed.javaClass.simpleName}"
                            )
                        }
                        return@intercept processed
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=$id target=$className#getIcon count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure(id, t)
        }
    }

    
    private fun installBaseIconFactoryHooks(cl: ClassLoader, appContext: android.content.Context?) {
        try {
            val cls = cl.loadClass(CLS_BASE_ICON_FACTORY)

            // 缓存 mIconBitmapSize 字段（用于 createIconBitmap 取正确尺寸）
            val fIconBitmapSize = cls.declaredFields.firstOrNull { it.name == "mIconBitmapSize" }
                ?.apply { isAccessible = true }

            // ---- 1) normalizeAndWrapToAdaptiveIcon(Drawable, float[]) ----
            val normalizeMethods = cls.declaredMethods.filter { m ->
                m.name == "normalizeAndWrapToAdaptiveIcon"
            }
            if (normalizeMethods.isEmpty()) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-SIG-001 id=base_icon_factory_normalize reason=method_not_found")
            } else {
                normalizeMethods.forEach { m ->
                    m.isAccessible = true
                    hook(m)
                        .setId("base_icon_factory_normalize")
                        .setPriority(XposedInterface.PRIORITY_DEFAULT)
                        .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                        .intercept { chain ->
                            refreshConfigSilently()
                            if (!FORCE_NATIVE) return@intercept chain.proceed()

                            val args = chain.args

                            // 真实签名：第一个参数是 Drawable，第二个是 float[]
                            val drawable = args.firstOrNull { it is Drawable } as? Drawable
                            val scaleArr = args.firstOrNull { it is FloatArray } as? FloatArray

                            if (drawable != null && scaleArr != null && scaleArr.isNotEmpty()) {
                                val originalScale = scaleArr[0]
                                scaleArr[0] = 1.0f

                                val returnType = m.returnType
                                if (android.graphics.drawable.AdaptiveIconDrawable::class.java
                                        .isAssignableFrom(returnType)
                                ) {
                                    val wrapped = runCatching {
                                        com.Nevkythera.ColorOSSplashScreenEvolution.wrapper
                                            .TransparentAdaptiveIconDrawable(drawable)
                                    }.getOrNull()
                                    if (wrapped != null) {
                                        log(
                                            Log.INFO, TAG,
                                            "event=hook_hit id=base_icon_factory_normalize " +
                                                "decision=force_no_shrink original_scale=$originalScale -> 1.0"
                                        )
                                        return@intercept wrapped
                                    }
                                }
                                // 无法包裹时仍放行（scale 已改成 1.0）
                                log(
                                    Log.INFO, TAG,
                                    "event=hook_hit id=base_icon_factory_normalize " +
                                        "decision=force_scale_only original_scale=$originalScale -> 1.0"
                                )
                            }
                            chain.proceed()
                        }
                }
            }

            // ---- 2) createIconBitmap(Drawable, float, int) ----
            val createIconBitmapMethods = cls.declaredMethods.filter { m ->
                m.name == "createIconBitmap" &&
                    m.parameterCount == 3 &&
                    Drawable::class.java.isAssignableFrom(m.parameterTypes[0])
            }
            if (createIconBitmapMethods.isEmpty()) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-SIG-001 id=base_icon_factory_create_icon_bitmap reason=method_not_found")
            } else {
                createIconBitmapMethods.forEach { m ->
                    m.isAccessible = true
                    hook(m)
                        .setId("base_icon_factory_create_icon_bitmap")
                        .setPriority(XposedInterface.PRIORITY_DEFAULT)
                        .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                        .intercept { chain ->
                            refreshConfigSilently()
                            if (!FORCE_NATIVE) return@intercept chain.proceed()

                            val drawable = chain.args.firstOrNull() as? Drawable
                            if (drawable != null) {
                                runCatching {
                                    // ---- 1) 尺寸取 thisObject(BaseIconFactory).mIconBitmapSize ----
                                    val self = chain.thisObject
                                    val size = (fIconBitmapSize?.get(self) as? Int)
                                        ?.takeIf { it > 0 }
                                        ?: appIconSize(cl).takeIf { it > 0 }
                                        ?: 100

                                    //
                                    //   根因：MD3E 指示器「跟随应用图标」取色拿到的
                                    //   currentIconDrawable 是 getIconExt 阶段（G 层）缓存的
                                    //   原始 Drawable，而真正画在屏幕上的是本方法产出的
                                    //   Bitmap（= 原始图标 + 圆角 + 缩放 + 透明背景包裹）。
                                    //   两者不是同一份像素，于是：
                                    //     · 图标主色取偏（有时恰好采到系统默认色）；
                                    //     · AdaptiveIconDrawable 按 (iconSize*1.2)² 采样时
                                    //       边缘是透明像素，Palette 量化后落回兜底色；
                                    //     · 与「替换图标获取方式」组合时，采的根本是上一个图标。
                                    //   这里把 createIconBitmap 的**输出**登记为取色源，取色对象
                                    //   就与屏幕上显示的内容严格一致。
                                    //   传入的 drawable 上。若它恰好是**正在显示的图标**，
                                    //   图标会当场被画坏（前景/背景层尺寸错位）。
                                    //   这里先存下 bounds，画完立刻还原。
                                    val oldBounds = drawable.copyBounds()
                                    val oldAlpha = drawable.alpha
                                    val oldFilter = drawable.colorFilter
                                    try {
                                        val bitmap = GraphicUtils.drawable2Bitmap(drawable, size)
                                        //   立刻采样出主色，晚于此刻就再也拿不到这份像素了）。
                                        registerFinalIcon(bitmap)
                                        log(
                                            Log.INFO, TAG,
                                            "event=hook_hit id=base_icon_factory_create_icon_bitmap " +
                                                "decision=avoid_shrink_by_system size=$size"
                                        )
                                        return@intercept bitmap
                                    } finally {
                                        runCatching {
                                            drawable.bounds = oldBounds
                                            drawable.alpha = oldAlpha
                                            drawable.colorFilter = oldFilter
                                        }
                                    }
                                }.onFailure {
                                    log(Log.WARN, TAG, "event=hook_error id=base_icon_factory_create_icon_bitmap", it)
                                }
                            }
                            chain.proceed()
                        }
                }
            }

            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=base_icon_factory " +
                    "target=BaseIconFactory normalize=${normalizeMethods.size} createIconBitmap=${createIconBitmapMethods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("base_icon_factory", t)
        }
    }

    
    private fun processIconDrawable(
        cl: ClassLoader,
        appContext: android.content.Context?,
        oriDrawable: Drawable,
        pkgName: String?,
        pkgActivity: String?
    ): Drawable {
        if (appContext == null) return oriDrawable

        val iconSize = appIconSize(cl)
        if (iconSize <= 0) return oriDrawable

        val pm = appContext.packageManager
        val targetPkg = pkgName ?: currentPackageName
        if (targetPkg.isBlank()) return oriDrawable

        // ---- 1) 替换获取图标方式 ----
        val drawable: Drawable = if (REPLACE_ICON || targetPkg == "com.android.settings") {
            runCatching {
                when {
                    targetPkg == "com.android.contacts" &&
                        pkgActivity == "com.android.contacts.activities.PeopleActivity" ->
                        pm.getActivityIcon(
                            android.content.ComponentName(
                                "com.android.contacts",
                                "com.android.contacts.DialtactsActivityAlias"
                            )
                        )
                    targetPkg == "com.android.settings" &&
                        pkgActivity == "com.android.settings.BackgroundApplicationsManager" ->
                        pm.getApplicationIcon("com.android.settings")
                    else -> pm.getApplicationIcon(targetPkg)
                }
            }.onSuccess {
                log(
                    Log.INFO, TAG,
                    "event=icon_replace result=ok pkg=$targetPkg activity=$pkgActivity " +
                        "newClass=${it.javaClass.simpleName}"
                )
            }.getOrElse {
                log(Log.WARN, TAG, "event=icon_replace result=fail reason=getApplicationIcon pkg=$targetPkg", it)
                oriDrawable
            }
        } else {
            oriDrawable
        }

        // ---- 2) 计算"是否需要缩小"状态（供 createIconDrawable 改 mFinalIconSize） ----
        //   createIconDrawable 侧不再重复替换 / 重复计算。
        iconReplaced = true
        iconStateDecided = true
        currentIsNeedShrinkIcon = when (SHRINK_ICON) {
            SHRINK_NONE -> false
            SHRINK_ALL -> true
            else -> false
        }
        //   这里把缩放判定的全部输入输出打出来，便于从 LSPosed 日志直接确诊。
        log(
            Log.INFO, TAG,
            "event=icon_state pkg=$targetPkg shrinkMode=$SHRINK_ICON " +
                "iconSize=$iconSize intrinsic=${drawable.intrinsicWidth} " +
                "isAdaptive=${drawable is android.graphics.drawable.AdaptiveIconDrawable} " +
                "needShrink=$currentIsNeedShrinkIcon"
        )

        // ---- 3) 记录当前图标 + 提取主色（供"从图标取色"与模糊背景使用） ----
        currentIconDrawable = drawable

        //   下面这段取色**必须彻底移除**，改为只做「登记」，取色推迟到
        //   createIconBitmap 产出最终像素之后（见 registerFinalIcon）。
        //
        //   为什么原实现一定取不准：
        //     此处处在 getIconExt（G 层）路径上，拿到的 drawable 是**原始图标**，
        //     而屏幕上最终显示的是 createIconBitmap 的产物 —— 中间还要经过
        //     normalizeAndWrapToAdaptiveIcon 包裹、IconNormalizer 归一化、
        //     createIconBitmap 缩放。原实现在这里就采样并冻结主色，等于用
        //     "半成品"的颜色去染色，跟用户看到的图标必然对不上。
        //
        //   更糟的是它还污染 Drawable：曾用 drawable2Bitmap 把采样 bounds
        //     永久留在正在显示的图标上（「图标消失 / 显示残缺 / 自适应图标前景
        //     背景尺寸不对等」的根因）。现已统一改用无副作用的 toSampleBitmap，
        //     并且**此处的采样整体删除**，连这一个隐患也一并消除。
        //
        //   保留这段日志便于在真机日志里确认「最终图标」链路是否走通。
        log(
            Log.INFO, TAG,
            "event=icon_state_registered pkg=$targetPkg " +
                "class=${drawable.javaClass.simpleName} " +
                "intrinsic=${drawable.intrinsicWidth} " +
                "note=accent_deferred_to_create_icon_bitmap"
        )
        return drawable
    }

    /**
     * 从 getIcon 的第一个参数里提取包名。
     *
     * 参考实现：args(0).cast<ActivityInfo>()?.packageName!!
     * 但 getIcon 可能有多种重载（首参不一定是 ActivityInfo），
     * 这里遍历所有参数，取第一个能解析出 ActivityInfo 的。
     */
    private fun extractPkgNameFromFirstArg(args: List<Any?>): String? {
        for (a in args) {
            val pkg = when (a) {
                is ActivityInfo -> a.packageName
                else -> runCatching {
                    val f = a?.javaClass?.getDeclaredField("packageName")
                    f?.isAccessible = true
                    f?.get(a) as? String
                }.getOrNull()
            }
            if (!pkg.isNullOrBlank()) return pkg
        }
        return null
    }

    /** 从 getIcon 参数里提取 targetActivity（供 contacts/settings 特殊处理）。 */
    private fun extractActivityFromFirstArg(args: List<Any?>): String? {
        for (a in args) {
            val act = when (a) {
                is ActivityInfo -> a.targetActivity
                else -> runCatching {
                    val f = a?.javaClass?.getDeclaredField("targetActivity")
                    f?.isAccessible = true
                    f?.get(a) as? String
                }.getOrNull()
            }
            if (!act.isNullOrBlank()) return act
        }
        return null
    }

    // （3）绘制图标圆角 已合并进 G 层 getIcon 的 bitmap 处理，见 processIconDrawable()
    //     （对齐参考实现 SystemUIHooker 的 roundBitmapByShader 处理，不再用 outlineProvider 裁剪）

    // ====================== H. 背景栏：替换背景颜色
    //
    //
    //   通道一（保留，1.6 实测有效，负责"背景遮罩不透明"这一基础需求）：
    //     SplashscreenContentDrawer#getBGColorFromCache 改返回值。
    //     —— 这个 Hook 保证 build() 用我们的颜色填充 SplashScreenView 背景，
    //       解决"背景透明"的问题。它必须保留，不能动。
    //
    //   通道二（新增，对齐参照仓库 SystemUIHooker，负责"从图标/莫奈/自定义"三色替换）：
    //     SplashViewBuilder#createIconDrawable(Drawable, boolean, ...) 的 before。
    //     —— 系统在 createIconDrawable 时会把「已处理好的图标」作为 args(0) 传入，
    //       参照仓库正是在这里用 args(0) 直接 Palette 取色，写 instance.mThemeColor。
    //       这一步不依赖任何跨方法状态（不再等 currentIconDominantColor），
    //       彻底绕开"getBGColorFromCache 在 getIcon 之前调用、currentIconDominantColor 为 null"
    //       的时序 bug —— 这正是"只能读默认颜色"的根因。

    /**
     * 背景颜色 Hook：getBGColorFromCache 改返回值 + createIconDrawable 写 mThemeColor。
     */
    private fun installBackgroundHooks(cl: ClassLoader) {
        //
        // 背景色替换的生效点（基于真实 smali）：
        //   makeSplashScreenContentView 里 getBGColorFromCache 返回 v7 →
        //   （可能被 getThemeBackgroundColor 覆盖 v7）→ setWindowBGColor(v7) 写 mThemeColor →
        //   build() → fillViewWithIcon() 读 mThemeColor → SplashScreenView$Builder.setBackgroundColor()
        //
        // 所以最可靠的替换点有两个：
        //   1) getBGColorFromCache 改返回值（对"未被 getThemeBackgroundColor 覆盖"的场景有效）；
        //   2) SplashViewBuilder.build() 的 before 里直接写 mThemeColor 字段
        //      （覆盖所有场景，包括被 getThemeBackgroundColor 覆盖的情况）。
        //
        // 这里保留 1) 作为第一道，真正的兜底在 C 层 builder_build 里写 mThemeColor（见下）。
        installCacheTmpAttrsHook(cl)
    }

    // ========= I. android.window.SplashScreenView$Builder#build —— 圆角 + 模糊背景 Hook
    //
    //   1) 绘制圆角：给 mIconView 设 ViewOutlineProvider + clipToOutline = true
    //   2) 模糊背景：缩小图标后，在图标背后叠一层放大的模糊图标当背景

    private fun installSplashViewBuildHook(cl: ClassLoader) {
        try {
            val cls = cl.loadClass(CLS_SPLASH_VIEW_BUILDER_FRAMEWORK)
            val methods = cls.declaredMethods.filter { it.name == "build" && it.parameterCount == 0 }
            if (methods.isEmpty()) throw NoSuchMethodException("$CLS_SPLASH_VIEW_BUILDER_FRAMEWORK#build")

            val appContext = hostAppContext()
            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("splash_view_builder_build")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val master = FORCE_NATIVE

                        val result = chain.proceed()

                        if (!master) return@intercept result

                        applyIconDecor(cl, result, source = "framework_builder")

                        result
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=splash_view_builder_build " +
                    "target=SplashScreenView\$Builder#build count=${methods.size} gate=live"
            )
        } catch (t: Throwable) {
            logHookFailure("splash_view_builder_build", t)
        }
    }

    // ========= J. 热启动也适用启动遮罩（system_server / 系统框架作用域）
    //
    //
    //   activityRecordClass.method { name = "getStartingWindowType"; paramCount(7) }.hook {
    //       before {
    //           val isHotStartCompatible = prefs.get(ENABLE_HOT_START_COMPATIBLE)
    //                   && prefs.get(FORCE_ENABLE_SPLASH_SCREEN) && args(1).boolean()
    //           if (isHotStartCompatible) result = 2
    //       }
    //   }
    //
    //   原理：热启动（Activity 已在后台任务栈里，进程还活着）时系统默认复用任务快照
    //   （STARTING_WINDOW_TYPE_SNAPSHOT = 1），不会再生成 Splash Screen。
    //   这里在"热启动"参数为真时把窗口类型改成 SPLASH_SCREEN(2)，
    //   强制系统重新走一次启动遮罩流程。
    //
    //   因此模块默认作用域必须包含「系统框架（android）」——
    //   见 resources/META-INF/xposed/scope.list。

    /**
     * hook com.android.server.wm.ActivityRecord#getStartingWindowType（system_server）。
     *
     * 参数个数随 Android 版本变化（AOSP 12 是 6 个，13+ 是 7 个），
     * 这里用 5..8 的宽松区间匹配，避免版本差异导致静默失效。
     */
    private fun installHotStartSplashHook(cl: ClassLoader) {
        try {
            val cls = cl.loadClass(CLS_ACTIVITY_RECORD)
            //   （AOSP 12/13/14/15 各有增删），写死个数会直接 NoSuchMethodException。
            //   我们**只看返回值**做判断，所以签名怎么变都不影响。
            val methods = cls.declaredMethods.filter { m ->
                m.name == "getStartingWindowType" && m.returnType == Int::class.javaPrimitiveType
            }
            if (methods.isEmpty()) {
                throw NoSuchMethodException("$CLS_ACTIVITY_RECORD#getStartingWindowType")
            }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("android_hot_start_splash")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()

                        if (!FORCE_NATIVE || !ENABLE_HOT_START_SPLASH) {
                            return@intercept chain.proceed()
                        }

                        //   热启动的典型表现就是走快照，把它换成 Splash Screen 即可。
                        //
                        //    启动窗口"的场景（半透明 Activity / 同 App 内跳转）
                        //    也强行改成 Splash，容易闪一下，已去掉。）
                        val original = chain.proceed() as? Int
                        val needForce = original == AR_STARTING_WINDOW_TYPE_SNAPSHOT

                        if (needForce) {
                            log(
                                Log.INFO, TAG,
                                "event=hook_hit id=android_hot_start_splash " +
                                    "decision=force_splash_screen paramCount=${m.parameterCount} " +
                                    "original=$original " +
                                    "result=$AR_STARTING_WINDOW_TYPE_SPLASH_SCREEN"
                            )
                            return@intercept AR_STARTING_WINDOW_TYPE_SPLASH_SCREEN
                        }
                        original
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=android_hot_start_splash " +
                    "target=$CLS_ACTIVITY_RECORD#getStartingWindowType count=${methods.size} " +
                    "signatures=${methods.joinToString("|") { "${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }}):${it.returnType.simpleName}" }} " +
                    "gate=live"
            )
        } catch (t: Throwable) {
            logHookFailure("android_hot_start_splash", t)
        }
    }

    
    private fun installSplashViewDecorHook(cl: ClassLoader) {
        try {
            val cls = loadSplashViewBuilderClass(cl)
            val methods = cls.declaredMethods.filter {
                it.name == "build" && it.parameterCount == 0
            }
            if (methods.isEmpty()) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-SIG-001 id=wm_shell_builder_decor reason=method_not_found")
                return
            }

            val appContext = hostAppContext()
            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("wm_shell_builder_decor")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val result = chain.proceed()
                        if (!FORCE_NATIVE) return@intercept result

                        applyIconDecor(cl, result, source = "wm_shell_builder")
                        result
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=wm_shell_builder_decor " +
                    "target=${cls.name}#build count=${methods.size} gate=live"
            )
        } catch (t: Throwable) {
            logHookFailure("wm_shell_builder_decor", t)
        }
    }

    /**
     * 统一执行"圆角 + 模糊背景"装饰（两个 build() 出口共用，靠 iconDecorApplied 去重）。
     */
    private fun applyIconDecor(
        cl: ClassLoader,
        splashScreenView: Any?,
        source: String
    ) {
        //
        // 在本 ROM 上被状态栏/控制中心/锁屏/通知等大量**非 Splash 的 View** 复用，
        // 模糊层与 MD3E 指示器于是被加得到处都是。
        //
        // 这里沿整个继承链精确比对类名，凡不是 SplashScreenView 的（哪怕也是
        // FrameLayout 子类）一律直接丢弃，从根上杜绝误装饰。
        if (!isSplashScreenView(splashScreenView)) {
            log(
                Log.WARN, TAG,
                "event=decor_skip reason=not_splash_screen_view source=$source " +
                    "actual=${splashScreenView?.javaClass?.name ?: "null"}"
            )
            return
        }
        val view = splashScreenView as FrameLayout

        //   而 onPackageReady 早于 Application 创建，拿到的是 null —— 这个 null 被
        //   一路传下去，addIconBlurBackground 第一行就静默 return 了，这正是
        //   "blur_check 全绿、却既无 hook_hit 也无报错"的元凶。
        val appContext = view.context
        if (appContext == null) {
            log(Log.WARN, TAG, "event=hook_error id=apply_icon_decor source=$source reason=view_context_null")
            return
        }

        if (REMOVE_ICON) {
            val hidden = hideAllIconViews(view)
            log(
                Log.INFO, TAG,
                "event=hook_hit id=remove_icon source=$source decision=hide_all_icons " +
                    "hiddenCount=$hidden"
            )
            iconDecorApplied = true
            return
        }

        if (iconDecorApplied) return

        //   没算出来，模糊就跟着一起失效（且没有任何报错）。现在解耦。
        val needBlur = ENABLE_ICON_BLUR_BG
        val iconSize = appIconSize(cl)
        log(
            Log.INFO, TAG,
            "event=blur_check source=$source enable=$ENABLE_ICON_BLUR_BG " +
                "needShrink=$currentIsNeedShrinkIcon needBlur=$needBlur " +
                "morph=$ENABLE_MORPH_SHAPE " +
                "roundCorner=$DRAW_ROUND_CORNER hasIcon=${currentIconDrawable != null} " +
                "hasBlurView=${findBlurView(view) != null} " +
                "iconSize=$iconSize childCount=${view.childCount}"
        )

        runCatching {
            if (DRAW_ROUND_CORNER) {
                drawIconRoundCorner(cl, splashScreenView)
                iconDecorApplied = true
            }
            //   即使 build() 阶段加的模糊层被 ROM 重建子 View 清掉，
            //   onAttachedToWindow 出口也能检测到"没有"并补上。
            //
            //   同时开会让图标底下糊成一团，所以开了形变就跳过模糊。
            if (ENABLE_MORPH_SHAPE) {
                if (findTagView(view, TAG_MORPH_SHAPE) == null) {
                    addMorphShape(cl, view, source)
                }
            } else if (needBlur && findTagView(view, TAG_BLUR_BG) == null) {
                addIconBlurBackground(cl, splashScreenView)
            }
        }.onFailure {
            log(Log.WARN, TAG, "event=hook_error id=apply_icon_decor source=$source", it)
        }
    }

    /** 在 view 树里找我们加的装饰层（用 tag 标识）。 */
    private fun findTagView(view: View, tag: String): View? {
        if (tag == view.tag) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i) ?: continue
                val found = findTagView(child, tag)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * 判断某个对象是不是真正的 android.window.SplashScreenView。
     *
     * 沿继承链精确比对类名（用 `getClass()`，不碰 ClassLoader，避免加载失败）。
     * 这样既能放过 SplashScreenView 自己，也能放过 ColorOS 对其做的子类化；
     * 但其它任何 View（状态栏、控制中心、锁屏、通知里的 FrameLayout 等）都会被拒绝。
     */
    private fun isSplashScreenView(obj: Any?): Boolean {
        if (obj !is View) return false
        var c: Class<*>? = obj.javaClass
        while (c != null) {
            if (c.name == CLS_SPLASH_SCREEN_VIEW) return true
            c = c.superclass
        }
        return false
    }

    /** 在 view 树里找我们加的模糊层。 */
    private fun findBlurView(view: View): View? = findTagView(view, TAG_BLUR_BG)

    
    private fun addMorphShape(cl: ClassLoader, splashScreenView: FrameLayout, source: String) {
        runCatching {
            val appContext = splashScreenView.context
            var rawIconSize = appIconSize(cl)
            if (rawIconSize <= 0) rawIconSize = dp2px(appContext, 108f)
            val iconSize = (rawIconSize / 1.5).toInt().coerceAtLeast(1)
            // 基准：色块直径 = （缩小后）图标的 2 倍（录屏实测比例），再乘用户调的倍率
            val scale = MORPH_SHAPE_SCALE.coerceIn(50, 300) / 100f
            val size = (iconSize * 2f * scale).toInt().coerceAtLeast(1)

            val fallback = monetTint(appContext)
            val morphView = MorphShapeView(appContext).apply {
                tag = TAG_MORPH_SHAPE
                z = -1f
                setTint(fallback)
            }
            // 先用 CENTER 挂上去（保证首帧不飞），随后 [alignToIconCenter] 会按图标
            // 实际位置重设 margin —— 图标不一定在 SplashScreenView 正中。
            splashScreenView.addView(
                morphView,
                0,
                FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            )
            alignToIconCenter(
                splashScreenView, morphView, findIconView(splashScreenView), size, what = "morph"
            )

            log(
                Log.INFO, TAG,
                "event=hook_hit id=add_morph_shape source=$source iconSize=$iconSize " +
                    "scale=$scale size=$size colorType=$MORPH_SHAPE_COLOR_TYPE " +
                    "childCount=${splashScreenView.childCount}"
            )

            // 跟随应用图标取色：Palette 是**同步**计算，直接跑在主线程会掉帧，
            // 所以丢到单线程池里算，算完再 post 回主线程染色。
            //
            //   1. [iconAccentColor] 内部只在**独立采样位图**上取色，绝不改动原图标
            //      Drawable 的 bounds（原实现用 drawable2Bitmap 会把 64px bounds 留在
            //      图标上，图标当场被画坏）；
            //   2. 后台线程只做纯计算，**不碰任何 View**；染色统一 post 回主线程；
            //   3. post 前校验视图仍 attach 到窗口，且 Splash 未被新一轮启动替换
            //      （避免给已 detach / 已回收的旧视图设色）。
            if (MORPH_SHAPE_COLOR_TYPE == MORPH_COLOR_FROM_ICON) {
                //   1) currentFinalIconAccentColor —— 由 createIconBitmap 在**最终像素**上
                //      提前算好（最准，且不受跨生命周期竞态影响）；
                //   2) 若还没算出来（颜色值尚未回填），退回现算，但采样对象换成
                //      **最终图标位图** currentIconBitmap（而非 getIconExt 的原始 Drawable）；
                //   3) 两者都没有才退回原始 Drawable —— 这条路径下取色可能与屏幕略有偏差，
                //      日志里会明确标注 result=fallback_drawable。
                //
                //   原实现只有 (3)，所以「取色偏移」和「有时显示系统颜色」一直存在：
                //   屏幕上是 createIconBitmap 的产物，采的却是更早一站的原始 Drawable。
                val cachedColor = currentFinalIconAccentColor
                val finalBitmap = currentIconBitmap
                val drawable = currentIconDrawable

                if (cachedColor != null) {
                    // 已经算好了：主线程直接染色，零延迟、零量化开销
                    applyMorphTint(morphView, cachedColor, "final_bitmap_cached", fallback)
                } else if (finalBitmap != null) {
                    ICON_COLOR_EXECUTOR.execute {
                        val color = runCatching {
                            paletteAccentFromBitmap(finalBitmap, isDarkMode(appContext))
                        }.getOrNull()
                        applyMorphTint(morphView, color ?: fallback, "final_bitmap", fallback)
                    }
                } else if (drawable != null) {
                    ICON_COLOR_EXECUTOR.execute {
                        // 后台线程：纯计算，异常一律吞掉，绝不外泄（否则线程池线程死掉）
                        val color = runCatching {
                            iconAccentColor(drawable, isDarkMode(appContext))
                        }.getOrNull()
                        applyMorphTint(morphView, color ?: fallback, "fallback_drawable", fallback)
                    }
                } else {
                    log(
                        Log.WARN, TAG,
                        "event=hook_error id=morph_icon_color reason=icon_drawable_null keep=monet"
                    )
                }
            }
        }.onFailure {
            log(Log.WARN, TAG, "event=hook_error id=add_morph_shape source=$source", it)
        }
    }

    /**
     * 把颜色应用到几何形变视图上（统一入口，主线程调用）。
     *
     * 抽出这个方法的理由：取色有「缓存命中 / 位图现算 / 退回 Drawable」三条路径，
     * 三条都要做同样的事 —— post 回主线程 + attach 校验 + 吞掉异常 + 打日志。
     * 复制三遍迟早会漏掉其中一条（历史上 `post_failed` 分支就是这样被漏掉的）。
     *
     * @param color    最终颜色（调用方已保证非 null，取色失败时传 fallback）
     * @param source   取色来源标记，只用于日志定位
     * @param fallback 兜底色，用于区分「真取到了」还是「回落莫奈」
     */
    private fun applyMorphTint(
        morphView: MorphShapeView,
        color: Int,
        source: String,
        fallback: Int
    ) {
        runCatching {
            morphView.post {
                runCatching {
                    // 视图必须仍挂在窗口上，否则 setTint 的 invalidate 无意义
                    // （极端情况下视图已 detach，留着也不报错，但白干）
                    if (!morphView.isAttachedToWindow) {
                        log(
                            Log.WARN, TAG,
                            "event=hook_error id=morph_icon_color " +
                                "reason=detached source=$source keep=monet"
                        )
                        return@runCatching
                    }
                    morphView.setTint(color)
                    log(
                        Log.INFO, TAG,
                        "event=hook_hit id=morph_icon_color " +
                            "source=$source " +
                            "result=${if (color == fallback) "fallback_monet" else "ok"} " +
                            "color=#${Integer.toHexString(color)}"
                    )
                }
            }
        }.onFailure {
            log(Log.WARN, TAG, "event=hook_error id=morph_icon_color reason=post_failed source=$source")
        }
    }

    /**
     * 找到 SplashScreenView 上的图标 View。
     *
     * 优先反射 `mIconView` 字段（AOSP 原生字段名）；部分 ROM 改过字段名，
     * 则退回遍历直接子 View 找第一个 ImageView。
     */
    private fun findIconView(splashScreenView: View): ImageView? {
        runCatching {
            var v: Class<*>? = splashScreenView.javaClass
            while (v != null && v != View::class.java) {
                val f = runCatching { v!!.getDeclaredField("mIconView") }.getOrNull()
                if (f != null) {
                    f.isAccessible = true
                    val iv = f.get(splashScreenView) as? ImageView
                    if (iv != null) return iv
                }
                v = v.superclass
            }
        }
        if (splashScreenView is android.view.ViewGroup) {
            for (i in 0 until splashScreenView.childCount) {
                val child = splashScreenView.getChildAt(i)
                if (child is ImageView) return child
            }
        }
        return null
    }

    
    private fun alignToIconCenter(
        parent: FrameLayout,
        target: View,
        iconView: View?,
        size: Int,
        what: String
    ) {
        if (iconView == null) {
            log(
                Log.INFO, TAG,
                "event=hook_hit id=align_icon target=$what reason=icon_view_null keep=center"
            )
            return
        }

        /** 按图标当前位置重设 target 的 margin。返回是否成功定位。 */
        fun applyAlign(why: String): Boolean = runCatching {
            if (iconView.width <= 0 || iconView.height <= 0) return false
            if (parent.width <= 0 || parent.height <= 0) return false
            val parentLoc = IntArray(2)
            val iconLoc = IntArray(2)
            parent.getLocationOnScreen(parentLoc)
            iconView.getLocationOnScreen(iconLoc)

            val cx = iconLoc[0] - parentLoc[0] + iconView.width / 2
            val cy = iconLoc[1] - parentLoc[1] + iconView.height / 2

            // 夹到 parent 范围内，防止极端布局算出负数 margin 导致视图飞出
            val left = (cx - size / 2)
            val top = (cy - size / 2)

            val lp = target.layoutParams as? FrameLayout.LayoutParams
            if (lp != null && lp.gravity == (Gravity.TOP or Gravity.LEFT) &&
                lp.leftMargin == left && lp.topMargin == top && lp.width == size && lp.height == size
            ) {
                return true // 没变化，避免无谓的 requestLayout
            }
            target.layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.TOP or Gravity.LEFT
                leftMargin = left
                topMargin = top
            }
            log(
                Log.INFO, TAG,
                "event=hook_hit id=align_icon target=$what why=$why cx=$cx cy=$cy size=$size " +
                    "parentWH=${parent.width}x${parent.height} " +
                    "iconWH=${iconView.width}x${iconView.height}"
            )
            true
        }.getOrDefault(false)

        val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            runCatching { applyAlign("layout_change") }
        }
        iconView.addOnLayoutChangeListener(layoutListener)
        // 视图移除时注销，避免持有已回收的视图
        target.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                runCatching { iconView.removeOnLayoutChangeListener(layoutListener) }
            }
        })

        // preDraw 兜底：等首次布局完成（icon 尚未测量时 width/height 为 0）
        val observer = parent.viewTreeObserver
        if (!observer.isAlive) return
        var tries = 0
        observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                tries++
                val ok = runCatching { applyAlign("predraw") }.getOrDefault(false)
                // 定位成功，或等够 10 帧放弃（保持 CENTER）
                if (ok || tries >= 10) {
                    runCatching { parent.viewTreeObserver.removeOnPreDrawListener(this) }
                    if (!ok) {
                        log(
                            Log.INFO, TAG,
                            "event=hook_hit id=align_icon target=$what " +
                                "reason=icon_not_laid_out keep=center"
                        )
                    }
                }
                return true
            }
        })
    }

    /**
     * 莫奈取色：动态色的 primary（主强调色）。
     * 不用 background（与遮罩背景同色 = 完全看不见）。
     */
    private fun monetTint(appContext: android.content.Context): Int = runCatching {
        val dark = isDarkMode(appContext)
        val scheme = if (dark) {
            dynamicDarkColorScheme(appContext)
        } else {
            dynamicLightColorScheme(appContext)
        }
        val c = scheme.primary.toArgb()
        Color.argb(TINT_ALPHA, Color.red(c), Color.green(c), Color.blue(c))
    }.getOrDefault(Color.argb(TINT_ALPHA, 0xFF, 0xFF, 0xFF))

    
    private fun registerFinalIcon(bitmap: android.graphics.Bitmap) {
        // 自己拷一份：原 bitmap 的所有权在系统侧，我们不能持有它做异步读取
        val copy = runCatching {
            bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        }.getOrNull() ?: return

        // 上一轮遗留的副本立刻释放，避免连续启动时位图堆积
        currentIconBitmap?.let { runCatching { it.recycle() } }
        currentIconBitmap = copy

        // 主色在后台算（Palette 量化是 CPU 密集型，绝不能在主线程跑）
        ICON_COLOR_EXECUTOR.execute {
            val appContext = hostAppContext()
            val dark = appContext?.let { isDarkMode(it) } ?: false
            val color = runCatching { paletteAccentFromBitmap(copy, dark) }.getOrNull()
            currentFinalIconAccentColor = color
            log(
                Log.INFO, TAG,
                "event=final_icon_color " +
                    "result=${if (color != null) "ok" else "null"} " +
                    "color=${color?.let { "#" + Integer.toHexString(it) } ?: "none"} " +
                    "size=${copy.width}x${copy.height}"
            )
        }
    }

    /**
     * 从**最终图标位图**提取强调色。
     *
     * 注意与 [iconAccentColor] 的区别：后者接收 Drawable，需要先安全采样；
     * 本方法接收的已经是屏幕像素本身，直接量化，少一次绘制、也更准。
     *
     * 透明像素处理：图标位图四角必然是透明的（圆角 / 自适应图标外框）。
     * 为了让 Palette 不被透明区干扰，这里先按 **中心 80% 区域**裁剪 ——
     * 自适应图标的可见内容一定落在中间，裁掉外圈既提速又避免采到抗锯齿灰边。
     */
    private fun paletteAccentFromBitmap(
        src: android.graphics.Bitmap,
        dark: Boolean
    ): Int? = runCatching {
        if (src.width <= 0 || src.height <= 0) return@runCatching null

        // 中心裁剪：取中心 80% 区域，避开圆角外的透明像素与抗锯齿边缘
        val cropSize = (minOf(src.width, src.height) * 0.8f).toInt().coerceAtLeast(1)
        val x = (src.width - cropSize) / 2
        val y = (src.height - cropSize) / 2
        val cropped = if (cropSize >= minOf(src.width, src.height)) {
            src
        } else {
            runCatching { android.graphics.Bitmap.createBitmap(src, x, y, cropSize, cropSize) }
                .getOrNull() ?: src
        }

        val palette = Palette.from(cropped).maximumColorCount(16).generate()
        if (cropped !== src) runCatching { cropped.recycle() }

        // swatch 优先级：vibrant → lightVibrant → darkVibrant → muted → dominant
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
            ?: return@runCatching null

        val hsv = FloatArray(3)
        Color.colorToHSV(swatch.rgb, hsv)
        // 饱和度太低 → 抬到可见区间；明度按深浅模式夹取
        if (hsv[1] < 0.30f) hsv[1] = 0.30f
        hsv[2] = if (dark) hsv[2].coerceIn(0.45f, 0.85f) else hsv[2].coerceIn(0.50f, 0.92f)
        Color.HSVToColor(TINT_ALPHA, hsv)
    }.getOrNull()

    /**
     * 跟随应用图标取色：用 Palette 从图标里提取一个有彩色。
     *
     * 优先 vibrant，退化顺序 lightVibrant → muted → dominant；
     * 拿到后再做一次"可用性修正"：饱和度 / 明度都拉到可见区间，
     * 避免纯黑白图标（微信、设置这类）取出来是一坨灰。
     *
     * 注意：本方法**必须在后台线程调用**（Palette.generate 是同步量化，会卡顿）。
     */
    
    private fun iconAccentColor(drawable: Drawable, dark: Boolean): Int? = runCatching {
        // 先在独立画布上安全采样，不动原 Drawable 任何状态
        val bitmap = GraphicUtils.toSampleBitmap(drawable, 64) ?: run {
            log(Log.WARN, TAG, "event=hook_error id=morph_icon_color reason=sample_bitmap_null")
            return@runCatching null
        }
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            bitmap.recycle()
            return@runCatching null
        }
        val palette = Palette.from(bitmap).maximumColorCount(8).generate()
        // 采样位图用完即回收（这里是我们自己创建的副本，回收安全）
        bitmap.recycle()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
            ?: run {
                log(Log.WARN, TAG, "event=hook_error id=morph_icon_color reason=no_swatch")
                return@runCatching null
            }

        val hsv = FloatArray(3)
        Color.colorToHSV(swatch.rgb, hsv)
        // 饱和度太低 → 提上来；明度按深浅模式夹到可见区间
        if (hsv[1] < 0.30f) hsv[1] = 0.30f
        hsv[2] = if (dark) hsv[2].coerceIn(0.45f, 0.85f) else hsv[2].coerceIn(0.50f, 0.92f)
        Color.HSVToColor(TINT_ALPHA, hsv)
    }.onFailure {
        log(Log.WARN, TAG, "event=hook_error id=morph_icon_color reason=palette", it)
    }.getOrNull()

    /**
     * 移除图标：递归隐藏 SplashScreenView 上所有 ImageView（应用图标 + 品牌图）。
     *
     * 只隐藏 ImageView，不动 View 的背景色 —— 因此背景遮罩颜色仍然正常渲染，
     * 也不会影响"关闭截图覆盖"（那是清 mSplashScreenIcon / mBrandingImage 字段）。
     *
     * @return 被隐藏的 View 数量
     */
    private fun hideAllIconViews(view: View): Int {
        var count = 0
        if (view is ImageView) {
            if (view.visibility != View.GONE) {
                view.visibility = View.GONE
                count++
            }
            return count
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i) ?: continue
                count += hideAllIconViews(child)
            }
        }
        return count
    }

    
    private fun installSplashViewAttachedHook(cl: ClassLoader) {
        try {
            val cls = cl.loadClass("android.window.SplashScreenView")
            // SplashScreenView 自己不一定 override onAttachedToWindow（ColorOS 上就没有），
            // 所以沿父类链往上找；但**绝不勾到 android.view.View**，否则 systemui 里
            // 每一个 View 的 attach 都会进 hook，开销不可接受。
            val methods = mutableListOf<java.lang.reflect.Method>()
            var cursor: Class<*>? = cls
            while (cursor != null && cursor.name != "android.view.View") {
                methods.addAll(
                    cursor.declaredMethods.filter {
                        it.name == "onAttachedToWindow" && it.parameterCount == 0
                    }
                )
                cursor = cursor.superclass
            }
            if (methods.isEmpty()) {
                log(Log.WARN, TAG, "event=install_hook result=skip code=CSE-SIG-001 id=splash_view_attached reason=method_not_found note=fallback_to_build_hooks")
                return
            }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("splash_view_attached")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val result = chain.proceed()
                        if (!FORCE_NATIVE) return@intercept result
                        // 双保险：即便父类链回退勾到了中间类，也只在真 SplashScreenView 上装饰
                        if (!isSplashScreenView(chain.thisObject)) return@intercept result

                        applyIconDecor(cl, chain.thisObject, source = "attached")
                        result
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=splash_view_attached " +
                    "target=android.window.SplashScreenView#onAttachedToWindow " +
                    "count=${methods.size} gate=live"
            )
        } catch (t: Throwable) {
            logHookFailure("splash_view_attached", t)
        }
    }

    
    private fun drawIconRoundCorner(
        cl: ClassLoader,
        splashScreenView: Any?
    ) {
        if (splashScreenView !is FrameLayout) {
            log(Log.WARN, TAG, "event=hook_error id=draw_icon_round_corner reason=view_not_framelayout")
            return
        }
        val appContext = splashScreenView.context ?: hostAppContext()
        if (appContext == null) {
            log(Log.WARN, TAG, "event=hook_error id=draw_icon_round_corner reason=context_null")
            return
        }

        runCatching {
            val iconView = splashScreenView.javaClass.getDeclaredField("mIconView")
                .apply { isAccessible = true }
                .get(splashScreenView) as? ImageView ?: return

            val iconSize = appIconSize(cl)
            if (iconSize <= 0) return

            // 动态图标（IconAnimateListener 接口）不绘制圆角
            val drawable = iconView.drawable
            if (drawable != null &&
                drawable.javaClass.interfaces.any { it.name.contains("IconAnimateListener") }
            ) {
                return
            }

            val cornerRate = iconRoundCornerRate / 100f
            iconView.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val border = dp2px(appContext, 1.5f)
                    outline.setRoundRect(
                        border, border, view.width - border, view.height - border,
                        iconSize.toFloat() * cornerRate
                    )
                }
            }
            iconView.clipToOutline = true
            log(Log.INFO, TAG, "event=hook_hit id=draw_icon_round_corner iconSize=$iconSize rate=$cornerRate")
        }.onFailure {
            log(Log.WARN, TAG, "event=hook_error id=draw_icon_round_corner", it)
        }
    }

    /**
     * 缩小图标后，在图标背后叠一层放大的模糊图标当背景
     * （对应参考实现 IconHookHandler 里 build_SplashScreenViewBuilder 的 addAfterHook）。
     */
    private fun addIconBlurBackground(
        cl: ClassLoader,
        splashScreenView: Any?
    ) {
        if (splashScreenView !is FrameLayout) {
            log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=view_not_framelayout")
            return
        }
        val appContext = splashScreenView.context ?: hostAppContext()
        if (appContext == null) {
            log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=context_null")
            return
        }
        log(
            Log.INFO, TAG,
            "event=hook_enter id=add_icon_blur_bg childCount=${splashScreenView.childCount} " +
                "iconDrawable=${currentIconDrawable != null}"
        )

        runCatching {
            // 取 mIconView：优先反射字段，失败则遍历子 View 兜底（部分 ROM 字段名不同）
            var iconView = runCatching {
                splashScreenView.javaClass.getDeclaredField("mIconView")
                    .apply { isAccessible = true }
                    .get(splashScreenView) as? ImageView
            }.getOrNull()

            if (iconView == null) {
                for (i in 0 until splashScreenView.childCount) {
                    val child = splashScreenView.getChildAt(i)
                    if (child is ImageView) {
                        iconView = child
                        break
                    }
                }
            }
            if (iconView == null) {
                log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=icon_view_not_found")
                return
            }
            //   当前实际显示的 drawable（两条路都拿不到才放弃）。
            val drawable = currentIconDrawable ?: iconView.drawable
            if (drawable == null) {
                log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=icon_drawable_null")
                return
            }
            if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=drawable_size_zero")
                return
            }

            // 图标尺寸：优先系统资源；拿不到（部分 ROM 资源名不同）时按 108dp 兜底，
            // 保证模糊层不会因为 size=0 而静默跳过。
            var rawIconSize = appIconSize(cl)
            if (rawIconSize <= 0) rawIconSize = dp2px(appContext, 108f)
            val iconSize = (rawIconSize / 1.5).toInt()
            if (iconSize <= 0) {
                log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=icon_size_zero")
                return
            }
            //   这也是"加了却像没加"的原因之一）。
            val bgIconSize = (iconSize * 2 * (BLUR_BG_SCALE.coerceIn(50, 300) / 100f))
                .toInt().coerceAtLeast(1)
            val blurRadius = bgIconSize.toFloat() / 8

            val blurBgDrawable = GraphicUtils.createShadowedIcon(
                appContext,
                drawable,
                iconSize,
                bgIconSize,
                iconSize * iconRoundCornerRate / 100f
            )
            if (blurBgDrawable == null) {
                log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=create_shadowed_icon_null")
                return
            }

            val iconBlurBGView = ImageView(appContext).apply {
                setImageDrawable(blurBgDrawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
                alpha = 0.85f
                tag = TAG_BLUR_BG
                setRenderEffect(
                    RenderEffect.createBlurEffect(
                        blurRadius,
                        blurRadius,
                        Shader.TileMode.DECAL
                    )
                )
                // 放到图标下面：既用 z 也用 elevation，确保不同 ROM 的绘制顺序都正确
                z = -1f
            }

            splashScreenView.addView(
                iconBlurBGView,
                0,
                FrameLayout.LayoutParams(bgIconSize, bgIconSize).apply { gravity = Gravity.CENTER }
            )
            alignToIconCenter(
                splashScreenView, iconBlurBGView, iconView, bgIconSize, what = "blur"
            )
            iconView.alpha = 0.9f

            log(
                Log.INFO, TAG,
                "event=hook_hit id=add_icon_blur_bg iconSize=$iconSize bgIconSize=$bgIconSize " +
                    "childCount=${splashScreenView.childCount}"
            )

            //   导致刚加进去的模糊层被移除。下一帧再检查一次，丢了就补回来。
            splashScreenView.post {
                runCatching {
                    var exists = false
                    for (i in 0 until splashScreenView.childCount) {
                        if (splashScreenView.getChildAt(i)?.tag == TAG_BLUR_BG) {
                            exists = true
                            break
                        }
                    }
                    if (!exists) {
                        splashScreenView.addView(
                            iconBlurBGView,
                            0,
                            FrameLayout.LayoutParams(bgIconSize, bgIconSize).apply {
                                gravity = Gravity.CENTER
                            }
                        )
                        alignToIconCenter(
                            splashScreenView, iconBlurBGView, iconView, bgIconSize, what = "blur_re"
                        )
                        log(
                            Log.INFO, TAG,
                            "event=hook_hit id=add_icon_blur_bg decision=re_add_after_build"
                        )
                    }
                }.onFailure {
                    log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg reason=re_add", it)
                }
            }
        }.onFailure {
            log(Log.WARN, TAG, "event=hook_error id=add_icon_blur_bg", it)
        }
    }

    
    private fun installCacheTmpAttrsHook(cl: ClassLoader) {
        val appContext = hostAppContext()
        try {
            val cls = cl.loadClass(CLS_CONTENT_DRAWER)
            val methods = cls.declaredMethods.filter {
                it.name == CLS_GET_BG_COLOR_FROM_CACHE && it.parameterCount == 2
            }
            if (methods.isEmpty()) {
                // 宽松匹配：找不到精确签名就按名字匹配
                val loose = cls.declaredMethods.filter { it.name == CLS_GET_BG_COLOR_FROM_CACHE }
                if (loose.isEmpty()) throw NoSuchMethodException("$CLS_CONTENT_DRAWER#getBGColorFromCache")
                installCacheTmpAttrsLoose(cl, cls, loose, appContext)
                return
            }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("drawer_get_bg_color_from_cache")
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val self = chain.thisObject
                        // 缓存 mTmpAttrs（供"从图标取色"fallback）
                        runCatching {
                            val fTmpAttrs = self.javaClass.getDeclaredField("mTmpAttrs")
                                .apply { isAccessible = true }
                            mTmpAttrsInstance = fTmpAttrs.get(self)
                        }
                        // 取原始背景色，再按配置决定是否替换
                        val raw = chain.proceed() as? Int
                        if (FORCE_NATIVE && CHANGE_BG_COLOR_TYPE != BG_TYPE_NONE) {
                            getBackgroundColor(cl, appContext)?.let { color ->
                                log(
                                    Log.INFO, TAG,
                                    "event=hook_hit id=drawer_get_bg_color_from_cache " +
                                        "original=$raw decision=replace_bg -> $color"
                                )
                                return@intercept color
                            }
                        }
                        raw
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=drawer_get_bg_color_from_cache " +
                    "target=SplashscreenContentDrawer#getBGColorFromCache count=${methods.size}"
            )
        } catch (t: Throwable) {
            logHookFailure("drawer_get_bg_color_from_cache", t)
        }
    }

    private fun installCacheTmpAttrsLoose(
        cl: ClassLoader,
        cls: Class<*>,
        methods: List<Method>,
        appContext: android.content.Context?
    ) {
        methods.forEach { m ->
            m.isAccessible = true
            hook(m)
                .setId("drawer_get_bg_color_from_cache")
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                .intercept { chain ->
                    refreshConfigSilently()
                    val self = chain.thisObject
                    runCatching {
                        val fTmpAttrs = self.javaClass.getDeclaredField("mTmpAttrs")
                            .apply { isAccessible = true }
                        mTmpAttrsInstance = fTmpAttrs.get(self)
                    }
                    val raw = chain.proceed() as? Int
                    if (FORCE_NATIVE && CHANGE_BG_COLOR_TYPE != BG_TYPE_NONE) {
                        getBackgroundColor(cl, appContext)?.let { color ->
                            log(
                                Log.INFO, TAG,
                                "event=hook_hit id=drawer_get_bg_color_from_cache " +
                                    "original=$raw decision=replace_bg -> $color"
                            )
                            return@intercept color
                        }
                    }
                    raw
                }
        }
        log(
            Log.INFO, TAG,
            "event=install_hook result=ok id=drawer_get_bg_color_from_cache " +
                "target=SplashscreenContentDrawer#getBGColorFromCache count=${methods.size} (loose)"
        )
    }

    /**
     * 背景色替换已在 C 层 builder_build 里直接写 mThemeColor 完成（对齐真实 smali），
     * 这里不再需要单独的 createIconDrawable / setBackgroundColor hook。
     */

    
    private fun getBackgroundColor(
        cl: ClassLoader,
        appContext: android.content.Context?,
        iconDrawable: Drawable? = null
    ): Int? {
        if (appContext == null) return null
        val dark = isDarkMode(appContext)

        return when (CHANGE_BG_COLOR_TYPE) {
            //   该功能在 ColorOS 上取色结果不稳定，UI 层已删除该选项。
            //   老用户若配置里仍是 1，这里直接返回 null（不替换），避免走失效逻辑。
            BG_TYPE_FROM_ICON -> null
            // 莫奈取色
            //
            //   改为取莫奈的**背景颜色** `background`
            //   （对齐参照项目 RestoreSplashScreen 自身 App 界面所用的动态背景色
            //    MiuixTheme.colorScheme.background，即 dynamicXxxColorScheme().background）。
            BG_TYPE_FROM_MONET -> {
                runCatching {
                    val color = when (BG_COLOR_MODE) {
                        BG_MODE_LIGHT -> dynamicLightColorScheme(appContext).background.toArgb()
                        BG_MODE_DARK -> dynamicDarkColorScheme(appContext).background.toArgb()
                        else -> if (!dark)
                            dynamicLightColorScheme(appContext).background.toArgb()
                        else
                            dynamicDarkColorScheme(appContext).background.toArgb()
                    }
                    log(
                        Log.INFO, TAG,
                        "event=bg_color source=monet mode=$BG_COLOR_MODE dark=$dark " +
                            "color=#${Integer.toHexString(color)}"
                    )
                    color
                }.onFailure {
                    log(Log.WARN, TAG, "event=bg_color source=monet result=fail", it)
                }.getOrNull()
            }
            // 自定义颜色
            BG_TYPE_FROM_CUSTOM -> {
                runCatching {
                    Color.parseColor(if (dark) CUSTOM_BG_COLOR_NIGHT else CUSTOM_BG_COLOR)
                }.getOrNull()
            }
            else -> null
        }
    }

    // ---- 图标/背景 Hook 的辅助方法 ----

    /**
     * 每一次 Splash Screen 生命周期开始时清掉上一轮遗留的运行时状态。
     *
     * 参考实现是在 removeStartingWindow() 里 resetCache()；
     * 本模块把它提前到 makeSplashScreenContentView（A 层）的 before 阶段，
     * 与"取图标 → 缩小 → 圆角 → 背景"的调用顺序天然吻合。
     */
    private fun resetSplashState() {
        currentIconDominantColor = null
        currentIconDrawable = null
        //   否则连续启动应用会在 SystemUI 进程里堆积位图（内存压力 → 卡顿）。
        currentIconBitmap?.let { runCatching { it.recycle() } }
        currentIconBitmap = null
        currentFinalIconAccentColor = null
        currentIsNeedShrinkIcon = false
        iconShrinkApplied = false
        iconReplaced = false
        iconStateDecided = false
        iconDecorApplied = false
        mTmpAttrsInstance = null
    }

    /**
     * 从 makeSplashScreenContentView 的参数里提取当前启动的应用包名。
     *
     * 签名：makeSplashScreenContentView(Context, StartingWindowInfo, int, Consumer)
     * 第 1 个参数（index 1）是 StartingWindowInfo，其 targetActivityInfo 里带 packageName。
     */
    private fun extractCurrentPackageName(args: Array<Any?>) {
        runCatching {
            val info = args.getOrNull(1) ?: return
            val activityInfo: ActivityInfo? = when (info) {
                is ActivityInfo -> info
                else -> {
                    val fTarget = info.javaClass.getDeclaredField("targetActivityInfo")
                        .apply { isAccessible = true }
                    fTarget.get(info) as? ActivityInfo
                }
            }
            if (activityInfo != null) {
                currentPackageName = activityInfo.packageName ?: ""
                if (currentPackageName.isNotEmpty()) {
                    log(Log.INFO, TAG, "event=package_info package=$currentPackageName")
                }
            }
        }.onFailure {
            log(Log.WARN, TAG, "event=package_info result=fail reason=extract", it)
        }
    }

    /**
     * 获取宿主 App 的 Context（用于 packageManager / 动态取色）。
     *
     * 注意：`android.app.ActivityThread` 是 Android 的 hidden API（@hide），
     * 不在公开的 android.jar 里，**编译期无法直接引用**，必须用反射调用：
     *   ActivityThread.currentApplication()
     * 这是 Xposed 模块获取宿主 Context 的标准做法。
     */
    private fun hostAppContext(): android.content.Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val method = activityThread.getDeclaredMethod("currentApplication")
        method.isAccessible = true
        method.invoke(null) as? android.content.Context
    }.getOrNull()

    /** 判断当前系统是否深色模式。 */
    private fun isDarkMode(context: android.content.Context): Boolean =
        (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    /** 获取 SplashScreen 图标的标准尺寸（starting_surface_icon_size）。 */
    private fun appIconSize(cl: ClassLoader): Int = runCatching {
        val appResources = hostAppContext()?.resources ?: return 0
        val resId = appResources.getIdentifier(
            "starting_surface_icon_size", "dimen", "android"
        )
        if (resId != 0) appResources.getDimensionPixelSize(resId) else 0
    }.getOrDefault(0)

    /** dp 转 px（用于圆角 outline 的 border 计算）。 */
    private fun dp2px(context: android.content.Context, dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()

    // ==================== E2. 粒子退出动画（替换原生 ripple 退出动画）
    //
    //   SplashScreenExitAnimationUtils 有两个 startAnimations 重载，**均返回 void**：
    //     startAnimations(int, ViewGroup, ...)  ← 14 参，唯一汇合点（12 参版内部转调它）
    //   14 参版：p0=animationType，p1=ViewGroup(splashScreenView)，p12=AnimatorListener。
    //   方法体只是在 `type == 1 ? createFadeOutAnimation : createRadialVanishSlideUpAnimator`
    //   之后 `animator.start()`。
    //
    //   传入的 AnimatorListener 其实就是 SplashScreenExitAnimation 自己，它的
    //   onAnimationEnd 只做 `reset()`（隐藏 splash + 触发移除窗口的回调），
    //   **完全不用它的 Animator 参数**。于是可以：
    //     1) 不创建 / 不启动原生动画；
    //     2) 自己播粒子；
    //     3) 粒子播完手动调一次 listener.onAnimationEnd(null) —— splash 正常消失。
    //   返回 null 是安全的（原方法返回 void）。
    //
    //   优先级取 PRIORITY_HIGHEST：必须**先于** E 层的 ripple 强制钩子执行，
    //   这样粒子模式下 E 层根本不会被调用（我们直接 return null 不 proceed）。

    private fun installExitParticleHook(cl: ClassLoader) {
        try {
            val cls = cl.loadClass(CLS_EXIT_ANIM_UTILS)
            val methods = cls.declaredMethods.filter { m ->
                m.name == "startAnimations" &&
                    m.parameterCount >= 2 &&
                    m.parameterTypes[0] == Int::class.javaPrimitiveType
            }
            if (methods.isEmpty()) {
                throw NoSuchMethodException("$CLS_EXIT_ANIM_UTILS#startAnimations(int, ...)")
            }

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId("exit_particle")
                    .setPriority(XposedInterface.PRIORITY_HIGHEST)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        if (FORCE_NATIVE && EXIT_ANIM_MODE != EXIT_MODE_DEFAULT) {
                            val args = chain.args
                            val host = args.getOrNull(1) as? ViewGroup
                            if (host != null) {
                                val listener = args.getOrNull(12)
                                    as? android.animation.Animator.AnimatorListener
                                if (playParticleExit(host, listener)) {
                                    // 已接管：不调用原生动画（原方法返回 void，null 安全）
                                    return@intercept null
                                }
                            }
                        }
                        chain.proceed()
                    }
            }
            log(
                Log.INFO, TAG,
                "event=install_hook result=ok id=exit_particle " +
                    "target=SplashScreenExitAnimationUtils#startAnimations " +
                    "count=${methods.size} priority=HIGHEST"
            )
        } catch (t: Throwable) {
            logHookFailure("exit_particle", t)
        }
    }

    /**
     * 在 [host]（SplashScreenView）上播放粒子消散，并负责让 splash 正常结束。
     *
     * @return true 表示已接管（调用方应跳过原生动画）；false 表示未接管（走原生）。
     */
    private fun playParticleExit(
        host: ViewGroup,
        listener: android.animation.Animator.AnimatorListener?
    ): Boolean {
        return try {
            val context = host.context ?: return false
            val w = host.width
            val h = host.height
            if (w <= 0 || h <= 0) {
                log(Log.WARN, TAG, "event=hook_error id=exit_particle reason=view_size_zero ${w}x$h")
                return false
            }

            // 1) 把当前屏幕内容截下来（1/CAPTURE_SCALE）：既作铺底原图，也供粒子采样。
            val bw = (w / CAPTURE_SCALE).coerceAtLeast(1)
            val bh = (h / CAPTURE_SCALE).coerceAtLeast(1)
            val source = android.graphics.Bitmap.createBitmap(
                bw, bh, android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(source)
            canvas.scale(1f / CAPTURE_SCALE, 1f / CAPTURE_SCALE, 0f, 0f)
            host.draw(canvas)
            canvas.setBitmap(null)

            val durationMs = EXIT_PARTICLE_DURATION_MS
                .coerceIn(EXIT_DURATION_MIN, EXIT_DURATION_MAX)
            // 原生 AnimatorListener 的 onAnimation* 参数类型是**非空** Animator（不能传 null），
            // 而其实现（SplashScreenExitAnimation）根本不用这个参数 —— 传一个未启动的
            // 占位 Animator 即可，调用它只为触发 reset()（隐藏 splash + 移除窗口）。
            val placeholderAnimator = android.animation.ValueAnimator()
            val finish: () -> Unit = {
                // 走系统原本的结束回调：reset() 会隐藏 splash 并触发窗口移除。
                runCatching { listener?.onAnimationStart(placeholderAnimator) }
                runCatching { listener?.onAnimationEnd(placeholderAnimator) }
                Unit
            }

            // source 交给 SplashParticleView（消散模式当底图 + 采样颜色），
            // 由视图在 detach 时自行回收。这里不要再回收。
            val particleView = SplashParticleView(
                context, source, CAPTURE_SCALE, durationMs,
                EXIT_ANIM_MODE == EXIT_MODE_DISSOLVE, PARTICLE_TIME_MS, finish
            )

            // 2) 隐藏原内容 + 去掉背景色，屏幕上只剩粒子。
            runCatching {
                host.background = null
                for (i in 0 until host.childCount) {
                    host.getChildAt(i)?.visibility = View.INVISIBLE
                }
            }

            host.addView(
                particleView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            particleView.start()

            log(
                Log.INFO, TAG,
                "event=hook_hit id=exit_particle decision=play_particle " +
                    "duration=$durationMs size=${w}x$h sample=${bw}x$bh"
            )
            true
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "event=hook_error id=exit_particle reason=setup", t)
            false
        }
    }

    // ============================================================ Hook 辅助工具

    /**
     * 解析 SplashViewBuilder / StartingWindowViewBuilder。
     *
     * Android 14 及以前叫 `SplashscreenContentDrawer$SplashViewBuilder`，
     * Android 15 起 AOSP 改名为 `SplashscreenContentDrawer$StartingWindowViewBuilder`
     * （参考项目 RestoreSplashScreen 也是这样二选一 fallback）。
     *
     * 只认其中一个名字，会导致定型层（C）与缩小图标（G）在新系统上静默失效。
     */
    private fun loadSplashViewBuilderClass(cl: ClassLoader): Class<*> =
        runCatching { cl.loadClass(CLS_SPLASH_VIEW_BUILDER) }.getOrNull()
            ?: runCatching { cl.loadClass(CLS_STARTING_WINDOW_VIEW_BUILDER) }.getOrNull()
            ?: throw ClassNotFoundException(
                "$CLS_SPLASH_VIEW_BUILDER / $CLS_STARTING_WINDOW_VIEW_BUILDER"
            )

    /**
     * Hook 一个「返回 int」的方法，按 name 宽松匹配（不写死参数签名，避免 ROM 改签名后失效）。
     * 若目标类有多个同名重载，全部挂钩。
     */
    private inline fun hookIntReturning(
        cl: ClassLoader,
        className: String,
        methodName: String,
        id: String,
        label: String,
        crossinline transform: (Int) -> Int
    ) {
        try {
            val cls = cl.loadClass(className)
            val methods = cls.declaredMethods.filter {
                it.name == methodName && it.returnType == Int::class.javaPrimitiveType
            }
            if (methods.isEmpty()) throw NoSuchMethodException("$className#$methodName")

            methods.forEach { m ->
                m.isAccessible = true
                hook(m)
                    .setId(id)
                    .setPriority(XposedInterface.PRIORITY_DEFAULT)
                    .setExceptionMode(XposedInterface.ExceptionMode.DEFAULT)
                    .intercept { chain ->
                        refreshConfigSilently()
                        val raw = chain.proceed()
                        val original = raw as? Int ?: return@intercept raw
                        if (!FORCE_NATIVE) return@intercept raw
                        val mapped = transform(original)
                        if (mapped != original) {
                            log(Log.INFO, TAG, "event=hook_hit id=$id label=$label original=$original decision=force_native -> $mapped")
                        }
                        mapped
                    }
            }
            log(Log.INFO, TAG, "event=install_hook result=ok id=$id target=$label count=${methods.size}")
        } catch (t: Throwable) {
            logHookFailure(id, t)
        }
    }

    private inline fun installStage(stage: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log(Log.ERROR, TAG, "event=install_hook result=fail stage=$stage", t)
        }
    }

    private fun logHookFailure(id: String, t: Throwable) {
        val code = when (t) {
            is ClassNotFoundException -> "CSE-CL-001/class_not_found"
            is NoSuchMethodException -> "CSE-SIG-001/method_not_found"
            is NoSuchFieldException -> "CSE-FLD-001/field_not_found"
            else -> "CSE-HOOK-001/${t.javaClass.simpleName}"
        }
        log(Log.WARN, TAG, "event=install_hook result=skip code=$code id=$id", t)
    }

}

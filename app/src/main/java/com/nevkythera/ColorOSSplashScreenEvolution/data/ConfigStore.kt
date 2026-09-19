package com.Nevkythera.ColorOSSplashScreenEvolution.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CSE 配置模型。
 *
 * 本次大改（总开关化）：
 *  - [masterSwitch]   -> 总开关（原 forceNative）。所有功能 Hook 前必须先验证它为 true。
 *  - [disablePreview] -> B/C/D 层：关闭 ColorOS 的 preview 截图 / XML 图覆盖（已移到图标页）。
 *  - ripple 不再独立成开关，跟随 [masterSwitch] 一并生效（E 层 Hook 由总开关控制）。
 *
 * 图标页：
 *  - [drawRoundCorner] -> 绘制 Splash Screen 图标圆角。
 *  - [shrinkIcon]      -> 缩小图标（0=不缩小 / 2=全部；值 1 已废弃）。
 *  - [replaceIcon]     -> 替换图标获取方式（改用 PackageManager）。
 *
 * 背景页：
 *  - [changeBgColorType] -> 0=不替换 / 1=从图标取色 / 2=莫奈取色 / 3=自定义颜色。
 *  - [bgColorMode]       -> 0=浅色 / 1=暗色 / 2=跟随系统。
 *  - [customBgColor]     -> 自定义背景颜色（浅色，ARGB 十六进制字符串）。
 *  - [customBgColorNight]-> 自定义背景颜色（暗色）。
 *
 * 界面开关：
 *  - [showLauncherIcon] -> 是否在桌面显示应用图标。
 */
data class CseConfig(
    val masterSwitch: Boolean = true,
    val disablePreview: Boolean = true,
    val drawRoundCorner: Boolean = false,
    
    val shrinkIcon: Int = SHRINK_NONE,
    val replaceIcon: Boolean = false,
    /** 单个粒子自身的运动时长（毫秒，100~2000，默认 600）。 */
    val particleTimeMs: Int = PARTICLE_TIME_DEFAULT,
    /**
     * MD3 Expressive 几何形变加载动画：图标背后放一个持续形变 + 旋转的几何色块。
     * 开启后**跳过静态模糊背景**（两者都贴在图标背后，同时开会糊成一团）。
     */
    val enableMorphShape: Boolean = false,
    /** 指示器大小：相对基准的倍率百分比（50~300，默认 100）。 */
    val morphShapeScale: Int = 100,
    /** 指示器取色：0 = 莫奈取色，1 = 跟随应用图标取色。 */
    val morphShapeColorType: Int = 0,
    /** 移除图标：强制隐藏 SplashScreen 上的所有图标（与「关闭截图覆盖」相互独立）。 */
    val removeIcon: Boolean = false,
    val changeBgColorType: Int = 0,
    val bgColorMode: Int = 2,
    val customBgColor: String = "#FFFFFF",
    val customBgColorNight: String = "#000000",
    val showLauncherIcon: Boolean = true,
    /** 杂项：热启动也适用启动遮罩（Hook 系统框架 ActivityRecord）。 */
    val enableHotStartSplash: Boolean = false,

    /**
     * 退出动画效果：0 = 默认（Android 原生 ripple，不做任何修改）/ 1 = 粒子消失。
     * 见 [EXIT_MODE_DEFAULT] / [EXIT_MODE_PARTICLE]。
     */
    val exitAnimMode: Int = EXIT_MODE_DEFAULT,

    /** 粒子消失动画总时长（毫秒，[EXIT_DURATION_MIN]~[EXIT_DURATION_MAX]，默认 600）。 */
    val exitParticleDurationMs: Int = EXIT_DURATION_DEFAULT
) {
    companion object {
        const val PREFS_NAME = "cse_config"

        // ── 退出动画模式 ──
        /** 默认：不改动，保持 Android 自带的 ripple 退出动画。 */
        const val EXIT_MODE_DEFAULT = 0
        /** 粒子扩散：整屏粒子**同时**飞散（移动幅度更大）。 */
        const val EXIT_MODE_DIFFUSE = 1
        /** 粒子消散：整屏粒子从左到右**渐进**消失（Telegram 同款）。 */
        const val EXIT_MODE_DISSOLVE = 2

        /** 粒子动画时长范围与默认值（毫秒）。用户可调 200~2000。 */
        const val EXIT_DURATION_MIN = 200
        const val EXIT_DURATION_MAX = 2000
        const val EXIT_DURATION_DEFAULT = 600

        /** 单个粒子的运动时长：范围与默认值（毫秒）。 */
        const val PARTICLE_TIME_MIN = 100
        const val PARTICLE_TIME_MAX = 2000
        const val PARTICLE_TIME_DEFAULT = 600

        fun normalizeParticleTime(raw: Int): Int =
            raw.coerceIn(PARTICLE_TIME_MIN, PARTICLE_TIME_MAX)

        /** 归一化退出动画模式（消化可能的非法值）。 */
        fun normalizeExitMode(raw: Int): Int = when (raw) {
            EXIT_MODE_DIFFUSE, EXIT_MODE_DISSOLVE -> raw
            else -> EXIT_MODE_DEFAULT
        }

        /** 归一化粒子时长到合法区间。 */
        fun normalizeExitDuration(raw: Int): Int =
            raw.coerceIn(EXIT_DURATION_MIN, EXIT_DURATION_MAX)

        // ── 缩小图标：两态常量 ──
        /** 不缩小。 */
        const val SHRINK_NONE = 0
        /** 缩小全部图标。（值 1 = 已移除的「仅缩小低分辨率」，属无效值） */
        const val SHRINK_ALL = 2

        /**
         * 把任意输入归一化为合法状态（0 或 2）。
         *
         * 为什么需要它：老用户配置里可能存着已移除的值 `1`，
         * 若不归一化，UI 的 `selectedIndex` 会越界（下拉列表只有 2 项），
         * 且远程配置日志里会出现无意义的 shrink_icon=1。
         */
        fun normalizeShrinkIcon(raw: Int): Int =
            if (raw == SHRINK_ALL) SHRINK_ALL else SHRINK_NONE

        // 总开关沿用旧 key，保证老用户升级后配置不丢
        const val KEY_MASTER_SWITCH = "force_native"
        const val KEY_DISABLE_PREVIEW = "disable_preview"
        const val KEY_DRAW_ROUND_CORNER = "draw_round_corner"
        const val KEY_SHRINK_ICON = "shrink_icon"
        const val KEY_REPLACE_ICON = "replace_icon"
        const val KEY_PARTICLE_TIME_MS = "exit_particle_time_ms"
        const val KEY_ENABLE_MORPH_SHAPE = "enable_morph_shape"
        const val KEY_MORPH_SHAPE_SCALE = "morph_shape_scale"
        const val KEY_MORPH_SHAPE_COLOR_TYPE = "morph_shape_color_type"
        const val KEY_REMOVE_ICON = "remove_icon"
        const val KEY_CHANGE_BG_COLOR_TYPE = "change_bg_color_type"
        const val KEY_BG_COLOR_MODE = "bg_color_mode"
        const val KEY_CUSTOM_BG_COLOR = "custom_bg_color"
        const val KEY_CUSTOM_BG_COLOR_NIGHT = "custom_bg_color_night"
        const val KEY_SHOW_LAUNCHER_ICON = "show_launcher_icon"
        const val KEY_ENABLE_HOT_START_SPLASH = "enable_hot_start_splash"
        const val KEY_EXIT_ANIM_MODE = "exit_anim_mode"
        const val KEY_EXIT_PARTICLE_DURATION = "exit_particle_duration_ms"
    }
}

/**
 * 配置存储（App 侧门面）。
 *
 * 读写全部经由 [XposedRepo]：
 *  - 本地 SharedPreferences 保证模块未激活时界面依然可用；
 *  - 模块激活后同一份配置会同步写进 LSPosed 的远程偏好，
 *    Hook 进程即可实时读到。
 */
class ConfigStore(context: Context) {

    private val repo = XposedRepo.getInstance(context)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<CseConfig> = _state.asStateFlow()

    fun read(): CseConfig = CseConfig(
        masterSwitch = repo.getBoolean(CseConfig.KEY_MASTER_SWITCH, true),
        disablePreview = repo.getBoolean(CseConfig.KEY_DISABLE_PREVIEW, true),
        drawRoundCorner = repo.getBoolean(CseConfig.KEY_DRAW_ROUND_CORNER, false),
        shrinkIcon = CseConfig.normalizeShrinkIcon(repo.getInt(CseConfig.KEY_SHRINK_ICON, 0)),
        replaceIcon = repo.getBoolean(CseConfig.KEY_REPLACE_ICON, false),
        particleTimeMs = CseConfig.normalizeParticleTime(
            repo.getInt(CseConfig.KEY_PARTICLE_TIME_MS, CseConfig.PARTICLE_TIME_DEFAULT)
        ),
        enableMorphShape = repo.getBoolean(CseConfig.KEY_ENABLE_MORPH_SHAPE, false),
        morphShapeScale = repo.getInt(CseConfig.KEY_MORPH_SHAPE_SCALE, 100),
        morphShapeColorType = repo.getInt(CseConfig.KEY_MORPH_SHAPE_COLOR_TYPE, 0),
        removeIcon = repo.getBoolean(CseConfig.KEY_REMOVE_ICON, false),
        changeBgColorType = repo.getInt(CseConfig.KEY_CHANGE_BG_COLOR_TYPE, 0),
        bgColorMode = repo.getInt(CseConfig.KEY_BG_COLOR_MODE, 2),
        customBgColor = repo.getString(CseConfig.KEY_CUSTOM_BG_COLOR, "#FFFFFF"),
        customBgColorNight = repo.getString(CseConfig.KEY_CUSTOM_BG_COLOR_NIGHT, "#000000"),
        showLauncherIcon = repo.getBoolean(CseConfig.KEY_SHOW_LAUNCHER_ICON, true),
        enableHotStartSplash = repo.getBoolean(CseConfig.KEY_ENABLE_HOT_START_SPLASH, false),
        exitAnimMode = CseConfig.normalizeExitMode(
            repo.getInt(CseConfig.KEY_EXIT_ANIM_MODE, CseConfig.EXIT_MODE_DEFAULT)
        ),
        exitParticleDurationMs = CseConfig.normalizeExitDuration(
            repo.getInt(CseConfig.KEY_EXIT_PARTICLE_DURATION, CseConfig.EXIT_DURATION_DEFAULT)
        )
    )

    fun setMasterSwitch(value: Boolean) = update(CseConfig.KEY_MASTER_SWITCH, value)
    fun setDisablePreview(value: Boolean) = update(CseConfig.KEY_DISABLE_PREVIEW, value)
    fun setDrawRoundCorner(value: Boolean) = update(CseConfig.KEY_DRAW_ROUND_CORNER, value)
    fun setShrinkIcon(value: Int) =
        update(CseConfig.KEY_SHRINK_ICON, CseConfig.normalizeShrinkIcon(value))
    fun setReplaceIcon(value: Boolean) = update(CseConfig.KEY_REPLACE_ICON, value)
    fun setParticleTimeMs(value: Int) =
        update(CseConfig.KEY_PARTICLE_TIME_MS, CseConfig.normalizeParticleTime(value))
    fun setEnableMorphShape(value: Boolean) = update(CseConfig.KEY_ENABLE_MORPH_SHAPE, value)
    fun setMorphShapeScale(value: Int) = update(CseConfig.KEY_MORPH_SHAPE_SCALE, value)
    fun setMorphShapeColorType(value: Int) =
        update(CseConfig.KEY_MORPH_SHAPE_COLOR_TYPE, value)
    fun setRemoveIcon(value: Boolean) = update(CseConfig.KEY_REMOVE_ICON, value)
    fun setChangeBgColorType(value: Int) = update(CseConfig.KEY_CHANGE_BG_COLOR_TYPE, value)
    fun setBgColorMode(value: Int) = update(CseConfig.KEY_BG_COLOR_MODE, value)
    fun setCustomBgColor(value: String) = update(CseConfig.KEY_CUSTOM_BG_COLOR, value)
    fun setCustomBgColorNight(value: String) = update(CseConfig.KEY_CUSTOM_BG_COLOR_NIGHT, value)
    fun setEnableHotStartSplash(value: Boolean) =
        update(CseConfig.KEY_ENABLE_HOT_START_SPLASH, value)

    fun setExitAnimMode(value: Int) =
        update(CseConfig.KEY_EXIT_ANIM_MODE, CseConfig.normalizeExitMode(value))

    fun setExitParticleDurationMs(value: Int) =
        update(CseConfig.KEY_EXIT_PARTICLE_DURATION, CseConfig.normalizeExitDuration(value))

    /**
     * 桌面图标开关属于纯本地状态（由 PackageManager 组件开关承担，
     * 无需同步到 Hook 进程）。
     */
    fun setShowLauncherIcon(value: Boolean) {
        repo.setBoolean(CseConfig.KEY_SHOW_LAUNCHER_ICON, value, syncRemote = false)
        _state.value = read()
    }

    private fun update(key: String, value: Boolean) {
        repo.setBoolean(key, value)
        _state.value = read()
    }

    private fun update(key: String, value: Int) {
        repo.setInt(key, value)
        _state.value = read()
    }

    private fun update(key: String, value: String) {
        repo.setString(key, value)
        _state.value = read()
    }
}

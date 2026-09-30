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
 *  - [iconScale]       -> 图标大小（百分比，100 = 原始大小）。
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
    
    val iconScale: Int = ICON_SCALE_DEFAULT,
    val replaceIcon: Boolean = false,
    /** 还原 AOSP 应用过渡动画（含小窗过渡）。 */
    val aospTransition: Boolean = false,

    /** 详细日志（写入 logcat，供排查；默认开启）。 */
    val detailedLog: Boolean = true,

    /** 自定义启动遮罩背景图片（内容 URI；空 = 未选）。 */
    val splashImageUri: String = "",
    /** 是否启用背景图片（开启后才有渲染）。 */
    val splashImageEnabled: Boolean = false,
    /** 图片不透明度（0~100，默认 100）。 */
    val splashImageAlpha: Int = 100,
    /** 背景媒体类型：image / gif / video。 */
    val splashMediaKind: String = "image",
    /** 背景媒体的归一化裁切变换（GIF/视频用；空 = 居中铺满）。 */
    val splashMediaTransform: String = "",
    /** 背景媒体版本号（换图/换视频时更新，供 SystemUI 侧判断缓存是否过期）。 */
    val splashMediaVersion: Long = 0L,

    /** 单个粒子自身的运动时长（毫秒，100~2000，默认 600）。 */
    val particleTimeMs: Int = PARTICLE_TIME_DEFAULT,
    /** 加载动画模式：0 = 不启用 / 1 = 几何图形 / 2 = 加载条。 */
    val loadingAnimMode: Int = LOADING_ANIM_NONE,
    /** 指示器大小：相对基准的倍率百分比（50~300，默认 100）。 */
    val morphShapeScale: Int = 100,
    /** 加载条长度（dp，[LOADING_BAR_LENGTH_MIN]~[LOADING_BAR_LENGTH_MAX]）。 */
    val loadingBarLength: Int = LOADING_BAR_LENGTH_DEFAULT,
    /** 加载条粗细（dp，[LOADING_BAR_THICKNESS_MIN]~[LOADING_BAR_THICKNESS_MAX]）。 */
    val loadingBarThickness: Int = LOADING_BAR_THICKNESS_DEFAULT,
    /** 指示器取色：0 = 莫奈取色，1 = 跟随应用图标取色。 */
    val morphShapeColorType: Int = 0,
    /** 图标水平位移（dp，默认 0 = 原位置）。 */
    val iconOffsetX: Int = 0,
    /** 图标垂直位移（dp）。 */
    val iconOffsetY: Int = 0,
    /** 加载动画指示器水平位移（dp，默认 0 = 原位置）。 */
    val indicatorOffsetX: Int = 0,
    /** 加载动画指示器垂直位移（dp）。 */
    val indicatorOffsetY: Int = 0,
    /** 移除图标：隐藏 SplashScreen 上的应用图标（与「移除底部图片」相互独立）。 */
    val removeIcon: Boolean = false,
    /** 移除底部图片：清掉应用自带的底部品牌图（与「移除图标」相互独立）。 */
    val removeBrandingImage: Boolean = false,
    /** 显示应用名称：在启动遮罩上显示被启动应用的名称。 */
    val showAppName: Boolean = false,
    /** 应用名称文本大小（sp）。 */
    val appNameTextSize: Int = APP_NAME_TEXT_SIZE_DEFAULT,
    /** 应用名称水平位移（dp，默认 0 = 中心正下方）。 */
    val appNameOffsetX: Int = 0,
    /** 应用名称垂直位移（dp）。 */
    val appNameOffsetY: Int = 0,
    /** 应用名称显示位置：0 = 图标正下方，1 = 底部。 */
    val appNamePosition: Int = APP_NAME_POS_BOTTOM,
    /** 应用名称取色：0 = 跟随图标，1 = 自定义颜色，2 = 跟随系统。 */
    val appNameColorType: Int = APP_NAME_COLOR_FROM_SYSTEM,
    /** 应用名称自定义颜色（ARGB 十六进制字符串）。 */
    val appNameCustomColor: String = "#FFFFFF",
    /** 应用名称字体粗细（100~900，默认 400）。 */
    val appNameFontWeight: Int = APP_NAME_FONT_WEIGHT_DEFAULT,
    /** 使用英语作为文本（应用支持英语时，否则用默认）。 */
    val appNameUseEnglish: Boolean = false,
    /** 自定义字体文件名（空 = 未选择）。 */
    val appNameFontName: String = "",
    /** 自定义字体版本（换字体时更新，供 SystemUI 判断缓存是否过期）。 */
    val appNameFontVersion: Long = 0L,
    val changeBgColorType: Int = 0,
    val bgColorMode: Int = 2,
    val customBgColor: String = "#FFFFFF",
    val customBgColorNight: String = "#000000",
    val showLauncherIcon: Boolean = true,
    /** 杂项：热启动也适用启动遮罩（Hook 系统框架 ActivityRecord）。 */
    val enableHotStartSplash: Boolean = false,

    /** 自定义最小遮罩显示时长：是否启用。 */
    val minSplashShowEnabled: Boolean = true,
    /** 自定义最小遮罩显示时长（ms）。 */
    val minSplashShowMs: Int = MIN_SPLASH_SHOW_MS_DEFAULT,

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

        /** 自定义最小遮罩显示时长（ms）。 */
        const val MIN_SPLASH_SHOW_MS_DEFAULT = 400
        const val MIN_SPLASH_SHOW_MS_MIN = 100
        const val MIN_SPLASH_SHOW_MS_MAX = 60000

        /** 输入超过它时弹警告（拖慢进入应用速度）。 */
        const val MIN_SPLASH_SHOW_WARN_ABOVE = 5000

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

        // ── 图标大小 ──
        const val ICON_SCALE_MIN = 50
        const val ICON_SCALE_MAX = 150
        const val ICON_SCALE_DEFAULT = 100

        // 总开关沿用旧 key，保证老用户升级后配置不丢
        const val KEY_MASTER_SWITCH = "force_native"
        const val KEY_DISABLE_PREVIEW = "disable_preview"
        const val KEY_DRAW_ROUND_CORNER = "draw_round_corner"
        const val KEY_SHRINK_ICON = "shrink_icon"
        const val KEY_ICON_SCALE = "icon_scale"
        const val KEY_REPLACE_ICON = "replace_icon"
        const val KEY_PARTICLE_TIME_MS = "exit_particle_time_ms"
        const val KEY_AOSP_TRANSITION = "aosp_transition"
        const val KEY_DETAILED_LOG = "detailed_log"
        const val KEY_SPLASH_IMAGE_URI = "splash_image_uri"
        const val KEY_SPLASH_IMAGE_ENABLED = "splash_image_enabled"
        const val KEY_SPLASH_IMAGE_ALPHA = "splash_image_alpha"
        const val KEY_SPLASH_MEDIA_KIND = "splash_media_kind"
        const val KEY_SPLASH_MEDIA_TRANSFORM = "splash_media_transform"
        const val KEY_SPLASH_MEDIA_VERSION = "splash_media_version"
        const val KEY_ENABLE_MORPH_SHAPE = "enable_morph_shape"
        const val KEY_LOADING_ANIM_MODE = "loading_anim_mode"
        const val KEY_MORPH_SHAPE_SCALE = "morph_shape_scale"
        const val KEY_LOADING_BAR_LENGTH = "loading_bar_length"
        const val KEY_LOADING_BAR_THICKNESS = "loading_bar_thickness"
        const val KEY_MORPH_SHAPE_COLOR_TYPE = "morph_shape_color_type"
        const val KEY_ICON_OFFSET_X = "icon_offset_x"
        const val KEY_ICON_OFFSET_Y = "icon_offset_y"
        const val KEY_INDICATOR_OFFSET_X = "indicator_offset_x"
        const val KEY_INDICATOR_OFFSET_Y = "indicator_offset_y"
        const val KEY_REMOVE_ICON = "remove_icon"
        const val KEY_REMOVE_BRANDING_IMAGE = "remove_branding_image"
        const val KEY_SHOW_APP_NAME = "show_app_name"
        const val KEY_APP_NAME_TEXT_SIZE = "app_name_text_size"
        const val KEY_APP_NAME_OFFSET_X = "app_name_offset_x"
        const val KEY_APP_NAME_OFFSET_Y = "app_name_offset_y"
        const val KEY_APP_NAME_COLOR_TYPE = "app_name_color_type"
        const val KEY_APP_NAME_CUSTOM_COLOR = "app_name_custom_color"
        const val KEY_APP_NAME_POSITION = "app_name_position"
        const val KEY_APP_NAME_FONT_WEIGHT = "app_name_font_weight"
        const val KEY_APP_NAME_USE_ENGLISH = "app_name_use_english"
        const val KEY_APP_NAME_FONT_NAME = "app_name_font_name"
        const val KEY_APP_NAME_FONT_VERSION = "app_name_font_version"
        const val KEY_CHANGE_BG_COLOR_TYPE = "change_bg_color_type"
        const val KEY_BG_COLOR_MODE = "bg_color_mode"
        const val KEY_CUSTOM_BG_COLOR = "custom_bg_color"
        const val KEY_CUSTOM_BG_COLOR_NIGHT = "custom_bg_color_night"
        const val KEY_SHOW_LAUNCHER_ICON = "show_launcher_icon"
        const val KEY_ENABLE_HOT_START_SPLASH = "enable_hot_start_splash"
        const val KEY_MIN_SPLASH_SHOW_ENABLED = "min_splash_show_enabled"
        const val KEY_MIN_SPLASH_SHOW_MS = "min_splash_show_ms"
        const val KEY_EXIT_ANIM_MODE = "exit_anim_mode"
        const val KEY_EXIT_PARTICLE_DURATION = "exit_particle_duration_ms"

        // ── 加载动画：模式 / 尺寸 / 位移 ──
        /** 不启用。 */
        const val LOADING_ANIM_NONE = 0
        /** MD3E 几何形变指示器。 */
        const val LOADING_ANIM_SHAPE = 1
        /** MD3 加载条。 */
        const val LOADING_ANIM_BAR = 2
        const val LOADING_BAR_LENGTH_MIN = 40
        const val LOADING_BAR_LENGTH_MAX = 480
        const val LOADING_BAR_LENGTH_DEFAULT = 160
        const val LOADING_BAR_THICKNESS_MIN = 2
        const val LOADING_BAR_THICKNESS_MAX = 24
        const val LOADING_BAR_THICKNESS_DEFAULT = 4
        /** 图标 / 指示器位移范围（dp）。 */
        const val OFFSET_MIN = -300
        const val OFFSET_MAX = 300

        // ── 应用名称 ──
        const val APP_NAME_TEXT_SIZE_MIN = 8
        const val APP_NAME_TEXT_SIZE_MAX = 48
        const val APP_NAME_TEXT_SIZE_DEFAULT = 14
        /** 取色：跟随应用图标。 */
        const val APP_NAME_COLOR_FROM_ICON = 0
        /** 取色：自定义颜色。 */
        const val APP_NAME_COLOR_CUSTOM = 1
        /** 取色：跟随系统。 */
        const val APP_NAME_COLOR_FROM_SYSTEM = 2

        // ── 应用名称显示位置 ──
        /** 图标正下方。 */
        const val APP_NAME_POS_BELOW_ICON = 0
        /** 底部（原品牌图所在位置）。 */
        const val APP_NAME_POS_BOTTOM = 1

        // ── 应用名称字体 ──
        const val APP_NAME_FONT_WEIGHT_MIN = 100
        const val APP_NAME_FONT_WEIGHT_MAX = 900
        const val APP_NAME_FONT_WEIGHT_DEFAULT = 400
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
        iconScale = run {
            // 老配置只有「缩小图标」两态开关；开启时近似映射成 67%（/1.5）。
            val legacy = repo.getInt(CseConfig.KEY_SHRINK_ICON, 0)
            val def = if (legacy == 2) 67 else CseConfig.ICON_SCALE_DEFAULT
            repo.getInt(CseConfig.KEY_ICON_SCALE, def)
                .coerceIn(CseConfig.ICON_SCALE_MIN, CseConfig.ICON_SCALE_MAX)
        },
        replaceIcon = repo.getBoolean(CseConfig.KEY_REPLACE_ICON, false),
        aospTransition = repo.getBoolean(CseConfig.KEY_AOSP_TRANSITION, false),
        detailedLog = repo.getBoolean(CseConfig.KEY_DETAILED_LOG, true),
        splashImageUri = repo.getString(CseConfig.KEY_SPLASH_IMAGE_URI, "") ?: "",
        splashImageEnabled = repo.getBoolean(CseConfig.KEY_SPLASH_IMAGE_ENABLED, false),
        splashImageAlpha = repo.getInt(CseConfig.KEY_SPLASH_IMAGE_ALPHA, 100).coerceIn(0, 100),
        splashMediaKind = repo.getString(CseConfig.KEY_SPLASH_MEDIA_KIND, "image") ?: "image",
        splashMediaTransform = repo.getString(CseConfig.KEY_SPLASH_MEDIA_TRANSFORM, "") ?: "",
        splashMediaVersion = repo.getLong(CseConfig.KEY_SPLASH_MEDIA_VERSION, 0L),
        particleTimeMs = CseConfig.normalizeParticleTime(
            repo.getInt(CseConfig.KEY_PARTICLE_TIME_MS, CseConfig.PARTICLE_TIME_DEFAULT)
        ),
        loadingAnimMode = run {
            val raw = repo.getInt(CseConfig.KEY_LOADING_ANIM_MODE, -1)
            if (raw in CseConfig.LOADING_ANIM_NONE..CseConfig.LOADING_ANIM_BAR) raw
            else if (repo.getBoolean(CseConfig.KEY_ENABLE_MORPH_SHAPE, false)) CseConfig.LOADING_ANIM_SHAPE
            else CseConfig.LOADING_ANIM_NONE
        },
        morphShapeScale = repo.getInt(CseConfig.KEY_MORPH_SHAPE_SCALE, 100),
        loadingBarLength = repo.getInt(
            CseConfig.KEY_LOADING_BAR_LENGTH, CseConfig.LOADING_BAR_LENGTH_DEFAULT
        ).coerceIn(CseConfig.LOADING_BAR_LENGTH_MIN, CseConfig.LOADING_BAR_LENGTH_MAX),
        loadingBarThickness = repo.getInt(
            CseConfig.KEY_LOADING_BAR_THICKNESS, CseConfig.LOADING_BAR_THICKNESS_DEFAULT
        ).coerceIn(CseConfig.LOADING_BAR_THICKNESS_MIN, CseConfig.LOADING_BAR_THICKNESS_MAX),
        morphShapeColorType = repo.getInt(CseConfig.KEY_MORPH_SHAPE_COLOR_TYPE, 0),
        iconOffsetX = repo.getInt(CseConfig.KEY_ICON_OFFSET_X, 0)
            .coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX),
        iconOffsetY = repo.getInt(CseConfig.KEY_ICON_OFFSET_Y, 0)
            .coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX),
        indicatorOffsetX = repo.getInt(CseConfig.KEY_INDICATOR_OFFSET_X, 0)
            .coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX),
        indicatorOffsetY = repo.getInt(CseConfig.KEY_INDICATOR_OFFSET_Y, 0)
            .coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX),
        removeIcon = repo.getBoolean(CseConfig.KEY_REMOVE_ICON, false),
        removeBrandingImage = repo.getBoolean(CseConfig.KEY_REMOVE_BRANDING_IMAGE, false),
        showAppName = repo.getBoolean(CseConfig.KEY_SHOW_APP_NAME, false),
        appNameTextSize = repo.getInt(
            CseConfig.KEY_APP_NAME_TEXT_SIZE, CseConfig.APP_NAME_TEXT_SIZE_DEFAULT
        ).coerceIn(CseConfig.APP_NAME_TEXT_SIZE_MIN, CseConfig.APP_NAME_TEXT_SIZE_MAX),
        appNameOffsetX = repo.getInt(CseConfig.KEY_APP_NAME_OFFSET_X, 0)
            .coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX),
        appNameOffsetY = repo.getInt(CseConfig.KEY_APP_NAME_OFFSET_Y, 0)
            .coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX),
        appNamePosition = repo.getInt(
            CseConfig.KEY_APP_NAME_POSITION, CseConfig.APP_NAME_POS_BOTTOM
        ),
        appNameColorType = repo.getInt(
            CseConfig.KEY_APP_NAME_COLOR_TYPE, CseConfig.APP_NAME_COLOR_FROM_SYSTEM
        ),
        appNameCustomColor = repo.getString(CseConfig.KEY_APP_NAME_CUSTOM_COLOR, "#FFFFFF"),
        appNameFontWeight = repo.getInt(
            CseConfig.KEY_APP_NAME_FONT_WEIGHT, CseConfig.APP_NAME_FONT_WEIGHT_DEFAULT
        ).coerceIn(CseConfig.APP_NAME_FONT_WEIGHT_MIN, CseConfig.APP_NAME_FONT_WEIGHT_MAX),
        appNameUseEnglish = repo.getBoolean(CseConfig.KEY_APP_NAME_USE_ENGLISH, false),
        appNameFontName = repo.getString(CseConfig.KEY_APP_NAME_FONT_NAME, ""),
        appNameFontVersion = repo.getLong(CseConfig.KEY_APP_NAME_FONT_VERSION, 0L),
        changeBgColorType = repo.getInt(CseConfig.KEY_CHANGE_BG_COLOR_TYPE, 0),
        bgColorMode = repo.getInt(CseConfig.KEY_BG_COLOR_MODE, 2),
        customBgColor = repo.getString(CseConfig.KEY_CUSTOM_BG_COLOR, "#FFFFFF"),
        customBgColorNight = repo.getString(CseConfig.KEY_CUSTOM_BG_COLOR_NIGHT, "#000000"),
        showLauncherIcon = repo.getBoolean(CseConfig.KEY_SHOW_LAUNCHER_ICON, true),
        enableHotStartSplash = repo.getBoolean(CseConfig.KEY_ENABLE_HOT_START_SPLASH, false),
        minSplashShowEnabled = repo.getBoolean(CseConfig.KEY_MIN_SPLASH_SHOW_ENABLED, true),
        minSplashShowMs = repo.getInt(
            CseConfig.KEY_MIN_SPLASH_SHOW_MS, CseConfig.MIN_SPLASH_SHOW_MS_DEFAULT
        ).coerceIn(CseConfig.MIN_SPLASH_SHOW_MS_MIN, CseConfig.MIN_SPLASH_SHOW_MS_MAX),
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
    fun setIconScale(value: Int) = update(
        CseConfig.KEY_ICON_SCALE,
        value.coerceIn(CseConfig.ICON_SCALE_MIN, CseConfig.ICON_SCALE_MAX)
    )
    fun setReplaceIcon(value: Boolean) = update(CseConfig.KEY_REPLACE_ICON, value)
    fun setDetailedLog(value: Boolean) = update(CseConfig.KEY_DETAILED_LOG, value)

    fun setSplashImageUri(value: String) = update(CseConfig.KEY_SPLASH_IMAGE_URI, value)

    fun setSplashImageEnabled(value: Boolean) =
        update(CseConfig.KEY_SPLASH_IMAGE_ENABLED, value)

    fun setSplashImageAlpha(value: Int) =
        update(CseConfig.KEY_SPLASH_IMAGE_ALPHA, value.coerceIn(0, 100))

    fun setSplashMediaKind(value: String) = update(CseConfig.KEY_SPLASH_MEDIA_KIND, value)

    fun setSplashMediaTransform(value: String) =
        update(CseConfig.KEY_SPLASH_MEDIA_TRANSFORM, value)

    fun setSplashMediaVersion(value: Long) = update(CseConfig.KEY_SPLASH_MEDIA_VERSION, value)

    fun setAospTransition(value: Boolean) = update(CseConfig.KEY_AOSP_TRANSITION, value)

    fun setParticleTimeMs(value: Int) =
        update(CseConfig.KEY_PARTICLE_TIME_MS, CseConfig.normalizeParticleTime(value))
    fun setLoadingAnimMode(value: Int) = update(CseConfig.KEY_LOADING_ANIM_MODE, value)
    fun setMorphShapeScale(value: Int) = update(CseConfig.KEY_MORPH_SHAPE_SCALE, value)
    fun setLoadingBarLength(value: Int) = update(
        CseConfig.KEY_LOADING_BAR_LENGTH,
        value.coerceIn(CseConfig.LOADING_BAR_LENGTH_MIN, CseConfig.LOADING_BAR_LENGTH_MAX)
    )

    fun setLoadingBarThickness(value: Int) = update(
        CseConfig.KEY_LOADING_BAR_THICKNESS,
        value.coerceIn(CseConfig.LOADING_BAR_THICKNESS_MIN, CseConfig.LOADING_BAR_THICKNESS_MAX)
    )

    fun setMorphShapeColorType(value: Int) =
        update(CseConfig.KEY_MORPH_SHAPE_COLOR_TYPE, value)

    fun setIconOffsetX(value: Int) = update(
        CseConfig.KEY_ICON_OFFSET_X, value.coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX)
    )

    fun setIconOffsetY(value: Int) = update(
        CseConfig.KEY_ICON_OFFSET_Y, value.coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX)
    )

    fun setIndicatorOffsetX(value: Int) = update(
        CseConfig.KEY_INDICATOR_OFFSET_X, value.coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX)
    )

    fun setIndicatorOffsetY(value: Int) = update(
        CseConfig.KEY_INDICATOR_OFFSET_Y, value.coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX)
    )
    fun setRemoveIcon(value: Boolean) = update(CseConfig.KEY_REMOVE_ICON, value)

    fun setRemoveBrandingImage(value: Boolean) =
        update(CseConfig.KEY_REMOVE_BRANDING_IMAGE, value)

    fun setShowAppName(value: Boolean) = update(CseConfig.KEY_SHOW_APP_NAME, value)

    fun setAppNameTextSize(value: Int) = update(
        CseConfig.KEY_APP_NAME_TEXT_SIZE,
        value.coerceIn(CseConfig.APP_NAME_TEXT_SIZE_MIN, CseConfig.APP_NAME_TEXT_SIZE_MAX)
    )

    fun setAppNameOffsetX(value: Int) = update(
        CseConfig.KEY_APP_NAME_OFFSET_X, value.coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX)
    )

    fun setAppNameOffsetY(value: Int) = update(
        CseConfig.KEY_APP_NAME_OFFSET_Y, value.coerceIn(CseConfig.OFFSET_MIN, CseConfig.OFFSET_MAX)
    )

    fun setAppNamePosition(value: Int) = update(CseConfig.KEY_APP_NAME_POSITION, value)

    fun setAppNameColorType(value: Int) = update(CseConfig.KEY_APP_NAME_COLOR_TYPE, value)

    fun setAppNameCustomColor(value: String) = update(CseConfig.KEY_APP_NAME_CUSTOM_COLOR, value)

    fun setAppNameFontWeight(value: Int) = update(
        CseConfig.KEY_APP_NAME_FONT_WEIGHT,
        value.coerceIn(CseConfig.APP_NAME_FONT_WEIGHT_MIN, CseConfig.APP_NAME_FONT_WEIGHT_MAX)
    )

    fun setAppNameUseEnglish(value: Boolean) = update(CseConfig.KEY_APP_NAME_USE_ENGLISH, value)

    fun setAppNameFontName(value: String) = update(CseConfig.KEY_APP_NAME_FONT_NAME, value)

    fun setAppNameFontVersion(value: Long) = update(CseConfig.KEY_APP_NAME_FONT_VERSION, value)
    fun setChangeBgColorType(value: Int) = update(CseConfig.KEY_CHANGE_BG_COLOR_TYPE, value)
    fun setBgColorMode(value: Int) = update(CseConfig.KEY_BG_COLOR_MODE, value)
    fun setCustomBgColor(value: String) = update(CseConfig.KEY_CUSTOM_BG_COLOR, value)
    fun setCustomBgColorNight(value: String) = update(CseConfig.KEY_CUSTOM_BG_COLOR_NIGHT, value)
    fun setEnableHotStartSplash(value: Boolean) =
        update(CseConfig.KEY_ENABLE_HOT_START_SPLASH, value)

    fun setMinSplashShowEnabled(value: Boolean) =
        update(CseConfig.KEY_MIN_SPLASH_SHOW_ENABLED, value)

    fun setMinSplashShowMs(value: Int) = update(
        CseConfig.KEY_MIN_SPLASH_SHOW_MS,
        value.coerceIn(CseConfig.MIN_SPLASH_SHOW_MS_MIN, CseConfig.MIN_SPLASH_SHOW_MS_MAX)
    )

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

    private fun update(key: String, value: Long) {
        repo.setLong(key, value)
        _state.value = read()
    }

    private fun update(key: String, value: String) {
        repo.setString(key, value)
        _state.value = read()
    }
}

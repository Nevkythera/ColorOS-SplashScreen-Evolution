package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale


object LocaleManager {

    private const val TAG = "CSE"

    /** 语言选项索引 —— 与 UI 下拉框的下标一一对应，改动时两边必须同步。 */
    const val FOLLOW_SYSTEM = 0
    const val SIMPLIFIED_CHINESE = 1
    const val ENGLISH = 2

    /** 可选语言数量（UI 用它来构造下拉项）。 */
    const val OPTION_COUNT = 3

    
    private const val PREFS_NAME = "androidx.appcompat.app.AppCompatDelegate.application_locales_record_file"

    /** AndroidX 记录语言列表用的键名。 */
    private const val KEY_LOCALES = "locales"

    /** 本模块自己的配置文件名（与 `ConfigStore` 保持一致）。 */
    private const val LEGACY_PREFS_NAME = "cse_config"

    /** 本模块自己的语言键 —— 跨版本稳定，作为兜底真源。 */
    private const val LEGACY_KEY = "app_language"

    /** 供 `values/` 兜底资源用的空语言标签。 */
    private const val TAG_FOLLOW_SYSTEM = ""

    // ────────────────────────────────────────────────────────────────
    // 写
    // ────────────────────────────────────────────────────────────────

    /**
     * 应用语言。
     *
     * @param index [FOLLOW_SYSTEM] / [SIMPLIFIED_CHINESE] / [ENGLISH]
     */
    fun apply(index: Int) {
        val locales: LocaleListCompat = when (index) {
            SIMPLIFIED_CHINESE -> LocaleListCompat.forLanguageTags("zh-CN")
            ENGLISH -> LocaleListCompat.forLanguageTags("en")
            // 空列表 = 清除本应用的语言设置，回落到跟随系统
            else -> LocaleListCompat.getEmptyLocaleList()
        }

        // ① 我们自己存一份（跨版本稳定，读取时的兜底真源）
        // ② 通知框架/AndroidX（让系统「应用语言」设置页状态同步；失效也不影响功能）
        runCatching { AppCompatDelegate.setApplicationLocales(locales) }
            .onFailure { Log.w(TAG, "event=locale_apply id=appcompat_failed err=${it.message}") }

        // ③ 自己重建 Activity —— API 33+ 框架只会「排队」应用语言，
        //    必须有一次 Activity 重建才会真正生效（见类注释根因 1）。
        recreateAllActivities()
    }

    /**
     * 记录语言选择。由 Activity 在**重建之前**调用，
     * 保证新 Activity 起来时能立刻读到正确值。
     */
    fun persist(context: Context, index: Int) {
        val tag = when (index) {
            SIMPLIFIED_CHINESE -> "zh-CN"
            ENGLISH -> "en"
            else -> TAG_FOLLOW_SYSTEM
        }

        // 本模块自己的偏好（兜底真源）
        runCatching {
            context.applicationContext
                .getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(LEGACY_KEY, tag).apply()
        }.onFailure {
            Log.w(TAG, "event=locale_persist id=legacy_failed err=${it.message}")
        }

        // AndroidX 的偏好 —— 尽量与官方存储保持一致（部分版本会读这里做迁移）
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LOCALES, tag).apply()
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 读
    // ────────────────────────────────────────────────────────────────

    
    fun current(context: Context): Int = indexOf(resolveTag(context))

    
    fun resolveTag(context: Context): String {
        val app = context.applicationContext

        // ① 本模块自己的配置
        runCatching {
            app.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(LEGACY_KEY, null)
        }.getOrNull()?.let { return normalize(it) }

        // ② AndroidX 的存储
        runCatching {
            app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LOCALES, null)
        }.getOrNull()?.let { return normalize(it) }

        // ③ 内存态
        val live = AppCompatDelegate.getApplicationLocales()
        if (!live.isEmpty) return normalize(live.toLanguageTags())

        return TAG_FOLLOW_SYSTEM
    }

    /** 语言标签 → 下拉框下标。 */
    fun indexOf(tag: String): Int {
        if (tag.isBlank()) return FOLLOW_SYSTEM
        return when (Locale.forLanguageTag(tag).language.lowercase(Locale.ROOT)) {
            "zh" -> SIMPLIFIED_CHINESE
            "en" -> ENGLISH
            else -> FOLLOW_SYSTEM
        }
    }

    /** 把各种来源的标签统一成 `"zh-CN"` / `"en"` / `""`。 */
    private fun normalize(raw: String): String {
        if (raw.isBlank()) return TAG_FOLLOW_SYSTEM
        return when (Locale.forLanguageTag(raw).language.lowercase(Locale.ROOT)) {
            "zh" -> "zh-CN"
            "en" -> "en"
            // 其他语言（含用户手动改系统设置选的语言）一律当「跟随系统」处理
            else -> TAG_FOLLOW_SYSTEM
        }
    }

    // ────────────────────────────────────────────────────────────────
    // Activity 重建 & Context 覆盖
    // ────────────────────────────────────────────────────────────────

    
    private fun recreateAllActivities() {
        val activities = ActivityTracker.activities()
        if (activities.isEmpty()) {
            Log.w(TAG, "event=locale_apply id=recreate_skipped reason=no_activity_tracked")
            return
        }
        activities.forEach { activity ->
            runCatching { activity.recreate() }
                .onFailure { Log.w(TAG, "event=locale_apply id=recreate_failed err=${it.message}") }
        }
        Log.i(TAG, "event=locale_apply id=recreate count=${activities.size}")
    }

    
    fun wrap(context: Context): Context {
        val tag = resolveTag(context)
        if (tag.isBlank()) return context // 跟随系统：不做任何覆盖

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }

    /** 当前语言标签，仅用于日志。 */
    fun describe(context: Context): String =
        resolveTag(context).ifBlank { "follow_system" }

    /**
     * 存活 Activity 的登记表 —— 由 `MainActivity` 在
     * `onCreate` 里通过 `Application.registerActivityLifecycleCallbacks` 注册，
     * 用弱引用避免泄漏。
     */
    internal object ActivityTracker {
        private val refs = mutableListOf<java.lang.ref.WeakReference<Activity>>()

        fun register(activity: Activity) {
            refs.removeAll { it.get() == null }
            if (refs.none { it.get() === activity }) {
                refs.add(java.lang.ref.WeakReference(activity))
            }
        }

        fun unregister(activity: Activity) {
            refs.removeAll { it.get() == activity || it.get() == null }
        }

        fun activities(): List<Activity> {
            refs.removeAll { it.get() == null }
            return refs.mapNotNull { it.get() }
        }
    }
}

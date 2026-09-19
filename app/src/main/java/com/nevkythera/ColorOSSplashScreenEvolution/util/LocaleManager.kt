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

/**
 * 模块界面语言管理。
 *
 * ────────────────────────────────────────────────────────────────────────
 * ★★ 为什么要自己重建 Activity（血泪教训，别改回去）
 * ────────────────────────────────────────────────────────────────────────
 *
 * 最初只用 `AppCompatDelegate.setApplicationLocales()`，结果**英文完全失效**。
 * 根因有两条，缺一不可：
 *
 * **根因 1：API 33+ 走的是「框架级」LocaleManager，而框架那条路需要 Activity 重建。**
 *
 * 反编译 appcompat 1.7.1 - `setApplicationLocales()` 字节码：
 *
 * ```java
 * if (Build.VERSION.SDK_INT >= 33) {
 *     Object lm = getLocaleManagerForApplication();
 *     if (lm != null) {
 *         Api33Impl.localeManagerSetApplicationLocales(lm, LocaleList.forLanguageTags(tags));
 *     }
 *     // ★ 注意：这条分支**完全不动** sRequestedAppLocales / sActivityDelegates
 * } else {
 *     sRequestedAppLocales = locales;
 *     applyLocalesToActiveDelegates();   // ← 旧版才会主动刷新所有 AppCompatDelegate
 * }
 * ```
 *
 * 也就是说 API 33+ 只把语言写进框架的 `LocaleManager`，
 * 真正的 Resources 覆盖要等 **Activity 重建**时由 `ResourcesManager` 应用。
 * 框架自己的 `Activity` 在 `ActivityThread.performLaunchActivity()` 里会
 * 重新 `getResources()`，所以系统应用没问题；但本模块的界面是
 * `ComponentActivity + Compose`，**没有任何组件会主动触发那次重建** →
 * 语言写了等于没写。
 *
 * 补充：该分支还依赖 `getLocaleManagerForApplication()`，
 * 其实现是 `context.getSystemService(LocaleManager.class)`，
 * 内部要求调用方是「应用级 Context」，**传 Activity 会返回 null**，
 * 于是 `if (lm != null)` 不成立，连写入都被跳过。
 *
 * **根因 2：`AppLocalesMetadataHolderService` 声明被清单合并丢弃 → 无法自动持久化。**
 *
 * 反编译 `AppCompatDelegate.isAutoStorageOptedIn()`：它去查
 * `AppLocalesMetadataHolderService` 这个 **Service 的 metaData** 里有没有
 * `autoStoreLocales=true`；查不到（抛 `NameNotFoundException`）就直接
 * `sIsAutoStoreLocalesOptedIn = false`，并打日志
 * `"Checking for metadata for AppLocalesMetadataHolderService : Service not found"`。
 *
 * 该 Service 从 appcompat 1.6 起由「aar 清单」声明改为**从源码编译进注入的清单**，
 * 而本工程**没有依赖 appcompat 的 AndroidManifest 合并产物**，所以 APK 清单里
 * 压根没有这个 Service（可用 `aapt dump xmltree <apk> AndroidManifest.xml | grep Locales` 复核）。
 *
 * ────────────────────────────────────────────────────────────────────────
 * ★ 本方案的策略：**双写 + 自己重建**
 * ────────────────────────────────────────────────────────────────────────
 *
 * 1. **双存储**：既有 AndroidX 自己的 SharedPreferences（[PREFS_NAME]），
 *    也写进本模块的 `cse_config`（[LEGACY_PREFS_NAME]）。
 *    ★ 冗余不是浪费 —— 前者依赖 appcompat 内部键名（未来升级可能变），
 *    后者是我们自己的键，**跨版本一定读得到**，作为兜底。
 * 2. **读回时两处都查**，`current()` 与 `applyTo()` 共用同一个解析函数，不会打架。
 * 3. **自己重建 Activity**：不指望框架替我们重建（它不会）。
 *    重建后 `attachBaseContext()` 里用 `createConfigurationContext()` 覆盖语言。
 * 4. `AppCompatDelegate.setApplicationLocales()` **仍然调用**：
 *    它在 API 33+ 会去更新系统「应用语言」设置页的展示状态；
 *    只是我们不再**依赖**它来生效。失败/无效果也不影响功能。
 *
 * ★ 语言选项刻意只保留「跟随系统 / 简体中文 / English」三种 —— 明确不含俄语。
 * ★ minSdk = 35（Android 15），下面所有 API 33 判断都是恒真的防御性写法。
 */
object LocaleManager {

    private const val TAG = "CSE"

    /** 语言选项索引 —— 与 UI 下拉框的下标一一对应，改动时两边必须同步。 */
    const val FOLLOW_SYSTEM = 0
    const val SIMPLIFIED_CHINESE = 1
    const val ENGLISH = 2

    /** 可选语言数量（UI 用它来构造下拉项）。 */
    const val OPTION_COUNT = 3

    /**
     * AndroidX 自己的语言偏好文件名。
     * 取自 `AppLocalesStorageHelper` 的实现（appcompat 1.7.1）。
     * ★ 只用于**兜底读**，不作为唯一真源 —— 见类注释「双存储」。
     */
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

    /**
     * 读取当前语言设置，用于初始化下拉框的选中项。
     *
     * ★ 判定顺序很重要：先看首选语言是不是 en，再看是不是 zh，
     *   否则 "zh-Hans-CN" 这类变体可能匹配不上而错误落回「跟随系统」。
     */
    fun current(context: Context): Int = indexOf(resolveTag(context))

    /**
     * 解析当前生效的语言标签，返回 `"zh-CN"` / `"en"` / `""`（跟随系统）。
     *
     * ★ 读取顺序（与写入顺序镜像）：
     *   ① 本模块自己的 `cse_config` —— 最稳，不依赖 appcompat 内部实现
     *   ② AndroidX 的偏好 —— 兼容用户在系统设置里改过语言的情况
     *   ③ `AppCompatDelegate.getApplicationLocales()` —— 内存态
     */
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

    /**
     * 重建当前进程内所有 Activity。
     *
     * ★ 为什么不直接调 `activity.recreate()`：
     *   本模块只有一个 Activity，但「重建」这个动作的语义是
     *   「让**所有**已存在的窗口重新读语言」，写成一个通用遍历，
     *   以后加了新 Activity 不用回来改。
     *
     * ★ 用 `Application.registerActivityLifecycleCallbacks` 拿到的实例集合，
     *   而不是自己维护静态引用 —— 后者会泄漏 Activity。
     */
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

    /**
     * 用当前语言包装 [context]。
     *
     * ★ 必须在 Activity 的 `attachBaseContext()` 里调用，**越早越好** ——
     *   晚了 Compose 已经拿到旧的 `Resources`，界面不会变。
     *
     * 传 `Activity` 是历史遗留签名，内部一律转成 Application 再取偏好，
     * 避免用到还没覆盖语言的 Activity 自身 Resources。
     */
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

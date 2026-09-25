package com.Nevkythera.ColorOSSplashScreenEvolution.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.HotReloadResult
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 配置存储 + LSPosed 激活状态。
 *
 * 本类取代了旧版的 `ActivationStore`（"Hook 侧写 JSON 文件 → App 侧读文件"）。
 * 旧方案有三个致命缺陷：
 *   1. 必须等 Hook 真的被注入并加载过才会落盘 —— 刚装完模块、还没重启 SystemUI 时
 *      会误报"未激活"；
 *   2. 文件里带时间戳，超过 [CseConfig] 里设定的陈旧阈值后会被判定为过期；
 *   3. Hook 侧在 `onModuleLoaded` 阶段调用 `ActivityThread.currentApplication()`
 *      经常拿到 null（此时 Application 还没 attach），导致信号根本写不出来。
 *
 * 现在的做法（与 libxposed 官方推荐一致）：
 *   模块设置界面通过 [XposedServiceHelper] 绑定框架提供的 [XposedService]。
 *   绑定成功 → 模块确实被 LSPosed 启用 → "已激活"；
 *   服务断开 → "未激活"。
 *   这比读文件既准又快（无需重启 SystemUI 就能立刻反映状态）。
 *
 * 顺带用 [XposedService.getRemotePreferences] 打通"设置界面 ↔ Hook 进程"的配置同步，
 * 让功能开关真正实时生效，而不再受编译期常量的限制。
 */
class XposedRepo private constructor(context: Context) {

    companion object {
        /** 远程偏好仓库名，Hook 侧需用同名调用 getRemotePreferences。 */
        const val REMOTE_PREFS_NAME = "cse_prefs"

        private const val TAG = "CSE"

        @Volatile
        private var instance: XposedRepo? = null

        fun getInstance(context: Context): XposedRepo =
            instance ?: synchronized(this) {
                instance ?: XposedRepo(context.applicationContext).also { instance = it }
            }
    }

    private val appContext = context.applicationContext

    /** App 本地偏好。即使模块未激活也要能正常读写，保证界面不崩。 */
    private val localPrefs: SharedPreferences =
        appContext.getSharedPreferences(CseConfig.PREFS_NAME, Context.MODE_PRIVATE)

    /** LSPosed 侧远程偏好。仅在服务绑定成功后可用。 */
    private var remotePrefs: SharedPreferences? = null

    /** 当前绑定的框架服务，未激活时为 null。 */
    @Volatile
    private var service: XposedService? = null

    /** 激活状态变化回调（在 Binder 线程触发，调用方需自行切回主线程）。 */
    var onActiveChanged: ((Boolean) -> Unit)? = null

    /** 服务绑定成功后回调，可用来刷新框架信息展示。 */
    var onServiceInfoChanged: ((FrameworkInfo?) -> Unit)? = null

    /** 框架信息快照。 */
    data class FrameworkInfo(
        val name: String,
        val version: String,
        val apiVersion: Int,
        val scope: List<String>
    )

    @Volatile
    private var info: FrameworkInfo? = null

    fun frameworkInfo(): FrameworkInfo? = info

    /** 是否已激活。 */
    val isActive: Boolean get() = service != null

    /** 当前框架的 API 版本（未激活时为 0）。 */
    val apiVersion: Int get() = service?.apiVersion ?: 0

    init {
        XposedServiceHelper.registerListener(
            object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(s: XposedService) {
                    service = s
                    remotePrefs = runCatching {
                        s.getRemotePreferences(REMOTE_PREFS_NAME)
                    }.getOrNull()

                    // 把本地已有的配置推一份到远程，保证首次启用时 Hook 侧能读到用户设置
                    syncAllToRemote()

                    info = runCatching {
                        FrameworkInfo(
                            name = s.frameworkName.orEmpty(),
                            version = s.frameworkVersion.orEmpty(),
                            apiVersion = s.apiVersion,
                            scope = s.scope.orEmpty()
                        )
                    }.getOrNull()

                    Log.i(TAG, "event=xposed_service result=bound framework=${info?.name} api=${info?.apiVersion}")
                    onActiveChanged?.invoke(true)
                    onServiceInfoChanged?.invoke(info)
                }

                override fun onServiceDied(s: XposedService) {
                    service = null
                    remotePrefs = null
                    info = null
                    Log.i(TAG, "event=xposed_service result=died")
                    onActiveChanged?.invoke(false)
                    onServiceInfoChanged?.invoke(null)
                }
            }
        )
    }

    // ---------------------------------------------------------------- 读写

    fun getBoolean(key: String, def: Boolean): Boolean =
        localPrefs.getBoolean(key, def)

    fun getInt(key: String, def: Int): Int =
        localPrefs.getInt(key, def)

    fun getLong(key: String, def: Long): Long =
        localPrefs.getLong(key, def)

    fun getString(key: String, def: String): String =
        localPrefs.getString(key, def) ?: def

    /**
     * 写入并同步到远程（若已激活）。
     * 传入 [syncRemote] = false 用于"仅本地"的场景。
     */
    fun setBoolean(key: String, value: Boolean, syncRemote: Boolean = true) {
        localPrefs.edit { putBoolean(key, value) }
        if (syncRemote) {
            remotePrefs?.edit { putBoolean(key, value) }
        }
    }

    fun setInt(key: String, value: Int, syncRemote: Boolean = true) {
        localPrefs.edit { putInt(key, value) }
        if (syncRemote) {
            remotePrefs?.edit { putInt(key, value) }
        }
    }

    fun setLong(key: String, value: Long, syncRemote: Boolean = true) {
        localPrefs.edit { putLong(key, value) }
        if (syncRemote) {
            remotePrefs?.edit { putLong(key, value) }
        }
    }

    fun setString(key: String, value: String, syncRemote: Boolean = true) {
        localPrefs.edit { putString(key, value) }
        if (syncRemote) {
            remotePrefs?.edit { putString(key, value) }
        }
    }

    /** 首次绑定服务时，把本地全部配置推到远程。 */
    private fun syncAllToRemote() {
        val remote = remotePrefs ?: return
        remote.edit {
            localPrefs.all.forEach { (k, v) ->
                when (v) {
                    is Boolean -> putBoolean(k, v)
                    is Int -> putInt(k, v)
                    is Long -> putLong(k, v)
                    is Float -> putFloat(k, v)
                    is String -> putString(k, v)
                }
            }
        }
    }

    /**
     * 触发**所有已加载目标**的热重载（libxposed API 102 的 `XposedService.hotReloadModule`）。
     *
     * 结果统一在**主线程**回调；[onDone] 的 message 为可选失败原因（null 表示“未激活/无目标”）。
     */
    fun hotReload(onDone: (ok: Boolean, message: String?) -> Unit) {
        val s = service
        if (s == null) {
            onDone(false, null)
            return
        }
        val targets = runCatching { s.runningTargets }.getOrNull().orEmpty()
        if (targets.isEmpty()) {
            onDone(false, null)
            return
        }
        val main = Handler(Looper.getMainLooper())
        val remaining = AtomicInteger(targets.size)
        val allOk = AtomicBoolean(true)
        val firstMsg = AtomicReference<String?>(null)
        for (t in targets) {
            runCatching {
                s.hotReloadModule(t, Bundle(), object : XposedService.HotReloadCallback {
                    override fun onHotReloadResult(target: HookedTarget, result: HotReloadResult) {
                        if (result.status != HotReloadResult.Status.SUCCEEDED &&
                            result.status != HotReloadResult.Status.IN_PROGRESS
                        ) {
                            allOk.set(false)
                            if (firstMsg.get() == null) firstMsg.set(result.message)
                        }
                        if (remaining.decrementAndGet() == 0) {
                            main.post { onDone(allOk.get(), firstMsg.get()) }
                        }
                    }
                })
            }.onFailure { e ->
                allOk.set(false)
                if (firstMsg.get() == null) firstMsg.set(e.message)
                if (remaining.decrementAndGet() == 0) {
                    main.post { onDone(allOk.get(), firstMsg.get()) }
                }
            }
        }
    }
}

package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * 桌面图标显隐控制。
 *
 * 原理：
 *   AndroidManifest 中真正的入口 MainActivity **不带 LAUNCHER**，
 *   桌面图标由 activity-alias `LauncherAlias` 提供。
 *   通过 PackageManager.setComponentEnabledSetting 启用/禁用该别名，
 *   即可在不卸载应用的前提下让桌面图标出现或消失。
 *
 * 注意（重要）：
 *   - 禁用后图标会消失，用户只能通过 LSPosed 管理器等入口再次打开本 App。
 *     因此界面上必须给出明确提示。
 *   - 切换后部分桌面（如 OPPO 桌面）需要短暂延迟才会刷新，属正常现象。
 *   - DONT_KILL_APP：不禁用应用本身，避免切换图标时把当前界面一起杀掉。
 */
object LauncherIconController {

    private val ALIAS = ComponentName(
        "com.Nevkythera.ColorOSSplashScreenEvolution",
        "com.Nevkythera.ColorOSSplashScreenEvolution.LauncherAlias"
    )

    /** 当前桌面图标是否可见。 */
    fun isIconVisible(context: Context): Boolean {
        val pm = context.packageManager
        return when (pm.getComponentEnabledSetting(ALIAS)) {
            // 显式启用
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            // 显式禁用
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            // 跟随 manifest（默认 enabled=true）
            else -> true
        }
    }

    /**
     * 设置桌面图标可见性。
     *
     * @return true 表示设置成功
     */
    fun setIconVisible(context: Context, visible: Boolean): Boolean = try {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            ALIAS,
            if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        true
    } catch (_: Throwable) {
        false
    }
}

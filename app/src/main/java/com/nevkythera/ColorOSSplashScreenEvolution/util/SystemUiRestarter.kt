package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.DataOutputStream

/**
 * 重启 SystemUI 与 system_server 侧 StartingWindow 逻辑。
 *
 * 说明：
 *  - 重启 SystemUI 会让 shell 侧（SplashscreenWindowCreator 等）重新初始化，
 *    使新配置生效。
 *  - 需要 root（su）。没有 root 时退化为仅提示用户手动操作。
 *  - 本模块不会在未授权的情况下静默执行 root 命令；调用方需自行确认用户已同意。
 */
object SystemUiRestarter {

    private const val TAG = "CSE"

    /** SystemUI 在 ColorOS 上的进程名（部分版本带后缀，一并尝试）。 */
    private val SYSTEMUI_PROCESSES = listOf(
        "com.android.systemui",
        "com.android.systemui:ui",
        "com.android.systemui:screenshot"
    )

    /**
     * 通过 su 重启 SystemUI。
     *
     * @return true 表示命令已成功下发（不代表 SystemUI 一定重启成功）。
     */
    fun restartSystemUi(context: Context): Boolean {
        val cmds = buildList {
            SYSTEMUI_PROCESSES.forEach { add("pkill -f $it") }
            // 兜底：用 am 杀掉 systemui 进程，系统会自动拉起
            add("am force-stop com.android.systemui")
        }

        return try {
            execAsRoot(cmds)
        } catch (t: Throwable) {
            Log.w(TAG, "restartSystemUi failed", t)
            false
        }
    }

    /**
     * 重启整机（reboot）。
     *
     * @return true 表示命令已成功下发。
     */
    fun rebootSystem(): Boolean = try {
        execAsRoot(listOf("reboot"))
    } catch (t: Throwable) {
        Log.w(TAG, "rebootSystem failed", t)
        false
    }

    /**
     * 只重启 system_server 侧的 splash 相关逻辑代价过大（会软重启），
     * 这里保留接口但不默认启用。
     */
    fun softReboot(): Boolean = try {
        execAsRoot(listOf("setprop ctl.restart zygote"))
    } catch (t: Throwable) {
        Log.w(TAG, "softReboot failed", t)
        false
    }

    private fun execAsRoot(commands: List<String>): Boolean {
        var process: Process? = null
        var out: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec(if (hasSu()) "su" else "sh")
            out = DataOutputStream(process.outputStream)
            commands.forEach {
                out.writeBytes("$it\n")
            }
            out.writeBytes("exit\n")
            out.flush()
            process.waitFor()
            Log.i(TAG, "event=restart result=ok cmd_count=${commands.size} sdk=${Build.VERSION.SDK_INT}")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "event=restart result=fail reason=${t.javaClass.simpleName}", t)
            false
        } finally {
            runCatching { out?.close() }
            runCatching { process?.destroy() }
        }
    }

    private fun hasSu(): Boolean = try {
        val candidates = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/magisk/.core/bin/su"
        )
        candidates.any { java.io.File(it).exists() }
    } catch (_: Throwable) {
        false
    }
}

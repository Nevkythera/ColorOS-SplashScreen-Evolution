package com.Nevkythera.ColorOSSplashScreenEvolution.util

import androidx.annotation.StringRes
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Root 管理器检测。
 *
 * ## 旧实现的缺陷（已修）
 * 旧版用 `ProcessBuilder("sh", "-c", "ksud -V")` 直接在**普通应用进程**里 fork shell。
 * 但 `ksud` / `magisk` / `apd` 这些可执行文件位于 `/data/adb/` 下，属主为 root、
 * 权限 0700，普通应用**没有任何权限执行或读取**。结果是 shell 探针永远返回空，
 * 代码只能回落到 `PackageInfo.versionName` 兜底 —— 而 KernelSU 的 versionName
 * 常常是 "1.0.0" 这类占位值，于是界面显示"未检测到"或一个完全错误的版本号。
 *
 * ## 现在的做法
 * 全部版本查询都通过 `su -c "<cmd>"` 执行，拿到 root 上下文后再读：
 *   - KernelSU / KernelSU Next : `/data/adb/ksud -V`
 *   - Magisk                   : `magisk -v`
 *   - APatch                   : `/data/adb/apd -V`
 *
 * 同时单独做一次连通性探测（`su -c echo Connected`）来判定 root 是否真的可用，
 * 这比"看 /system/bin/su 文件是否存在"可靠得多 —— 后者在多数现代机型上都不存在。
 *
 * ## 文案约定
 * 本对象是**纯 Kotlin 工具类（无 Context）**，因此所有面向用户的文案一律以
 * 字符串资源 ID（`@StringRes`）形式返回，由 Compose 层用 `stringResource` 解析，
 * 避免把中文硬编码在逻辑层。
 *
 * 注意：本对象所有方法都会 fork 进程执行 root 命令，务必在 IO 线程调用。
 */
object RootManagerDetector {

    /** 单条 shell 命令超时。su 弹窗授权时可能偏慢，给足余量但别太夸张。 */
    private const val TIMEOUT_MS = 3000L

    /** 探针返回的哨兵字符串。 */
    private const val CONNECTED = "Connected"

    /**
     * 检测结果。
     *
     * [name] 用 `@StringRes Int`：识别成功的方案名（KernelSU/Magisk/APatch）是
     * 专有名词、无需翻译，走 [nameLiteral]；识别失败的两条状态走资源。
     */
    data class RootInfo(
        @StringRes val nameRes: Int = R.string.root_not_detected,
        val nameLiteral: String? = null,
        @StringRes val versionRes: Int = R.string.value_none,
        val versionLiteral: String? = null,
        /** root 是否真正可用（决定"重启 SystemUI"是否可用）。 */
        val hasSu: Boolean = false
    )

    /** 识别出的 Root 方案名（专有名词，直接显示字面量）。 */
    private fun scheme(name: String, version: String) = RootInfo(
        nameLiteral = name,
        versionLiteral = version,
        hasSu = true
    )

    /**
     * root 权限是否可用。
     *
     * 用 `su -c echo Connected` 判定：能拿到预期的回显才算真的拿到了 root。
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        executeRootCommand("echo $CONNECTED") == CONNECTED
    }

    /**
     * 完整检测。会依次尝试各管理器的版本探针，命中即返回。
     *
     * 只有 root 真的可用时才去读版本 —— 否则每条探针都会卡满超时，
     * 界面要转好几秒，体验很差。
     */
    suspend fun detect(): RootInfo = withContext(Dispatchers.IO) {
        if (!checkRootConnectivity()) {
            return@withContext RootInfo()
        }

        // 逐个探针，命中即止
        val kernelsu = executeRootCommand("$PATH_KS_UD -V")
        if (!kernelsu.isNullOrBlank()) {
            return@withContext scheme(
                "KernelSU",
                kernelsu.lineSequence().first().trim().ifBlank { "" }
            ).let {
                if (it.versionLiteral.isNullOrBlank()) {
                    it.copy(versionLiteral = null, versionRes = R.string.root_unknown_version)
                } else it
            }
        }

        val magisk = executeRootCommand("magisk -v")
        if (!magisk.isNullOrBlank()) {
            return@withContext scheme(
                "Magisk",
                magisk.lineSequence().first().trim().ifBlank { "" }
            ).let {
                if (it.versionLiteral.isNullOrBlank()) {
                    it.copy(versionLiteral = null, versionRes = R.string.root_unknown_version)
                } else it
            }
        }

        val apatch = executeRootCommand("$PATH_APD -V")
        if (!apatch.isNullOrBlank()) {
            return@withContext scheme(
                "APatch",
                apatch.lineSequence().first().trim().ifBlank { "" }
            ).let {
                if (it.versionLiteral.isNullOrBlank()) {
                    it.copy(versionLiteral = null, versionRes = R.string.root_unknown_version)
                } else it
            }
        }

        // 有 root 但识别不出具体管理器
        RootInfo(nameRes = R.string.root_unknown_scheme, hasSu = true)
    }

    private const val PATH_KS_UD = "/data/adb/ksud"
    private const val PATH_APD = "/data/adb/apd"

    /** 一次轻量连通性探测。 */
    private fun checkRootConnectivity(): Boolean =
        executeRootCommand("echo $CONNECTED") == CONNECTED

    /**
     * 以 root 身份执行一条命令。
     *
     * @param command 需要执行的命令（不含 `su -c` 本身）
     * @return 标准输出（已 trim）；执行失败或退出码非 0 时返回 null
     */
    fun executeRootCommand(command: String): String? {
        var process: Process? = null
        return try {
            // 用数组形式传参，避免 shell 二次解析
            process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()

            // 简单超时保护：命令卡住时强行结束
            val finished = process.waitFor(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }

            if (process.exitValue() == 0) output else null
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { process?.destroy() }
        }
    }
}

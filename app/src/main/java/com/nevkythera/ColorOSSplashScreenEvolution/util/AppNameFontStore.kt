package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * 应用名称自定义字体的存储与跨进程读取。
 *
 * 与 [SplashImageStore] 同一套路：模块 Hook 跑在 SystemUI 进程，读不到本应用私有文件，
 * 于是把用户选的字体落在应用私有目录，再用同一个只读
 * [com.Nevkythera.ColorOSSplashScreenEvolution.data.SplashImageProvider]
 * （authority `<packageName>.splashimage`）开放给 SystemUI 读取。
 */
object AppNameFontStore {

    /** 私有目录里的固定文件名（保留 .ttf 后缀，供 Typeface 识别）。 */
    const val FILE_NAME = "app_name_font.ttf"

    /** Hook 侧要读取的 content URI。 */
    fun contentUri(packageName: String): Uri =
        Uri.parse("content://$packageName${SplashImageStore.AUTHORITY_SUFFIX}/$FILE_NAME")

    /** 本应用私有目录里的字体文件。 */
    fun fontFile(context: Context): File = File(context.filesDir, FILE_NAME)

    /** 把用户选的字体拷进私有目录，返回其显示名（失败返回 null）。 */
    fun save(context: Context, uri: Uri): String? = runCatching {
        val file = fontFile(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        } ?: return null
        if (file.length() <= 0L) return null
        displayName(context, uri)
    }.getOrNull()

    /** 删除已保存的自定义字体。 */
    fun clear(context: Context) {
        runCatching { fontFile(context).delete() }
    }

    private fun displayName(context: Context, uri: Uri): String = runCatching {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else {
                null
            }
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
}

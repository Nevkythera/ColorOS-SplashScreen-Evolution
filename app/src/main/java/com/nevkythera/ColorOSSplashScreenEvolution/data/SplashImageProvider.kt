package com.Nevkythera.ColorOSSplashScreenEvolution.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.Nevkythera.ColorOSSplashScreenEvolution.util.AppNameFontStore
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SplashImageStore
import java.io.FileNotFoundException

/**
 * 只读 ContentProvider：把当前选中的启动遮罩背景图片 / 应用名称自定义字体
 * 开放给 SystemUI 进程的 Hook。
 *
 * 模块 App 与 Hook 是两个进程、两个 uid，Hook 读不到 App 私有文件。这里把
 * `filesDir/splash_bg` 与 `filesDir/app_name_font.ttf` 通过一个 exported 的只读
 * provider 暴露出去，Hook 侧用 `contentResolver.openInputStream` 读取即可。
 *
 * 仅暴露这两个固定文件名，不提供任何写操作。
 */
class SplashImageProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("read-only provider: $uri")
        val context: Context = context ?: throw FileNotFoundException(uri.toString())
        val file = when (uri.lastPathSegment) {
            SplashImageStore.FILE_NAME -> SplashImageStore.imageFile(context)
            AppNameFontStore.FILE_NAME -> AppNameFontStore.fontFile(context)
            else -> throw FileNotFoundException(uri.toString())
        }
        if (!file.exists()) throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? = when (uri.lastPathSegment) {
        AppNameFontStore.FILE_NAME -> "font/*"
        else -> "image/*"
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = 0
}

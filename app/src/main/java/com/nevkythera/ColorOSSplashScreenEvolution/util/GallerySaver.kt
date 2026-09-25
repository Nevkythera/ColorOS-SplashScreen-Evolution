package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes

/** 把内置图片资源保存到系统相册（Pictures/CSE）。 */
object GallerySaver {

    fun savePngResource(
        context: Context,
        @DrawableRes resId: Int,
        displayName: String
    ): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/CSE"
            )
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            context.resources.openRawResource(resId).use { it.copyTo(out) }
        } ?: return false
        true
    }.getOrDefault(false)
}

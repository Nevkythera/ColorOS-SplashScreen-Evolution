package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette

/**
 * 图形工具类 —— 从 RestoreSplashScreen 的 `GraphicUtils` 精简迁移。
 *
 * 仅保留"绘制图标圆角 / 缩小图标 / 替换图标获取方式 / 替换背景颜色"所需的
 * 四个方法，去掉 MIUI 大图标、图标包、单独配置应用等无关逻辑。
 */
object GraphicUtils {

    
    fun drawable2Bitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        canvas.setBitmap(null)
        return bitmap
    }

    
    fun toSampleBitmap(drawable: Drawable, targetSize: Int = 64): Bitmap? {
        val out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val oldBounds = drawable.copyBounds()
        val oldAlpha = drawable.alpha
        val oldFilter = drawable.colorFilter
        try {
            // 等比居中绘制到 targetSize×targetSize，保持图标原始宽高比
            drawable.alpha = 255
            drawable.colorFilter = null
            drawable.bounds = android.graphics.Rect(0, 0, targetSize, targetSize)
            drawable.draw(canvas)
        } catch (t: Throwable) {
            out.recycle()
            return null
        } finally {
            runCatching {
                drawable.bounds = oldBounds
                drawable.alpha = oldAlpha
                drawable.colorFilter = oldFilter
            }
            canvas.setBitmap(null)
        }
        return out
    }

    /**
     * 绘制图标圆角（使用 BitmapShader）。
     *
     * @param bitmap        待绘制圆角的 Bitmap
     * @param cornerRadius  圆角半径（px）
     * @param shrinkTrigger 若图标尺寸小于此数值则放大图标（低分辨率图标放大），
     *                      传 0 表示不放大。对齐 RestoreSplashScreen 的
     *                      GraphicUtils#roundBitmapByShader(bitmap, roundDegree, shrinkTrigger)。
     * @return 绘制圆角后的 Bitmap，输入为 null 时返回 null
     */
    fun roundBitmapByShader(bitmap: Bitmap?, cornerRadius: Float, shrinkTrigger: Int = 0): Bitmap? {
        if (bitmap == null) return null

        val targetBitmap =
            Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(targetBitmap).drawRoundRect(
            RectF(0F, 0F, bitmap.width.toFloat(), bitmap.height.toFloat()),
            cornerRadius,
            cornerRadius,
            Paint().apply {
                isAntiAlias = true
                shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
        )

        // 低分辨率图标放大（尺寸小于 shrinkTrigger 时放大 1.7 倍）
        if (bitmap.width < shrinkTrigger) {
            val enlarged = Bitmap.createBitmap(
                (bitmap.width * 1.7).toInt(),
                (bitmap.width * 1.7).toInt(),
                Bitmap.Config.ARGB_8888
            )
            Canvas(enlarged).drawBitmap(
                targetBitmap,
                (enlarged.width * 0.5 - bitmap.width * 0.5).toFloat(),
                (enlarged.height * 0.5 - bitmap.height * 0.5).toFloat(),
                null
            )
            return enlarged
        }
        return targetBitmap
    }

    /**
     * 根据 Bitmap 获取背景颜色（主色提取）。
     *
     * @param bitmap 从中获取颜色的图片
     * @param isLight 是否为浅色模式
     */
    fun getBgColor(bitmap: Bitmap, isLight: Boolean): Int {
        val hsv = FloatArray(3)
        val color = Palette.from(bitmap).maximumColorCount(8).generate()
            .getDominantColor(Color.parseColor(if (isLight) "#F5F5F5" else "#1C2833"))
        Color.colorToHSV(color, hsv)
        if (isLight) {
            hsv[1] = hsv[1] - 0.4f // 减小饱和度
            hsv[2] = hsv[2] + 0.2f // 增大明度
        } else {
            hsv[1] = hsv[1] - 0.2f // 减小饱和度
            hsv[2] = hsv[2] - 0.7f // 减小明度
        }
        return Color.HSVToColor(hsv)
    }

    /**
     * 缩小图标, 以避免后续使用 setRenderEffect 模糊图标时出现毛边问题。
     *
     * @param drawable     需要创建阴影的 Drawable
     * @param oriIconSize  原始图标大小
     * @param blurIconSize 待模糊图标大小
     * @param cornerRadius 模糊图标圆角大小
     */
    fun createShadowedIcon(
        context: Context?,
        drawable: Drawable?,
        oriIconSize: Int,
        blurIconSize: Int,
        cornerRadius: Float
    ): Drawable? {
        if (drawable == null || context == null) return null

        val originalSize = drawable.intrinsicWidth
        val ratio = (oriIconSize.toDouble() / originalSize).coerceAtMost(1.0).toFloat()

        val scaledSize = (originalSize * ratio).toInt()
        val scaledBitmap = Bitmap.createBitmap(scaledSize, scaledSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)

        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, scaledSize.toFloat(), scaledSize.toFloat()),
                cornerRadius, cornerRadius, Path.Direction.CW
            )
        }
        canvas.clipPath(path)
        drawable.setBounds(0, 0, scaledSize, scaledSize)
        drawable.draw(canvas)

        val shadowSize = blurIconSize / 4
        val shadowBitmap = Bitmap.createBitmap(
            scaledSize + shadowSize, scaledSize + shadowSize, Bitmap.Config.ARGB_8888
        )
        val shadowCanvas = Canvas(shadowBitmap)
        shadowBitmap.setHasAlpha(true)
        val paint = Paint(3).apply { alpha = 90 }
        shadowCanvas.drawColor(0, PorterDuff.Mode.CLEAR)
        shadowCanvas.drawBitmap(
            scaledBitmap, (shadowSize / 2).toFloat(), (shadowSize / 2).toFloat(), paint
        )

        return BitmapDrawable(context.resources, shadowBitmap)
    }
}

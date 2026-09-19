package com.Nevkythera.ColorOSSplashScreenEvolution.wrapper

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable

/**
 * 透明背景的 AdaptiveIconDrawable —— 从 RestoreSplashScreen 迁移。
 *
 * 用于"替换图标获取方式"时包裹主题图标，避免 SystemUI 自己的
 * normalizeAndWrapToAdaptiveIcon 对图标二次缩放造成白边/错位。
 *
 * 原项目用 YukiHookAPI 反射访问 AdaptiveIconDrawable 的私有字段，
 * 这里改用标准 Java 反射。
 */
class TransparentAdaptiveIconDrawable(
    foregroundDrawable: Drawable
) : AdaptiveIconDrawable(ColorDrawable(Color.TRANSPARENT), foregroundDrawable) {

    private val mLayersShaderField = javaClass.superclass.getDeclaredField("mLayersShader")
        .apply { isAccessible = true }
    private val mCanvasField = javaClass.superclass.getDeclaredField("mCanvas")
        .apply { isAccessible = true }
    private val mLayersBitmapField = javaClass.superclass.getDeclaredField("mLayersBitmap")
        .apply { isAccessible = true }
    private val mPaintField = javaClass.superclass.getDeclaredField("mPaint")
        .apply { isAccessible = true }
    private val mMaskScaleOnlyField = javaClass.superclass.getDeclaredField("mMaskScaleOnly")
        .apply { isAccessible = true }

    private var mLayersShader: Shader?
        get() = mLayersShaderField.get(this) as? Shader
        set(value) = mLayersShaderField.set(this, value)

    private val mCanvas: Canvas?
        get() = mCanvasField.get(this) as? Canvas

    private val mLayersBitmap: Bitmap?
        get() = mLayersBitmapField.get(this) as? Bitmap

    private val mPaint: Paint?
        get() = mPaintField.get(this) as? Paint

    private val mMaskScaleOnly: Path?
        get() = mMaskScaleOnlyField.get(this) as? Path

    /**
     * 继承修改自 AdaptiveIconDrawable.draw：
     * 用透明色清空画布，避免背景图层把主题图标盖住。
     */
    override fun draw(canvas: Canvas) {
        if (mLayersBitmap == null) return
        if (mLayersShader == null) {
            mCanvas?.setBitmap(mLayersBitmap)
            mCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            background?.draw(mCanvas!!)

            foreground?.setBounds(0, 0, bounds.width(), bounds.height())
            foreground?.draw(mCanvas!!)

            mLayersShader = BitmapShader(
                mLayersBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
            )
            mPaint?.setShader(mLayersShader)
        }
        if (mMaskScaleOnly != null) {
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            canvas.drawPath(mMaskScaleOnly!!, mPaint!!)
            canvas.translate(-bounds.left.toFloat(), -bounds.top.toFloat())
        }
    }
}

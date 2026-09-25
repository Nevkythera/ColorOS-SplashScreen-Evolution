package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SplashMedia
import kotlin.math.max

/**
 * 媒体裁切视图：拖动平移、双指缩放、按钮 90° 旋转；图像始终铺满视图（不露底）。
 *
 * 持有 [Drawable]（静态图 / GIF 均可，GIF 会随视图自动播放）。视图本身就是「满屏取景框」，
 * 所见即所得：
 *  - [bake] 把当前显示区域截成位图（静态图用）；
 *  - [transform] 导出与尺寸无关的归一化裁切参数（GIF / 视频用，由 Hook 侧重放）。
 */
class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var drawable: Drawable? = null

    private var rotationDeg = 0
    private var scale = 1f
    private var minScale = 1f
    private var maxScale = 1f
    private var tx = 0f
    private var ty = 0f

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
                clamp()
                invalidate()
                return true
            }
        }
    )

    fun setDrawable(d: Drawable) {
        // 让 Drawable 能以本 View 为回调自动重绘（GIF 动画需要）。
        d.callback = this
        drawable = d
        if (d is AnimatedImageDrawable) {
            d.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            d.start()
        }
        rotationDeg = 0
        resetTransform()
        invalidate()
    }

    /** 顺时针旋转 90°（重置为铺满并居中）。 */
    fun rotate() {
        if (drawable == null) return
        rotationDeg = (rotationDeg + 90) % 360
        resetTransform()
        invalidate()
    }

    private fun srcW(): Int = drawable?.intrinsicWidth ?: 0
    private fun srcH(): Int = drawable?.intrinsicHeight ?: 0

    private fun effWidth(): Float =
        if (rotationDeg % 180 == 0) srcW().toFloat() else srcH().toFloat()

    private fun effHeight(): Float =
        if (rotationDeg % 180 == 0) srcH().toFloat() else srcW().toFloat()

    private fun resetTransform() {
        if (drawable == null || width == 0 || height == 0) return
        minScale = max(width / effWidth(), height / effHeight())
        maxScale = minScale * 5f
        scale = minScale
        tx = 0f
        ty = 0f
        clamp()
    }

    /** 约束平移，使图像始终覆盖视图（不露黑边）。 */
    private fun clamp() {
        val ew = effWidth() * scale
        val eh = effHeight() * scale
        val maxTx = max(0f, (ew - width) / 2f)
        val maxTy = max(0f, (eh - height) / 2f)
        tx = tx.coerceIn(-maxTx, maxTx)
        ty = ty.coerceIn(-maxTy, maxTy)
    }

    private fun matrixFor(w: Int, h: Int): Matrix = Matrix().apply {
        postTranslate(-srcW() / 2f, -srcH() / 2f)
        if (rotationDeg != 0) postRotate(rotationDeg.toFloat())
        postScale(scale, scale)
        postTranslate(w / 2f + tx, h / 2f + ty)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetTransform()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = drawable ?: return
        val save = canvas.save()
        canvas.concat(matrixFor(width, height))
        d.setBounds(0, 0, srcW(), srcH())
        d.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                dragging = true
            }

            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragging && !scaleDetector.isInProgress) {
                    tx += event.x - lastX
                    ty += event.y - lastY
                    lastX = event.x
                    lastY = event.y
                    clamp()
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }

    /** 把当前显示区域截成位图（所见即所得，静态图用）。 */
    fun bake(): Bitmap {
        val out = Bitmap.createBitmap(
            width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888
        )
        draw(Canvas(out))
        return out
    }

    /** 导出归一化裁切参数（GIF / 视频用）。 */
    fun transform(): SplashMedia.Transform = SplashMedia.Transform(
        rotation = rotationDeg,
        scaleRatio = if (minScale > 0f) scale / minScale else 1f,
        txFrac = if (width > 0) tx / width else 0f,
        tyFrac = if (height > 0) ty / height else 0f,
        iw = srcW(),
        ih = srcH()
    )
}

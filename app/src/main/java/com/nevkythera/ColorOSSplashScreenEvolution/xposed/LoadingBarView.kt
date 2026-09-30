package com.Nevkythera.ColorOSSplashScreenEvolution.xposed

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator

/**
 * 线性加载条（不确定型）—— 照搬 Material3 `LinearProgressIndicator`（非确定型）的动画。
 *
 * 与安装器 InstallerX 的进度条同一实现（它用的是 `LinearWavyProgressIndicator(amplitude = 0f)`，
 * 压平后就是这个）。规格摘自 androidx material3：
 *   - 周期 1750ms；四个端点各自跑 keyframes：
 *       首段 head：0→1，[0,1000]；   首段 tail：0→1，[250,1250]
 *       次段 head：0→1，[650,1500]； 次段 tail：0→1，[900,1750]
 *     缓动 = cubic-bezier(0.3, 0, 0.8, 0.15)（EasingEmphasizedAccelerate）。
 *   - 画 5 段：轨道-首线前 / 首线 / 轨道-两线间 / 次线 / 轨道-次线后；
 *     线下与轨道之间留 `gapSize`（4dp）+ 线宽 的间隙。
 *   - 线帽 Round（因此线段两端各外扩半个线宽，这正是 gap 额外加一个线宽的原因）。
 */
class LoadingBarView(context: Context) : View(context) {

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val gapPx = 4f * resources.displayMetrics.density

    private var startAtMs = 0L

    private val ticker: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = CYCLE_MS
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    fun setTint(color: Int) {
        indicatorPaint.color = color
        // 轨道用同色低透明度。
        trackPaint.color = (0x33 shl 24) or (color and 0x00FFFFFF)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAtMs = SystemClock.uptimeMillis()
        if (!ticker.isStarted) ticker.start()
    }

    override fun onDetachedFromWindow() {
        runCatching { ticker.cancel() }
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        indicatorPaint.strokeWidth = h
        trackPaint.strokeWidth = h

        val t = ((SystemClock.uptimeMillis() - startAtMs) % CYCLE_MS).toFloat()
        //   把区间从 [0,1] 映射到 [-ENTER,1]：让指示段从**左侧外部滑入**，
        //   而不是在左边缘凭空长出来。
        val head1 = enter(value(t, 0f, 1000f))
        val tail1 = enter(value(t, 250f, 1250f))
        val head2 = enter(value(t, 650f, 1500f))
        val tail2 = enter(value(t, 900f, 1750f))

        // Round cap 会让线段两端各外扩 h/2，因此间隙要多加一个线宽。
        val gap = (gapPx + h) / w

        // 首线前的轨道
        if (head1 < 1f - gap) {
            val start = if (head1 > 0f) head1 + gap else 0f
            drawSegment(canvas, w, h, start, 1f, trackPaint)
        }
        // 首线
        if (head1 - tail1 > 0f) {
            drawSegment(canvas, w, h, head1, tail1, indicatorPaint)
        }
        // 两线间的轨道
        if (tail1 > gap) {
            val start = if (head2 > 0f) head2 + gap else 0f
            val end = if (tail1 < 1f) tail1 - gap else 1f
            drawSegment(canvas, w, h, start, end, trackPaint)
        }
        // 次线
        if (head2 - tail2 > 0f) {
            drawSegment(canvas, w, h, head2, tail2, indicatorPaint)
        }
        // 次线后的轨道
        if (tail2 > gap) {
            val end = if (tail2 < 1f) tail2 - gap else 1f
            drawSegment(canvas, w, h, 0f, end, trackPaint)
        }
    }

    /** 画一段：[start]、[end] 为轨道宽度比例（与 M3 一致，不强制先后顺序）。 */
    private fun drawSegment(
        canvas: Canvas,
        w: Float,
        h: Float,
        start: Float,
        end: Float,
        paint: Paint
    ) {
        canvas.drawLine(start * w, h / 2f, end * w, h / 2f, paint)
    }

    /** 端点值：t 落在 [from, to] 内按缓动插值，否则取 0/1。 */
    private fun value(t: Float, from: Float, to: Float): Float = when {
        t <= from -> 0f
        t >= to -> 1f
        else -> EASE.getInterpolation((t - from) / (to - from))
    }

    /** 把 [0,1] 映射到 [-ENTER,1]。 */
    private fun enter(f: Float): Float = -ENTER + f * (1f + ENTER)

    private companion object {
        const val CYCLE_MS = 1750L

        /** 指示段从左侧外部滑入的横向占比。 */
        const val ENTER = 0.15f

        /** EasingEmphasizedAccelerateCubicBezier = cubic-bezier(0.3, 0, 0.8, 0.15)。 */
        val EASE = PathInterpolator(0.3f, 0.0f, 0.8f, 0.15f)
    }
}

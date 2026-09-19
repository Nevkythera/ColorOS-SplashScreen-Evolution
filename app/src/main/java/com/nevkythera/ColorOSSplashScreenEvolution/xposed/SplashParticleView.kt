package com.Nevkythera.ColorOSSplashScreenEvolution.xposed

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min
import kotlin.random.Random


class SplashParticleView(
    context: Context,
    private val source: Bitmap,
    captureScale: Int,
    private val durationMs: Int,
    /** true = 粒子消散（渐进，逐列）；false = 粒子扩散（全部同时飞散）。 */
    private val progressive: Boolean,
    /** 单个粒子自身的运动时长（毫秒，100~2000）。 */
    particleTimeMs: Int,
    private val onFinish: () -> Unit
) : View(context) {

    private val density = resources.displayMetrics.density

    /** 每颗粒子画的是**源图的一小块**（碎片）；关掉插值 → 像素级清晰。 */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
    }
    private val lifeCapMs = particleTimeMs.coerceIn(100, 2000).toFloat()
    /** 总时长 = 扫描时长 + 粒子自身时长（两者互不干扰）。 */
    private val totalMs: Long =
        (if (progressive) durationMs.toLong() else 0L) + lifeCapMs.toLong()

    /** 消散模式的底图：未被扫到的区域仍显示原内容。 */
    private val basePaint = Paint().apply { isFilterBitmap = captureScale > 1 }
    private val srcRect = Rect()
    private val dstRect = RectF()
    private var srcW = source.width
    private var fragSize = 1
    private var perPx = BASE_PER_PX
    /** 消散模式：每一**行**的「开始溶解」时刻（ms）；无随机 → 边界连贯。扫描方向：上→下。 */
    private var rowDueMs = FloatArray(0)

    // ---- 粒子（SOA，构造期一次性分配）----
    private var count = 0
    private var x0 = FloatArray(0)
    private var y0 = FloatArray(0)
    private var tx = FloatArray(0)
    private var ty = FloatArray(0)
    private var vY = FloatArray(0)
    private var r0 = FloatArray(0)
    /** 每颗粒子的起始延迟（ms）：消散模式按列递增（左早右晚）。 */
    private var delayMs = FloatArray(0)
    /** 每颗粒子的运动/淡出时长（ms），从 [delayMs] 起算。 */
    private var lifeMs = FloatArray(0)
    private var sx = IntArray(0)
    private var sy = IntArray(0)
    private var alpha0 = IntArray(0)

    private var centerX = 0f
    private var centerY = 0f

    private var animStartAt = 0L
    private var finished = false

    private val ticker: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = totalMs
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (!finished) {
                    finished = true
                    invalidate()
                    runCatching { onFinish() }
                }
            }
        })
    }

    init {
        buildParticles(source, captureScale)
    }

    private fun buildParticles(src: Bitmap, captureScale: Int) {
        val sw = src.width
        val sh = src.height
        if (sw <= 0 || sh <= 0) return
        centerX = sw / 2f
        centerY = sh / 2f

        // 官方 perPx=6；动态增大直到粒子数不超过上限。
        while (perPx < 64 && (sw / perPx) * (sh / perPx) > MAX_PARTICLES) perPx++

        val pixels = IntArray(sw * sh)
        src.getPixels(pixels, 0, sw, 0, 0, sw, sh)

        val maxLine = (sw / perPx).coerceAtLeast(1)
        val maxRow = (sh / perPx).coerceAtLeast(1)
        val cap = (maxLine + 2) * ((sh / perPx) + 2)
        val xs = FloatArray(cap); val ys = FloatArray(cap)
        val txs = FloatArray(cap); val tys = FloatArray(cap)
        val vys = FloatArray(cap); val r0s = FloatArray(cap)
        val delays = FloatArray(cap); val lifes = FloatArray(cap)
        val sxs = IntArray(cap); val sys = IntArray(cap); val alphas = IntArray(cap)
        var n = 0

        val rnd = Random(SystemClock.uptimeMillis())
        // 每行的溶解时刻（线性、无随机）→ 上→下扫描，边界连贯无竖线
        rowDueMs = FloatArray(maxRow + 1)
        for (r in 0..maxRow) {
            rowDueMs[r] = durationMs * (r.toFloat() / maxRow)
        }
        val half = perPx / 2
        var gy = half
        while (gy < sh) {
            val rowBase = gy * sw
            var gx = half
            while (gx < sw) {
                val c = pixels[rowBase + gx]
                if (Color.alpha(c) > 10) {
                    // 消散模式：按列给「起始延迟」（左列立即、右列最晚）→ 波从左扫到右；
                    // 扩散模式：全部立即开始。
                    val delay = if (progressive) {
                        rowDueMs[(gy / perPx).coerceIn(0, maxRow)]
                    } else {
                        0f
                    }
                    // 单个粒子的运动时长 = 「粒子速度」设置（毫秒），不再被扫描时长截断
                    val life = lifeCapMs.coerceAtLeast(1f)

                    xs[n] = gx.toFloat()
                    ys[n] = gy.toFloat()
                    if (progressive) {
                        // 消散：以上方为主的小幅偏移，由 −(f·vY)² 主导 → 由慢到快
                        txs[n] = smallOffset(rnd, 10, 26)
                        tys[n] = smallOffset(rnd, 4, 14)
                        vys[n] = (dp(5f) + rnd.nextInt(dp(8f).coerceAtLeast(1))).toFloat()
                    } else {
                        // 扩散：大爆炸——方向 360° 均匀随机，位移幅度很大
                        val ang = rnd.nextFloat() * TWO_PI
                        val rr = dp(220f) + rnd.nextInt(dp(380f).coerceAtLeast(1))
                        txs[n] = kotlin.math.cos(ang) * rr
                        tys[n] = kotlin.math.sin(ang) * rr
                        vys[n] = 0f
                    }
                    r0s[n] = perPx / 2f * (0.85f + rnd.nextFloat() * 0.3f)
                    lifes[n] = life
                    delays[n] = delay
                    sxs[n] = gx
                    sys[n] = gy
                    alphas[n] = min(Color.alpha(c), (ALPHA_CHOICES[rnd.nextInt(3)] * 255f).toInt())
                    n++
                }
                gx += perPx
            }
            gy += perPx
        }

        count = n
        x0 = xs.copyOf(n); y0 = ys.copyOf(n)
        tx = txs.copyOf(n); ty = tys.copyOf(n)
        vY = vys.copyOf(n); r0 = r0s.copyOf(n)
        lifeMs = lifes.copyOf(n)
        delayMs = delays.copyOf(n)
        sx = sxs.copyOf(n); sy = sys.copyOf(n); alpha0 = alphas.copyOf(n)
        fragSize = perPx
    }

    /** 小幅度、无方向偏置的随机偏移：±(baseDp + rand(spanDp))。 */
    private fun smallOffset(rnd: Random, baseDp: Int, spanDp: Int): Float {
        val v = (dp(baseDp.toFloat()) + rnd.nextInt(dp(spanDp.toFloat()).coerceAtLeast(1))).toFloat()
        return if (rnd.nextBoolean()) -v else v
    }

    private fun dp(v: Float): Int = (density * v).toInt()

    fun start() {
        animStartAt = SystemClock.uptimeMillis()
        if (!ticker.isStarted) ticker.start()
    }

    override fun onDetachedFromWindow() {
        runCatching { ticker.cancel() }
        runCatching { if (!source.isRecycled) source.recycle() }
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        if (count <= 0 || source.isRecycled) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val elapsed = (SystemClock.uptimeMillis() - animStartAt).coerceAtLeast(0L).toFloat()

        // ---- 消散模式：未被扫到的**行**仍显示原内容（上→下扫描）----
        if (progressive && !source.isRecycled && rowDueMs.isNotEmpty()) {
            val ky = h / source.height
            for (r in rowDueMs.indices) {
                if (elapsed >= rowDueMs[r]) continue   // 该行已溶解
                val t = (r * perPx).coerceAtMost(source.height)
                val b = ((r + 1) * perPx).coerceAtMost(source.height)
                if (b <= t) continue
                srcRect.set(0, t, srcW, b)
                dstRect.set(0f, t * ky, w, b * ky)
                canvas.drawBitmap(source, srcRect, dstRect, basePaint)
            }
        }

        for (i in 0 until count) {
            val raw = (elapsed - delayMs[i]) / lifeMs[i]
            // 还没轮到的区域（raw<0）交给底图，**不渲染粒子**；已消亡的也跳过
            if (raw < 0f || raw >= 1f) continue
            val f = raw
            val x = x0[i] + (x0[i] - centerX) / centerX * tx[i] * f
            val y = y0[i] + (y0[i] - centerY) / centerY * ty[i] * f - (f * vY[i]) * (f * vY[i])
            val r = (r0[i] * (1f - f)).coerceAtLeast(0.5f)
            val a = alpha0[i] * min(1.2f - f, 1f)
            if (a <= 1f) continue
            // 画源图的一小块（碎片），而不是纯色圆点 → 清晰
            val l = sx[i]
            val t = sy[i]
            srcRect.set(
                l, t,
                (l + fragSize).coerceAtMost(source.width),
                (t + fragSize).coerceAtMost(source.height)
            )
            dstRect.set(x - r, y - r, x + r, y + r)
            paint.alpha = a.toInt().coerceIn(0, 255)
            canvas.drawBitmap(source, srcRect, dstRect, paint)
        }
    }

    private companion object {
        /** 粒子数上限（限制逐粒子绘制的开销）。 */
        const val MAX_PARTICLES = 24_000

        /** 官方默认网格步长（px）。 */
        const val BASE_PER_PX = 6

        // 扩散模式的爆炸幅度（px）。
        const val TWO_PI = 6.2831855f

        /** 官方初始 alpha 取值。 */
        val ALPHA_CHOICES = floatArrayOf(0.3f, 0.6f, 1.0f)
    }
}

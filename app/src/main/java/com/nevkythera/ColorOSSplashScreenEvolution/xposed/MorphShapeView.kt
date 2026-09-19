package com.Nevkythera.ColorOSSplashScreenEvolution.xposed

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.min


class MorphShapeView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val path = Path()

    /** 动画起始时刻（uptimeMillis），所有状态都相对它计算。 */
    private var startAtMs: Long = 0L

    /** 首帧尺寸自检日志只打一次。 */
    private var sizeLogged = false

    /**
     * 心跳动画：唯一职责是每帧触发 [invalidate]。
     * 时长取官方的 GlobalRotationDurationMillis，线性、无限循环。
     */
    private val ticker: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = GLOBAL_ROTATION_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    /** 设置色块颜色（含 alpha）。 */
    fun setTint(color: Int) {
        paint.color = color
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

        val cx = w / 2f
        val cy = h / 2f

        val elapsed = SystemClock.uptimeMillis() - startAtMs
        val step = elapsed / MORPH_INTERVAL_MS

        // 形变进度：官方用 spring(0.6f, 200f)，这里用等价的欠阻尼阶跃响应
        val localMs = (elapsed % MORPH_INTERVAL_MS).toFloat()
        val raw = springEase(localMs / 1000f)
        // 形状插值必须夹在 0..1；旋转项用未夹取的 raw 让过冲表现为回弹。
        val morphT = raw.coerceIn(0f, 1f)

        val idx = (step % MORPHS.size).toInt()
        val morph = MORPHS[idx]

        // 官方：globalRotation 0→360 匀速 4666ms 一圈；每完成一次形变 +90°
        val globalRotation =
            (elapsed % GLOBAL_ROTATION_DURATION_MS).toFloat() / GLOBAL_ROTATION_DURATION_MS * FULL_ROTATION
        val morphRotationTarget = ((step % 4) * QUARTER_ROTATION).toFloat()
        val rotation = raw * QUARTER_ROTATION + morphRotationTarget + globalRotation

        // 用官方 Morph 得到当前插值形状，写入 Path
        val baseSize = min(w, h) * 0.5f
        val cubics = morph.asCubics(morphT)
        //   任何时刻质心都精确等于旋转轴 → 旋转绝对稳定，五边形不再甩，
        //   也不存在预计算质心与 Morph 匹配后实际质心不一致的残差
        //   （实测 Pill/Cookie4 用 raw 质心会残差 1~1.5px）。
        val cen = centroidOf(cubics)
        buildPath(cubics, ccx = cen[0], ccy = cen[1], baseSize = baseSize, cx = cx, cy = cy)

        // 首帧自检：视图若被父容器意外拉伸成全屏，形状会随之变巨大 —— 靠这行日志定位
        if (!sizeLogged) {
            sizeLogged = true
            runCatching {
                android.util.Log.d(
                    "CSE",
                    "morph_shape draw view=${w}x${h} baseSize=$baseSize " +
                        "center=($cx, $cy)"
                )
            }
        }

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    /**
     * shoelace 质心：把闭合曲线的 anchor0→anchor1 段组成多边形后按面积加权积分。
     * anchor 点是曲线真实经过的点，用它们近似质心足够精确（曲线本身在 anchor 的凸包内）。
     */
    private fun centroidOf(cubics: List<Cubic>): FloatArray {
        var area = 0f
        var gx = 0f
        var gy = 0f
        for (c in cubics) {
            val x0 = c.anchor0X
            val y0 = c.anchor0Y
            val x1 = c.anchor1X
            val y1 = c.anchor1Y
            val cross = x0 * y1 - x1 * y0
            area += cross
            gx += (x0 + x1) * cross
            gy += (y0 + y1) * cross
        }
        area /= 2f
        // 退化（面积≈0 或非有限）时退回形状包围盒中心，避免除 0 / NaN
        if (!area.isFinite() || kotlin.math.abs(area) < 1e-9f) {
            val b = SHAPE_BOUNDS
            return floatArrayOf((b[0] + b[2]) / 2f, (b[1] + b[3]) / 2f)
        }
        val cx = gx / (6f * area)
        val cy = gy / (6f * area)
        if (!cx.isFinite() || !cy.isFinite()) {
            val b = SHAPE_BOUNDS
            return floatArrayOf((b[0] + b[2]) / 2f, (b[1] + b[3]) / 2f)
        }
        return floatArrayOf(cx, cy)
    }

    
    private fun buildPath(
        cubics: List<Cubic>,
        ccx: Float,
        ccy: Float,
        baseSize: Float,
        cx: Float,
        cy: Float
    ) {
        path.rewind()

        val span = maxOf(
            SHAPE_BOUNDS[2] - SHAPE_BOUNDS[0],
            SHAPE_BOUNDS[3] - SHAPE_BOUNDS[1],
            MIN_SPAN
        )
        val scale = baseSize / span

        var first = true
        for (c in cubics) {
            val x0 = (c.anchor0X - ccx) * scale + cx
            val y0 = (c.anchor0Y - ccy) * scale + cy
            val x1 = (c.control0X - ccx) * scale + cx
            val y1 = (c.control0Y - ccy) * scale + cy
            val x2 = (c.control1X - ccx) * scale + cx
            val y2 = (c.control1Y - ccy) * scale + cy
            val x3 = (c.anchor1X - ccx) * scale + cx
            val y3 = (c.anchor1Y - ccy) * scale + cy
            if (first) {
                path.moveTo(x0, y0)
                first = false
            }
            path.cubicTo(x1, y1, x2, y2, x3, y3)
        }
        path.close()
    }

    /**
     * 欠阻尼弹簧阶跃响应，对应 Compose `spring(dampingRatio=0.6f, stiffness=200f)`：
     *   f(t) = 1 - e^(-ζω₀t) · (cos(ω_d·t) + (ζω₀/ω_d)·sin(ω_d·t))
     * ω₀ = √(stiffness)，ω_d = ω₀√(1-ζ²)。t=0.65s 处残差 ≈ 0.4%，与官方
     * "spring will finish in less than 650ms" 一致。
     */
    private fun springEase(tSec: Float): Float {
        val w0 = kotlin.math.sqrt(SPRING_STIFFNESS)
        val zeta = SPRING_DAMPING_RATIO
        val wd = w0 * kotlin.math.sqrt(1f - zeta * zeta)
        val a = zeta * w0
        val decay = kotlin.math.exp(-a * tSec)
        return 1f - decay * (kotlin.math.cos(wd * tSec) + (a / wd) * kotlin.math.sin(wd * tSec))
    }

    private companion object {
        // ===================== 官方默认动画配置（LoadingIndicator.kt） =====================
        const val GLOBAL_ROTATION_DURATION_MS = 4666L
        const val MORPH_INTERVAL_MS = 650L
        const val SPRING_DAMPING_RATIO = 0.6f
        const val SPRING_STIFFNESS = 200f
        const val FULL_ROTATION = 360f
        const val QUARTER_ROTATION = 90f
        // =================================================================================

        // ===== 注意：以下属性按声明顺序初始化，被依赖的必须写在前面 =====

        /** 防止退化（所有点重合）时除以 0。 */
        const val MIN_SPAN = 1e-4f

        /** 包围盒采样点：0→1 均匀取样，覆盖整个形变过程。 */
        val BOUND_SAMPLES: FloatArray =
            floatArrayOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f)

        
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        val POLYGONS: List<RoundedPolygon> =
            LoadingIndicatorDefaults.IndeterminateIndicatorPolygons

        /**
         * 预建的形变对（含 wrap：最后一个→第一个），避免每帧 new Morph。
         * 官方 `morphSequence` 也是先 normalized 再 new Morph，这里保持一致。
         */
        val MORPHS: List<Morph> = POLYGONS.indices.map { i ->
            Morph(
                POLYGONS[i].normalized(),
                POLYGONS[(i + 1) % POLYGONS.size].normalized()
            )
        }

        
        val SHAPE_BOUNDS: FloatArray = measureUnifiedBounds(MORPHS)

        init {
            // 自检日志：真机上若形状仍偏心，凭这行就能判断是形状数据异常还是别的原因
            runCatching {
                val b = SHAPE_BOUNDS
                android.util.Log.d(
                    "CSE",
                    "morph_shape bounds=[${b[0]}, ${b[1]}, ${b[2]}, ${b[3]}] " +
                        "center=(${(b[0] + b[2]) / 2f}, ${(b[1] + b[3]) / 2f}) " +
                        "span=${maxOf(b[2] - b[0], b[3] - b[1])} morphs=${MORPHS.size}"
                )
            }
        }

        private fun measureUnifiedBounds(morphs: List<Morph>): FloatArray {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (morph in morphs) {
                for (p in BOUND_SAMPLES) {
                    for (c in morph.asCubics(p)) {
                        val ax0 = c.anchor0X
                        val ax1 = c.anchor1X
                        if (ax0 < minX) minX = ax0
                        if (ax1 < minX) minX = ax1
                        if (ax0 > maxX) maxX = ax0
                        if (ax1 > maxX) maxX = ax1
                        val ay0 = c.anchor0Y
                        val ay1 = c.anchor1Y
                        if (ay0 < minY) minY = ay0
                        if (ay1 < minY) minY = ay1
                        if (ay0 > maxY) maxY = ay0
                        if (ay1 > maxY) maxY = ay1
                    }
                }
            }
            // 采样失败 / 退化 / 数值异常时退回 [0,1]（官方形状实测就是 [0,1]），
            // 保证不会画出 NaN 或把形状甩到角落。
            val span = maxOf(maxX - minX, maxY - minY)
            val valid = minX < maxX && minY < maxY && span.isFinite() && span in 0.1f..10f
            if (!valid) return floatArrayOf(0f, 0f, 1f, 1f)
            return floatArrayOf(minX, minY, maxX, maxY)
        }
    }
}

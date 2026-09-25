package com.Nevkythera.ColorOSSplashScreenEvolution.util

import android.graphics.Matrix

/**
 * 自定义背景媒体的类型与「裁切变换」。
 *
 * 静态图片可以直接把裁切结果烘成 PNG；但 GIF / MP4 无法逐帧重编码，必须
 * **原样保存媒体**并记录一份与尺寸无关的**归一化变换**，由 Hook 侧按同一套
 * 矩阵参数在目标视图上重放 —— 这样才能既保住动画/视频，又保留用户裁切。
 */
object SplashMedia {

    const val KIND_IMAGE = "image"
    const val KIND_GIF = "gif"
    const val KIND_VIDEO = "video"

    /**
     * 归一化裁切变换。
     *
     * - [rotation]   旋转角度（0/90/180/270）
     * - [scaleRatio] 当前缩放 / 铺满基准缩放（>=1）
     * - [txFrac]     水平位移 / 视图宽度
     * - [tyFrac]     垂直位移 / 视图高度
     * - [iw]/[ih]    源内容原始像素尺寸（图片/GIF 的固有尺寸，或视频分辨率）
     */
    data class Transform(
        val rotation: Int,
        val scaleRatio: Float,
        val txFrac: Float,
        val tyFrac: Float,
        val iw: Int,
        val ih: Int
    ) {
        fun encode(): String = "$rotation|$scaleRatio|$txFrac|$tyFrac|$iw|$ih"

        companion object {
            fun decode(raw: String?): Transform? {
                if (raw.isNullOrBlank()) return null
                val p = raw.split("|")
                if (p.size != 6) return null
                return runCatching {
                    Transform(p[0].toInt(), p[1].toFloat(), p[2].toFloat(), p[3].toFloat(), p[4].toInt(), p[5].toInt())
                }.getOrNull()
            }
        }
    }

    /** 由归一化参数重算某个视图尺寸下的绘制矩阵（与裁切视图的算法保持一致）。 */
    fun matrix(t: Transform, viewW: Int, viewH: Int): Matrix {
        val effW = if (t.rotation % 180 == 0) t.iw.toFloat() else t.ih.toFloat()
        val effH = if (t.rotation % 180 == 0) t.ih.toFloat() else t.iw.toFloat()
        val minScale = if (effW > 0f && effH > 0f) maxOf(viewW / effW, viewH / effH) else 1f
        val scale = t.scaleRatio * minScale
        val tx = t.txFrac * viewW
        val ty = t.tyFrac * viewH
        return Matrix().apply {
            postTranslate(-t.iw / 2f, -t.ih / 2f)
            if (t.rotation != 0) postRotate(t.rotation.toFloat())
            postScale(scale, scale)
            postTranslate(viewW / 2f + tx, viewH / 2f + ty)
        }
    }
}

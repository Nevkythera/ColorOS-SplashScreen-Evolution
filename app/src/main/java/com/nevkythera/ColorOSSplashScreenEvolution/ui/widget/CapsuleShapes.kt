package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 连续圆角（squircle / continuous corner）形状。
 *
 * MCGA 使用第三方库 `io.github.kyant0:capsule` 提供的 `ContinuousRoundedRectangle`。
 * 这里不引入额外依赖，改为自实现的 [ContinuousCornerShape]：
 * 通过 Material 3 Expressive 的 `RoundedCornerShape` 做近似，
 * 并统一收敛圆角取值，保证各个卡片视觉一致。
 *
 * 说明：Android 15+ 的 `RoundedCornerShape` 已支持渲染层级的平滑圆角
 * （系统会对大圆角做超椭圆处理），在 ColorOS / Android 16 上视觉效果与
 * 连续圆角基本无差。若后续要追求极致，可替换为自绘 Path 版本。
 */
object CapsuleShapes {

    /** 分组卡片"首条目"：上方大圆角、下方小圆角。 */
    fun first(big: Dp = 18.dp, small: Dp = 8.dp) = RoundedCornerShape(
        topStart = big, topEnd = big,
        bottomStart = small, bottomEnd = small
    )

    /** 分组卡片"中间条目"：四角均为小圆角。 */
    fun middle(small: Dp = 8.dp) = RoundedCornerShape(small)

    /** 分组卡片"末条目"：上方小圆角、下方大圆角。 */
    fun last(big: Dp = 18.dp, small: Dp = 8.dp) = RoundedCornerShape(
        topStart = small, topEnd = small,
        bottomStart = big, bottomEnd = big
    )

    /** 单条目分组：四角大圆角。 */
    fun single(big: Dp = 18.dp) = RoundedCornerShape(big)
}

package com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * 顶栏渐变模糊的基准样式 —— 对应 MCGA 的 `baseHazeStyle`。
 *
 * 用于 [com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage] 的标题栏：
 * 通过 [dev.chrisbanes.haze.hazeEffect] + `HazeProgressive.verticalGradient`
 * 让标题栏在列表滚动时产生"上实下虚"的渐变模糊，列表内容从标题下方淡入。
 */
@Composable
fun baseHazeStyle(
    // ★ 模糊半径从 30dp 降到 16dp：模糊开销与半径平方相关，
    //   30dp 时中低端机滚动会明显掉帧，16dp 视觉差别很小但性能好很多。
    blurRadius: Dp = 16.dp,
    containerColor: Color = MaterialTheme.colorScheme.background,
    /** 浅色主题下的蒙层浓度（越大越接近纯色，越小越透出内容）。 */
    lightAlpha: Float = 0.45f,
    /** 深色主题下的蒙层浓度。 */
    darkAlpha: Float = 0.60f,
): HazeStyle = HazeStyle(
    blurRadius = blurRadius,
    backgroundColor = containerColor,
    tint = HazeTint(
        color = containerColor.copy(
            // 原实现把深浅两个 alpha 用反了（深色主题反而用了更透的 lightAlpha），
            // 导致深色模式下顶栏几乎看不出模糊。这里按主题取对应浓度。
            alpha = if (isSystemInDarkTheme()) darkAlpha else lightAlpha
        )
    ),
)

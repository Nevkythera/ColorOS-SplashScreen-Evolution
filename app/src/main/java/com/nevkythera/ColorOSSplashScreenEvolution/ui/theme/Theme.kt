package com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * CSE 应用主题 —— Material 3 Expressive (MD3E)。
 *
 * 本次迁移要点（用户要求"将 md3 迁移至 md3e"）：
 *  - 使用 [MaterialExpressiveTheme] 而非普通 `MaterialTheme`，
 *    它会同时接管"配色 + 排版 + 形状 + 动效"四套 token；
 *  - 显式传入 [MotionScheme.expressive] —— 这是 MD3E 的弹性动效方案
 *    （弹簧式回弹、更长的持续时间曲线），标准 M3 的动效呈现不出 Expressive 的手感；
 *  - 优先使用系统动态取色（Material You），保证与用户壁纸 / ColorOS 主题一致；
 *    取不到时回落到 [expressiveLightColorScheme]。
 *
 * 注意：`material3:1.4.0-alpha16` 只提供 [expressiveLightColorScheme]，
 * 没有 `expressiveDarkColorScheme()`。因此暗色分支传 null，
 * 由 [MaterialExpressiveTheme] 依据 [isSystemInDarkTheme] 自行选择暗色基线。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        // Android 12+ 动态取色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> null
        else -> expressiveLightColorScheme()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

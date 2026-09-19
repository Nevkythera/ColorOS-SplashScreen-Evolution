package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 拖拽条（Slider）设置项 —— 与 [SwitchWidget] / [ChoiceWidget] 同一套视觉规范。
 *
 * 用于「指示器大小」这类连续值配置：
 *   左侧图标 + 标题 + 说明文字，下方一条拖拽条，右端显示当前值文本。
 *
 * 置灰（[enabled] = false）时整块按 0.38 透明度压暗，且拖拽条不可拖动。
 */
@Composable
fun SliderWidget(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    description: String? = null,
    enabled: Boolean = true,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    //   （如 "1.20×"），不会争抢宽度，所以这里不需要上限逻辑。
                    //     标题固定为「指示器大小 / Indicator size」，无需截断；
                    //     设了反而会在某些 ROM 的大字号 / 无障碍缩放下丢掉文字。
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = valueText,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
                description?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            modifier = Modifier.weight(1f),
            enabled = enabled,
            value = value,
            valueRange = valueRange,
            steps = steps,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}

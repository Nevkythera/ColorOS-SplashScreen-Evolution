package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R

/**
 * 纯展示 / 可点击的设置项 —— 对应 MCGA 的 `OptionWidget`。
 *
 * 主要用于"Root 管理器"、"Root 管理器版本"这类**只读信息行**：
 * 左侧图标 + 标题 + 说明文字，右侧一个小箭头。
 *
 * 规范要点（照 MCGA 迁移）：
 *  - 箭头用 `arrow_forward_ios` 矢量、12dp（**不是** Material 的
 *    `KeyboardArrowRight` 24dp —— 后者视觉上过大过粗）；
 *  - 图标 24dp，`onSurfaceVariant`；
 *  - [isError] 为 true 时说明文字用 error 色（用于"未检测到"这类异常态）；
 *  - [tintIcon] 置 false 时不染色，用于彩色图标（如作者头像）。
 */
@Composable
fun OptionWidget(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    description: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    tintIcon: Boolean = true
) {
    // 置灰时整体降低透明度，与 SwitchWidget 的禁用视觉一致
    val contentAlpha = if (enabled) 1f else 0.38f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (tintIcon) MaterialTheme.colorScheme.onSurfaceVariant
                else Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            Column {
                // ★ 与 SwitchWidget 同理：右侧只有 12dp 箭头，标题区宽度确定，
                //   不设 maxLines 让它自然折行（避免长英文标题被截断）。
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    style = MaterialTheme.typography.titleMedium
                )
                description?.let {
                    Text(
                        text = it,
                        color = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.arrow_forward_ios),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            modifier = Modifier.size(12.dp)
        )
    }
}

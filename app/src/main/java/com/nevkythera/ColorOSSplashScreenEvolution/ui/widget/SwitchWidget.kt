package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import android.view.HapticFeedbackConstants
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R

/**
 * 带开关的设置项 —— 对应 MCGA 的 `SwitchWidget`。
 *
 * 规范要点（照 MCGA 迁移）：
 *  - 整行可点击，点标题区域也能切换（而不是只有 Switch 本体可点）；
 *  - 图标 24dp，`onSurfaceVariant` 着色；
 *  - 标题 `titleMedium`，副标题 `bodyMedium`；
 *  - Switch 的滑块内带勾/叉图标（M3E 的 thumbContent），这是 MD3E 标志性细节；
 *  - 内边距 16dp，图文间距 16dp。
 */
@Composable
fun SwitchWidget(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    description: String? = null,
    enabled: Boolean = true
) {
    // 置灰时整体降低透明度
    val contentAlpha = if (enabled) 1f else 0.38f
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                // 清脆、强而极短的触感反馈
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onCheckedChange(!checked)
            }
            .padding(16.dp),
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
                //   这里的宽度是确定的（右侧 Switch 宽度固定，不参与争抢），
                //   所以文字只会"该几行就几行"，不会出现 ChoiceWidget 那种
                //   被压成竖排单字的情况。
                //   英文标题最长的是 "Force the use of the native Android
                //   SplashScreen API"（约 364dp > 可用 213dp），
                //   一旦设 maxLines = 1 就会截断成 "...SplashScreen A…"，
                //   用户读不完整 —— 折行反而更合适。
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    style = MaterialTheme.typography.titleMedium
                )
                description?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // MD3E 风格开关：滑块内显示勾/叉
        Switch(
            enabled = enabled,
            checked = checked,
            thumbContent = {
                Icon(
                    painter = painterResource(id = if (checked) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            },
            onCheckedChange = null
        )
    }
}

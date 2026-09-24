package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

private const val ACTION_NONE = ""
private const val ACTION_SYSTEMUI = "systemui"
private const val ACTION_REBOOT = "reboot"

/**
 * 重启菜单（页面内浮层）：同窗口才能用 haze 模糊页面内容。
 * 不加黑色遮罩（那会把页面压黑、看不出模糊）；点击空白处关闭。
 * 二级（选项）→ 三级（确认）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestartOverlay(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onRestartSystemUi: () -> Unit,
    onRebootSystem: () -> Unit
) {
    var pending by remember { mutableStateOf(ACTION_NONE) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .hazeEffect(state = hazeState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.restart_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (pending == ACTION_NONE) {
                    RestartOverlayRow(
                        iconRes = R.drawable.ic_restart_systemui,
                        label = stringResource(R.string.restart_systemui),
                        onClick = { pending = ACTION_SYSTEMUI }
                    )
                    RestartOverlayRow(
                        iconRes = R.drawable.ic_restart_device,
                        label = stringResource(R.string.restart_device),
                        onClick = { pending = ACTION_REBOOT }
                    )
                } else {
                    val isReboot = pending == ACTION_REBOOT
                    Text(
                        text = stringResource(
                            if (isReboot) R.string.reboot_confirm_message
                            else R.string.restart_confirm_message
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { pending = ACTION_NONE }) {
                            Text(stringResource(R.string.dialog_cancel))
                        }
                        TextButton(
                            onClick = {
                                pending = ACTION_NONE
                                onDismiss()
                                if (isReboot) onRebootSystem() else onRestartSystemUi()
                            }
                        ) {
                            Text(stringResource(R.string.dialog_confirm))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestartOverlayRow(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(text = label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

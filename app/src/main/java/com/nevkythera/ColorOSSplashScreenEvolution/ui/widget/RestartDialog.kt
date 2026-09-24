package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import android.os.Build
import com.Nevkythera.ColorOSSplashScreenEvolution.R

private const val DIALOG_BLUR_RADIUS_PX = 64

private const val ACTION_NONE = ""
private const val ACTION_SYSTEMUI = "systemui"
private const val ACTION_REBOOT = "reboot"

/**
 * 统一的重启选项对话框：重启系统界面 / 重启系统。
 * 二级（选项）→ 三级（确认），所有页面共用，保证入口与交互一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestartDialog(
    onDismiss: () -> Unit,
    onRestartSystemUi: () -> Unit,
    onRebootSystem: () -> Unit
) {
    var pending by remember { mutableStateOf(ACTION_NONE) }

    // 对话框窗口的背景模糊（API 31+）：需要 BLUR_BEHIND 标志 + 模糊半径，两者缺一不可
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val view = LocalView.current
        LaunchedEffect(Unit) {
            runCatching {
                val w = (view.parent as? DialogWindowProvider)?.window ?: return@runCatching
                w.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                w.setBackgroundBlurRadius(DIALOG_BLUR_RADIUS_PX)
            }
        }
    }

    if (pending != ACTION_NONE) {
        val isReboot = pending == ACTION_REBOOT
        AlertDialog(
            onDismissRequest = { pending = ACTION_NONE },
            icon = {
                Icon(
                    painter = painterResource(
                        if (isReboot) R.drawable.ic_restart_device
                        else R.drawable.ic_restart_systemui
                    ),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.restart_title)) },
            text = {
                Text(
                    stringResource(
                        if (isReboot) R.string.reboot_confirm_message
                        else R.string.restart_confirm_message
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending = ACTION_NONE
                        onDismiss()
                        if (isReboot) onRebootSystem() else onRestartSystemUi()
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = ACTION_NONE }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.refresh),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.restart_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RestartRow(
                    iconRes = R.drawable.ic_restart_systemui,
                    label = stringResource(R.string.restart_systemui),
                    onClick = { pending = ACTION_SYSTEMUI }
                )
                RestartRow(
                    iconRes = R.drawable.ic_restart_device,
                    label = stringResource(R.string.restart_device),
                    onClick = { pending = ACTION_REBOOT }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestartRow(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

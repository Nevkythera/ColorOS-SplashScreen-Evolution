package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R

private const val ACTION_NONE = ""
private const val ACTION_SYSTEMUI = "systemui"
private const val ACTION_REBOOT = "reboot"

/** 浮层进出场动画时长（毫秒）。 */
private const val OVERLAY_ANIM_MS = 200

/** 黑色遮罩的最大不透明度。 */
private const val SCRIM_ALPHA = 0.5f

/**
 * 重启菜单（页面内浮层）：全屏黑色遮罩 + 居中卡片，点击空白处关闭。
 * 二级（选项）→ 三级（确认）带淡入淡出动画。
 *
 * 由宿主无条件组合并传入 [visible]：关闭时能播完整退出动画，且内部状态跨开关保留。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestartOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onRestartSystemUi: () -> Unit,
    onRebootSystem: () -> Unit
) {
    var pending by remember { mutableStateOf(ACTION_NONE) }

    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = OVERLAY_ANIM_MS, easing = FastOutSlowInEasing),
        label = "restart_overlay"
    )

    // 每次打开都回到「二级：选择重启对象」
    LaunchedEffect(visible) { if (visible) pending = ACTION_NONE }

    // 完全关闭（且动画归零）后不再渲染，避免全屏 Box 拦截触摸
    if (!visible && progress == 0f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA * progress))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = progress
                    val s = 0.85f + 0.15f * progress
                    scaleX = s
                    scaleY = s
                }
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

                AnimatedContent(
                    targetState = pending,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(160)) togetherWith
                            fadeOut(animationSpec = tween(100))
                    },
                    label = "restart_stage"
                ) { stage ->
                    if (stage == ACTION_NONE) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        }
                    } else {
                        val isReboot = stage == ACTION_REBOOT
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

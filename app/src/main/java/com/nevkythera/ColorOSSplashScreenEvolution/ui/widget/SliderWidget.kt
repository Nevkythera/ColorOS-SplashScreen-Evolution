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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R

/**
 * 拖拽条（Slider）设置项 —— 与 [SwitchWidget] / [ChoiceWidget] 同一套视觉规范。
 *
 * 用于连续值配置：左侧图标 + 标题 + 说明，下方一条拖拽条，右端显示当前值文本。
 *
 * [valueEditable] 为真时，**点右端的数值文本**会弹出输入框，可直接键入精确数值。
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
    valueEditable: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    var showInput by remember { mutableStateOf(false) }

    if (showInput) {
        ValueInputDialog(
            title = title,
            initial = value,
            range = valueRange,
            onDismiss = { showInput = false },
            onConfirm = {
                onValueChange(it.coerceIn(valueRange.start, valueRange.endInclusive))
                showInput = false
            }
        )
    }

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
                        maxLines = 1,
                        modifier = Modifier
                            .clickable(enabled = enabled && valueEditable) { showInput = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
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

/** 点击数值后的精确输入框（支持负号与小数，超出范围会被夹回）。 */
@Composable
private fun ValueInputDialog(
    title: String,
    initial: Float,
    range: ClosedFloatingPointRange<Float>,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember { mutableStateOf(formatNumber(initial)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.value_input_hint)) },
                keyboardOptions = KeyboardOptions(
                    // 允许负值（如位移）时用文本键盘（数字键盘没有负号）；否则用小数数字键盘。
                    keyboardType = if (range.start < 0f) KeyboardType.Text else KeyboardType.Decimal
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    text.trim().toFloatOrNull()?.let { onConfirm(it) }
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/** 整数值显示成整数，非整数保留必要的小数位。 */
private fun formatNumber(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()

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
 * 数值输入设置项（**不用拖拽条**）：左侧图标 + 标题 + 说明，右侧显示当前值与单位，
 * 点击整行弹出输入框直接键入数值（正整数）。
 *
 * [warnAbove] 非空且输入值大于它时，会先弹一个警告框确认，再真正生效。
 */
@Composable
fun NumberFieldWidget(
    title: String,
    value: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    description: String? = null,
    enabled: Boolean = true,
    minValue: Int = 0,
    maxValue: Int = Int.MAX_VALUE,
    warnAbove: Int? = null,
    warnTitle: String? = null,
    warnMessage: String? = null
) {
    val contentAlpha = if (enabled) 1f else 0.38f
    var showInput by remember { mutableStateOf(false) }
    var pending: Int? by remember { mutableStateOf(null) }

    if (showInput) {
        NumberInputDialog(
            title = title,
            initial = value,
            unit = unit,
            onDismiss = { showInput = false },
            onConfirm = { parsed ->
                showInput = false
                val v = parsed.coerceIn(minValue, maxValue)
                if (warnAbove != null && v > warnAbove) pending = v else onValueChange(v)
            }
        )
    }

    pending?.let { v ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(warnTitle ?: title) },
            text = { Text(warnMessage ?: unit) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending = null
                        onValueChange(v)
                    }
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showInput = true }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
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

        Text(
            text = "$value $unit",
            color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

/** 正整数输入框。 */
@Composable
private fun NumberInputDialog(
    title: String,
    initial: Int,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initial.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(R.string.value_input_hint)) },
                suffix = { Text(unit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { text.trim().toIntOrNull()?.let(onConfirm) }
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

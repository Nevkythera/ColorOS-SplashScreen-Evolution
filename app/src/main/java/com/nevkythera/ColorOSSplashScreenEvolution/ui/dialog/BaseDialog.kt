package com.Nevkythera.ColorOSSplashScreenEvolution.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.CapsuleShapes

/**
 * 通用对话框外壳 —— 对应 MCGA 的 `BaseDialog`。
 *
 * 由 `ColorPickDialog` 里原先的私有实现提取而来，并把顶部图标 [iconRes] 参数化：
 * 原实现把图标硬编码成 `format_color_fill`，而「退出动画」对话框需要同一套外壳
 * 但换成动画图标 —— 不抽出来就只能复制粘贴，两处必然漂移。
 *
 * @param iconRes 顶部图标（无默认值：显式传，避免又变成隐式硬编码）
 * @param content 标题/图标与底部按钮之间的内容区
 */
@Composable
fun BaseDialog(
    title: String,
    iconRes: Int,
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = CapsuleShapes.single(24.dp)
        ) {
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(
                            top = 12.dp, bottom = 8.dp, start = 12.dp, end = 12.dp
                        )
                    )
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(
                            top = 8.dp, bottom = 8.dp, start = 8.dp, end = 4.dp
                        )
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                    TextButton(
                        onClick = onConfirmation,
                        modifier = Modifier.padding(
                            top = 8.dp, bottom = 8.dp, start = 4.dp, end = 16.dp
                        )
                    ) {
                        Text(stringResource(R.string.dialog_confirm))
                    }
                }
            }
        }
    }
}

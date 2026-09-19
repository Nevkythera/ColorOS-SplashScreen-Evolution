package com.Nevkythera.ColorOSSplashScreenEvolution.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.CapsuleShapes

/**
 * 「退出动画」选择对话框。
 *
 * ★ 为什么用**对话框**而不是 [com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.ChoiceWidget]
 *   的 `DropdownMenu`：用户明确要求「点击时显示对话框样式的选择界面，而不是浮动列表」。
 *
 * ★ 草稿值（[draftIndex]）用 `remember` 暂存，**点确定才回写**，
 *   因此点「取消」能真正撤销选择（不会像即时写回那样留下副作用）。
 *
 * @param options       选项文案（默认 / 粒子消失），下标即配置值
 * @param selectedIndex 当前选中项
 * @param onSelect      点确定后回传新的选中下标
 */
@Composable
fun ExitAnimDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismissRequest: () -> Unit,
    onSelect: (Int) -> Unit
) {
    var draftIndex by remember { mutableStateOf(selectedIndex) }

    BaseDialog(
        title = title,
        iconRes = R.drawable.animation,
        onDismissRequest = onDismissRequest,
        onConfirmation = { onSelect(draftIndex) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            options.forEachIndexed { index, label ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CapsuleShapes.single(12.dp))
                        .clickable { draftIndex = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = draftIndex == index,
                        onClick = { draftIndex = index }
                    )
                    Text(
                        text = label,
                        color = if (draftIndex == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

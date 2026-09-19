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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R

// ── 布局常量 ──

/** 左侧图标尺寸。 */
private val ICON_SIZE = 24.dp

/** 右端下拉箭头尺寸（与 [OptionWidget] 保持一致的 12dp 细箭头）。 */
private val ARROW_SIZE = 12.dp

/** 图标 / 标题 / 值 / 箭头之间的统一间距。 */
private val GAP = 16.dp

/**
 * 右侧「当前值」的**硬宽度上限**。
 *
 * ★ 为什么用固定 dp，而不是"剩余空间的比例"、也不用 `SubcomposeLayout` 探固有宽度：
 *
 *   比例方案需要在测量前知道容器可用宽度，而可用宽度在
 *   `Column(verticalScroll) → SplicedColumnGroup → Row` 里是层层传递且**可能无界**
 *   （放进 `horizontalScroll`、或放进没有 `weight` 的 `Row` 时，
 *   `constraints.maxWidth` 就等于 `Constraints.Infinity`）。
 *
 *   `SubcomposeLayout` 探固有宽度的做法上一版试过，**直接导致应用闪退**：
 *   `Constraints.Infinity == Int.MAX_VALUE`，它与 density 相乘或与其它尺寸相加
 *   后截断成 `int` 会**变成负数**，随即抛
 *   `IllegalArgumentException: width must be >= 0`。
 */
private val VALUE_MAX_WIDTH = 150.dp

/**
 * 下拉选择设置项 —— 对应 RestoreSplashScreen 的 `DropDownPreference`。
 *
 * 用于「缩小图标」「替换背景颜色」「颜色模式」「语言」这类多选一配置：
 * 左侧图标 + 标题 + 说明，右侧显示当前选中项并带下拉箭头；
 * 点击整行弹出 [DropdownMenu] 选项列表。
 *
 * 整行可点击（包括标题区域），与 [SwitchWidget] 的交互一致。
 *
 * ────────────────────────────────────────────────────────────────────────
 * ★★ 右侧值文本必须**约束宽度**，但**不能**给它加 `weight`（两次真实踩坑）
 * ────────────────────────────────────────────────────────────────────────
 *
 * **坑一 —— 完全不约束右侧值：**
 *   ```kotlin
 *   Box(Modifier.weight(1f)) { /* 标题 + 说明 */ }
 *   Text(currentText)                  // ← 无宽度约束
 *   Icon(Modifier.size(12.dp))
 *   ```
 *   `Row` 先按值文本"想要的完整宽度"量右侧，剩下的才给 `weight(1f)` 的标题。
 *   中文值很短（"不缩小"）看不出问题；切英文后值变成
 *   `"Follow app icon (experimental)"` 这类长串，标题区被压到只剩几十 dp，
 *   `titleMedium` 的英文单词**逐字母换行**，渲染成竖排单字。
 *
 * **坑二 —— 给两侧都加 `weight(1f)`（加 `fill = false` 也没用）：**
 *   本以为是"短则短、长则封顶"，但 **`weight` 一旦指定就参与分配**，
 *   `fill` 只影响是否吃掉**剩余**空间、**不影响分配比例**：
 *   只要值的固有宽度 ≥ 标题宽度，两边就各拿 `FREE/2`。
 *   于是拿"本来正常的行"去换"本来坏掉的行"，净效果是**把界面改坏**：
 *
 *   | 场景 | 值区所需 | 不约束时标题区 | 两侧加 weight 后 |
 *   |---|---|---|---|
 *   | 缩小图标 / 不缩小 | 39dp | **221dp** | ~~130dp~~ |
 *   | Shrink icon / Do not shrink | 84dp | **175dp** | ~~130dp~~ |
 *   | Indicator color / Follow app icon (experimental) | 195dp | ~~65dp~~ | **130dp** |
 *
 *   中文行标题区从 221dp 掉到 130dp，"缩小图标"的说明从 2 行涨到 4 行，
 *   右侧却留一大片空白 —— 这就是"把刚改的地方改坏"的原因。
 *
 * ────────────────────────────────────────────────────────────────────────
 * 最终方案：标题侧 `weight(1f)` + 值侧**固定硬上限**（`widthIn`）
 * ────────────────────────────────────────────────────────────────────────
 *
 * ```kotlin
 * Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
 *     Icon(Modifier.size(ICON_SIZE))
 *     Column(Modifier.weight(1f)) { /* 标题 + 说明 */ }      // ← 唯一弹性区
 *     Text(currentText,
 *          maxLines = 1, overflow = Ellipsis, textAlign = End,
 *          modifier = Modifier.widthIn(max = VALUE_MAX_WIDTH)  // ← 硬上限，不参与 weight
 *                    .wrapContentWidth(Alignment.End))         // ← 短值不被撑开
 *     Icon(Modifier.size(ARROW_SIZE))
 * }
 * ```
 *
 * 分配逻辑（`weight(1f)` 是唯一弹性的，值区宽度已由上限确定）：
 *   标题区宽度 = 行宽 − 图标 − min(值内容, 150dp) − 箭头 − 三个间距
 *
 * | 值文本 | 值区宽度 | 标题区宽度（360dp 屏） | 结果 |
 * |---|---|---|---|
 * | `不缩小`（39dp） | 39dp | **205dp** | 说明 2 行，与原状一致 ✅ |
 * | `缩小全部图标`（84dp） | 84dp | **160dp** | 正常 ✅ |
 * | `Follow app icon (experimental)`（195dp） | 截断到 150dp | **94dp** | 不再竖排单字 ✅ |
 * | `Material You dynamic color`（169dp） | 截断到 150dp | **94dp** | 值走省略号 ✅ |
 *
 * ★ 通用准则：**只能有一个弹性区**。本组件把它留给标题（文字最多、最需要宽度），
 *   值侧用固定上限约束。要素更多时也照此办理，不要出现两个 `weight`。
 */
@Composable
fun ChoiceWidget(
    title: String,
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    description: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val currentText = options.getOrElse(selectedIndex) { "" }
    val contentAlpha = if (enabled) 1f else 0.38f

    // ★ 下拉菜单必须与整行同处一个布局作用域，才能以行为锚点弹出。
    //   因此用 Box 包住「整行 + DropdownMenu」——
    //   注意 Box 不能用 weight（它是外层，宽度应为行宽）。
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    modifier = Modifier.size(ICON_SIZE)
                )
            } else {
                Spacer(modifier = Modifier.size(ICON_SIZE))
            }

            // ---- 标题 + 说明：唯一弹性区，吃满剩余宽度 ----
            Column(modifier = Modifier.weight(1f)) {
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

            // ---- 当前值：硬上限 + 靠右；短值不撑开、长值走省略号 ----
            Text(
                text = currentText,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = VALUE_MAX_WIDTH)
                    .wrapContentWidth(Alignment.End)
            )

            Icon(
                painter = painterResource(id = R.drawable.arrow_forward_ios),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                modifier = Modifier.size(ARROW_SIZE)
            )
        }

        // ★ 菜单挂在 Box 上（不是 Row 上）：Row 的宽度恒为行宽，
        //   Box 的宽度与之相同，弹出位置一致；且菜单要允许溢出到 Row 之外。
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = CapsuleShapes.single(16.dp)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (index == selectedIndex)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}

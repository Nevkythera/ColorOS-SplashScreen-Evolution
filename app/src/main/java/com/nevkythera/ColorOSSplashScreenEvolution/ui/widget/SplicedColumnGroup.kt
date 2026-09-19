package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 拼图式分组容器 —— 对应 MCGA 的 `SplicedColumnGroup`。
 *
 * 结构上解决两个问题：
 *  1. 组标题渲染在卡片**外部**、卡片上方，独立成行；
 *     只有真正的设置项才进入圆角卡片容器。
 *  2. 卡片内部各条目用 2.dp 间隙拼接，并通过 [CapsuleShapes]
 *     计算首/中/末条目的差异化圆角，形成"一整块被切开"的视觉。
 *
 * ────────────────────────────────────────────────────────────────────────
 * ★★ 条目增删的过渡动画（本版新增）
 * ────────────────────────────────────────────────────────────────────────
 *
 * 场景：「几何形变加载动画」打开后会**多出**「指示器大小」「指示器颜色」两块；
 * 切回「不缩小」时「模糊图标背景」会**消失**。直接改列表会让 Compose
 * 瞬间增删，观感生硬。
 *
 * 本实现让增删变成**平台原生 ListView 式推挤动画**：
 *   - 进入：竖向推开（`expandVertically`）+ 淡入
 *   - 退出：竖向收拢（`shrinkVertically`）+ 淡出
 *   用带阻尼弹簧驱动，手感是"先快后慢地让位"，不是匀速直线运动。
 *
 * ### 为什么调用方要写 `StableEntry`
 *
 * [AnimatedVisibility] 只在 `visible` 由 true→false 时播退出动画。
 * 如果条目直接从 `content` 列表里消失，Compose 根本看不到它，
 * **退出动画永远不会播放**。
 *
 * 所以这里引入「稳定槽位」：调用方为每个可能的条目分配一个**固定 key**，
 * 并把它一直留在列表里，用 `visible` 控制显隐：
 *
 * ```kotlin
 * SplicedColumnGroup(entries = listOf(
 *     StableEntry("round") { SwitchWidget(...) },
 *     StableEntry("shrink") { ChoiceWidget(...) },
 *     StableEntry("blur", visible = config.shrinkIcon != 0) { SwitchWidget(...) },
 *     //                    ↑ 一直存在，只切 visible —— 退出动画才会播
 * ))
 * ```
 *
 * @param title 组标题。为空字符串时不渲染标题行。
 * @param entries 稳定槽位列表，见 [StableEntry]。
 * @param animateChanges 是否播放增删过渡动画。默认开启。
 */
@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    animateChanges: Boolean = true,
    entries: List<StableEntry>
) {
    if (entries.isEmpty()) return

    // ★ 记住每个 key 是否「已经出现过」。
    //   首次组合时不播进入动画（否则整个页面一进来所有条目都在动，很吵）；
    //   之后由不可见变可见时才是真正的"新增"，那时才播。
    val seenKeys = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
    ) {
        // ---- 组标题：卡片之外，独立成行 ----
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
        }

        // ---- 条目容器 ----
        // ★ 只把「可见」的条目计入圆角计算：隐藏中的条目不该影响
        //   首/末条目的圆角归属，否则会出现"最后一个可见项的圆角是 middle"的错乱。
        val visibleEntries = entries.filter { it.visible }

        Column(
            modifier = Modifier.clip(CapsuleShapes.single()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            entries.forEach { entry ->
                val visibleIndex = visibleEntries.indexOfFirst { it.key == entry.key }
                    .takeIf { it >= 0 }

                val shape = when {
                    visibleEntries.size <= 1 -> CapsuleShapes.single()
                    visibleIndex == 0 -> CapsuleShapes.first()
                    visibleIndex == visibleEntries.size - 1 -> CapsuleShapes.last()
                    else -> CapsuleShapes.middle()
                }

                val isFirstAppearance = !seenKeys.containsKey(entry.key)
                if (entry.visible && isFirstAppearance) {
                    // 记录"已出现"，但不播动画 —— 见上方注释
                    seenKeys[entry.key] = true
                }

                AnimatedVisibility(
                    visible = entry.visible,
                    // 首帧不播进入动画；之后由隐转显才播（这才是用户感知的"新增设置块"）
                    enter = if (animateChanges && !isFirstAppearance) {
                        expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Top
                        ) + fadeIn(animationSpec = tween(durationMillis = 160))
                    } else {
                        EnterTransition.None
                    },
                    exit = if (animateChanges) {
                        shrinkVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut(animationSpec = tween(durationMillis = 120))
                    } else {
                        ExitTransition.None
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
                            .clip(shape)
                    ) {
                        entry.content(shape)
                    }
                }
            }
        }
    }
}

/**
 * 稳定槽位 —— [SplicedColumnGroup] 的条目单元。
 *
 * [key] 必须**在同一组内稳定且唯一**。只要 key 不变，
 * 该条目在列表里的位置/显隐变化都会被正确识别为「同一个块的显隐」，
 * 而不是「删一个再加一个」，从而正确播放推挤动画。
 *
 * ★ 声明顺序 = 渲染顺序。隐藏中的条目**仍然占位在列表里**（只是不参与布局），
 *   所以它的声明位置决定了它显隐时推开的是上面还是下面的邻居。
 *
 * @param key 稳定唯一标识。
 * @param visible 是否可见。由 false→true 时播放进入动画，反向播放退出动画。
 * @param content 条目内容，入参为该条目应使用的圆角 [Shape]。
 */
class StableEntry(
    val key: String,
    val visible: Boolean = true,
    val content: @Composable (Shape) -> Unit
)

/**
 * 构造 [StableEntry] 的便捷函数，让调用点读起来仍然像 `add { ... }`。
 *
 * ```kotlin
 * entries = buildList {
 *     entry("round") { SwitchWidget(...) }
 *     entry("blur", visible = shrink != 0) { SwitchWidget(...) }
 * }
 * ```
 */
fun MutableList<StableEntry>.entry(
    key: String,
    visible: Boolean = true,
    content: @Composable (Shape) -> Unit
) {
    add(StableEntry(key, visible, content))
}

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


@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    animateChanges: Boolean = true,
    entries: List<StableEntry>
) {
    if (entries.isEmpty()) return

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

package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.data.CseConfig
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.ChoiceWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SliderWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SwitchWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.entry

/**
 * 图标页 —— 对应 RestoreSplashScreen 的 `IconPage`。
 *
 * 搬运「绘制图标圆角 / 缩小图标 / 替换图标获取方式」三件套的**配置入口**，
 * 并把设置页主目录的「关闭截图覆盖」也搬到本页控件下。
 *
 * 所有图标开关都在 Hook 层受总开关（masterSwitch）门控；UI 层在总开关关闭时
 * 也会把整页置灰禁用（见 [enabled]）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPage(
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    masterEnabled: Boolean,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    BasePanelPage(
        title = stringResource(R.string.feature_icon),
        onBackClick = onBackClick,
        onRestartClick = onRestartClick
    ) { paddingValues, scrollBehavior, _ ->
        IconPageContent(
            paddingValues = paddingValues,
            scrollBehavior = scrollBehavior,
            config = config,
            store = store,
            onConfigChange = onConfigChange,
            enabled = masterEnabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPageContent(
    paddingValues: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    enabled: Boolean
) {
    // ── 缩小图标：只有两态，且「选项索引」与「配置值」不是同一个数字 ──
    //   ★ 为什么需要映射，不能直接用索引：
    //     配置值 `shrinkIcon` 是 CseConfig 的枚举值（0 = 不缩小 / 2 = 全部），
    //     中间值 1（仅缩小低分辨率）已移除，所以值域是 {0, 2} 而非 {0, 1}。
    //     下拉列表的 `index` 必须是连续的 0..n-1，两者不能混用。
    //   ★ 早期版本恰好因为"选项索引 == 配置值"而可以直接传，
    //     删掉中间项后这个巧合不成立了 —— 若仍直接传索引，
    //     选「缩小全部图标」会写进 1（无效值），Hook 层判定为"不缩小"。
    val shrinkValues = listOf(
        CseConfig.SHRINK_NONE,
        CseConfig.SHRINK_ALL
    )
    val shrinkOptions = listOf(
        stringResource(R.string.not_shrink_icon),
        stringResource(R.string.shrink_all_icon)
    )

    // ── 依赖关系（决定哪些条目可见 / 可用）────────────────────────────
    // ① 「不缩小」时隐藏「模糊图标背景」，其余选项显示。
    //    理由：不缩小图标时图标已按原始分辨率铺满，叠加模糊背板没有意义。
    val showBlurBg = config.shrinkIcon != CseConfig.SHRINK_NONE

    // ② 「几何形变加载动画」打开时，「模糊图标背景」置为不可用（变灰）。
    //    理由：两者都要占用图标背后的同一块绘制区域，
    //    而形变指示器优先级更高（Hook 层也是它先命中），
    //    所以此处只做置灰提示，不改用户的开关值 ——
    //    关掉动画后用户的原始选择应当原样恢复。
    val blurBgEnabled = enabled && !config.enableMorphShape

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
    ) {
        // ==================== 「一般」列表组 ====================
        // 顺序：绘制图标圆角 / 缩小图标 / 模糊图标背景 / 替换图标获取方式 /
        //       关闭截图覆盖 / 移除图标（★ 移除图标固定在最下方）
        //
        // ★ 全部条目用固定 key 常驻列表，只切 visible —— 这样显隐才能播放
        //   竖向推挤过渡（详见 SplicedColumnGroup 文档）。
        SplicedColumnGroup(
            title = stringResource(R.string.group_general),
            entries = buildList {
                entry("round_corner") {
                    SwitchWidget(
                        iconRes = R.drawable.architecture,
                        title = stringResource(R.string.draw_round_corner),
                        description = stringResource(R.string.draw_round_corner_desc),
                        checked = config.drawRoundCorner,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setDrawRoundCorner(it)
                            onConfigChange(config.copy(drawRoundCorner = it))
                        }
                    )
                }
                entry("shrink_icon") {
                    ChoiceWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.shrink_icon),
                        description = stringResource(R.string.shrink_icon_desc),
                        // ★ 索引 → 配置值的双向映射（两者不是同一个数字，理由见上方注释）
                        selectedIndex = shrinkValues.indexOf(config.shrinkIcon)
                            .coerceAtLeast(0),
                        options = shrinkOptions,
                        enabled = enabled,
                        onSelect = { index ->
                            val value = shrinkValues.getOrElse(index) { CseConfig.SHRINK_NONE }
                            store.setShrinkIcon(value)
                            onConfigChange(config.copy(shrinkIcon = value))
                        }
                    )
                }
                // ★ 「不缩小」时隐藏本项（sel = 0）
                entry(
                    key = "icon_blur_bg",
                    visible = showBlurBg
                ) {
                    SwitchWidget(
                        iconRes = R.drawable.blur_on,
                        title = stringResource(R.string.icon_blur_bg),
                        description = stringResource(R.string.icon_blur_bg_desc),
                        checked = config.enableIconBlurBg,
                        // ★ 几何形变动画开启时置灰（但保留用户的开关值不动）
                        enabled = blurBgEnabled,
                        onCheckedChange = {
                            store.setEnableIconBlurBg(it)
                            onConfigChange(config.copy(enableIconBlurBg = it))
                        }
                    )
                }
                entry(
                    key = "icon_blur_bg_scale",
                    visible = showBlurBg && config.enableIconBlurBg
                ) {
                    SliderWidget(
                        iconRes = R.drawable.blur_on,
                        title = stringResource(R.string.icon_blur_bg_scale),
                        description = stringResource(R.string.icon_blur_bg_scale_desc),
                        value = config.blurBgScale / 100f,
                        valueRange = 0.5f..3f,
                        steps = 24,
                        valueText = String.format(
                            stringResource(R.string.icon_blur_bg_scale_value),
                            config.blurBgScale / 100f
                        ),
                        enabled = blurBgEnabled,
                        onValueChange = { v ->
                            val pct = (v * 100).toInt().coerceIn(50, 300)
                            store.setBlurBgScale(pct)
                            onConfigChange(config.copy(blurBgScale = pct))
                        }
                    )
                }
                entry("replace_icon") {
                    SwitchWidget(
                        iconRes = R.drawable.android,
                        title = stringResource(R.string.replace_icon),
                        description = stringResource(R.string.replace_icon_desc),
                        checked = config.replaceIcon,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setReplaceIcon(it)
                            onConfigChange(config.copy(replaceIcon = it))
                        }
                    )
                }
                entry("disable_snapshot") {
                    SwitchWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.disable_snapshot_overlay),
                        description = stringResource(R.string.disable_snapshot_overlay_desc),
                        checked = config.disablePreview,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setDisablePreview(it)
                            onConfigChange(config.copy(disablePreview = it))
                        }
                    )
                }
                // ★ 移除图标：固定放在「一般」组最下方
                entry("remove_icon") {
                    SwitchWidget(
                        iconRes = R.drawable.grid_off_filled,
                        title = stringResource(R.string.remove_icon),
                        description = stringResource(R.string.remove_icon_desc),
                        checked = config.removeIcon,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setRemoveIcon(it)
                            onConfigChange(config.copy(removeIcon = it))
                        }
                    )
                }
            }
        )

        // ==================== 「个性化」列表组 ====================
        // MD3E 几何形变动画开关 + 其衍生配置（大小 / 颜色）
        SplicedColumnGroup(
            title = stringResource(R.string.group_personalize),
            entries = buildList {
                entry("morph_shape") {
                    SwitchWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.morph_shape),
                        description = stringResource(R.string.morph_shape_desc),
                        checked = config.enableMorphShape,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setEnableMorphShape(it)
                            onConfigChange(config.copy(enableMorphShape = it))
                        }
                    )
                }
                // ★ 打开指示器时这两项才会出现 —— 竖向推开过渡
                entry(
                    key = "morph_size",
                    visible = config.enableMorphShape
                ) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.morph_shape_size),
                        description = stringResource(R.string.morph_shape_size_desc),
                        value = config.morphShapeScale / 100f,
                        valueRange = 0.5f..3f,
                        steps = 24, // (3.0-0.5)/0.1 = 25 段
                        valueText = String.format(
                            stringResource(R.string.morph_shape_size_value),
                            config.morphShapeScale / 100f
                        ),
                        enabled = enabled,
                        onValueChange = { v ->
                            val pct = (v * 100).toInt().coerceIn(50, 300)
                            store.setMorphShapeScale(pct)
                            onConfigChange(config.copy(morphShapeScale = pct))
                        }
                    )
                }
                entry(
                    key = "morph_color",
                    visible = config.enableMorphShape
                ) {
                    ChoiceWidget(
                        iconRes = R.drawable.format_color_fill,
                        title = stringResource(R.string.morph_shape_color),
                        description = stringResource(R.string.morph_shape_color_desc),
                        selectedIndex = config.morphShapeColorType,
                        options = listOf(
                            stringResource(R.string.morph_color_monet),
                            stringResource(R.string.morph_color_icon)
                        ),
                        enabled = enabled,
                        onSelect = {
                            store.setMorphShapeColorType(it)
                            onConfigChange(config.copy(morphShapeColorType = it))
                        }
                    )
                }
            }
        )
    }
}

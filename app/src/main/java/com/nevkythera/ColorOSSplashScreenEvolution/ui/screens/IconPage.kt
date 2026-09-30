package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
import kotlin.math.roundToInt

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
    val loadingAnimOptions = listOf(
        stringResource(R.string.loading_anim_off),
        stringResource(R.string.loading_anim_shape),
        stringResource(R.string.loading_anim_bar)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
    ) {
        // ==================== 「一般」列表组 ====================
        // 顺序：绘制图标圆角 / 缩小图标 / 替换图标获取方式 /
        //
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
                entry("icon_scale") {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.icon_size),
                        description = stringResource(R.string.icon_size_desc),
                        value = config.iconScale.toFloat(),
                        valueRange = CseConfig.ICON_SCALE_MIN.toFloat()..
                            CseConfig.ICON_SCALE_MAX.toFloat(),
                        steps = 19, // (150-50)/5 = 20 段
                        valueText = "${config.iconScale}%",
                        enabled = enabled,
                        onValueChange = { v ->
                            val pct = (v / 5f).roundToInt() * 5
                            store.setIconScale(pct)
                            onConfigChange(config.copy(iconScale = pct))
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
                entry("icon_offset_x") {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.icon_offset_x),
                        description = stringResource(R.string.icon_offset_x_desc),
                        value = config.iconOffsetX.toFloat(),
                        valueRange = CseConfig.OFFSET_MIN.toFloat()..CseConfig.OFFSET_MAX.toFloat(),
                        steps = 59, // (300-(-300))/10 = 60 段
                        valueText = stringResource(R.string.offset_value, config.iconOffsetX),
                        enabled = enabled,
                        onValueChange = { v ->
                            val dp = (v / 10f).toInt() * 10
                            store.setIconOffsetX(dp)
                            onConfigChange(config.copy(iconOffsetX = dp))
                        }
                    )
                }
                entry("icon_offset_y") {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.icon_offset_y),
                        description = stringResource(R.string.icon_offset_y_desc),
                        value = config.iconOffsetY.toFloat(),
                        valueRange = CseConfig.OFFSET_MIN.toFloat()..CseConfig.OFFSET_MAX.toFloat(),
                        steps = 59,
                        valueText = stringResource(R.string.offset_value, config.iconOffsetY),
                        enabled = enabled,
                        onValueChange = { v ->
                            val dp = (v / 10f).toInt() * 10
                            store.setIconOffsetY(dp)
                            onConfigChange(config.copy(iconOffsetY = dp))
                        }
                    )
                }
            }
        )

        // ==================== 「个性化」列表组 ====================
        // 加载动画（不启用 / 几何图形 / 加载条）+ 衍生配置
        SplicedColumnGroup(
            title = stringResource(R.string.group_personalize),
            entries = buildList {
                entry("loading_anim") {
                    ChoiceWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.morph_shape),
                        description = stringResource(R.string.morph_shape_desc),
                        selectedIndex = config.loadingAnimMode,
                        options = loadingAnimOptions,
                        enabled = enabled,
                        onSelect = {
                            store.setLoadingAnimMode(it)
                            onConfigChange(config.copy(loadingAnimMode = it))
                        }
                    )
                }
                entry(
                    key = "morph_size",
                    visible = config.loadingAnimMode == CseConfig.LOADING_ANIM_SHAPE
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
                    key = "loading_bar_length",
                    visible = config.loadingAnimMode == CseConfig.LOADING_ANIM_BAR
                ) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.loading_bar_length),
                        description = stringResource(R.string.loading_bar_length_desc),
                        value = config.loadingBarLength.toFloat(),
                        valueRange = CseConfig.LOADING_BAR_LENGTH_MIN.toFloat()..
                            CseConfig.LOADING_BAR_LENGTH_MAX.toFloat(),
                        steps = 43, // (480-40)/10 = 44 段
                        valueText = stringResource(
                            R.string.offset_value,
                            config.loadingBarLength
                        ),
                        enabled = enabled,
                        onValueChange = { v ->
                            val dp = (v / 10f).toInt() * 10
                            store.setLoadingBarLength(dp)
                            onConfigChange(config.copy(loadingBarLength = dp))
                        }
                    )
                }
                entry(
                    key = "loading_bar_thickness",
                    visible = config.loadingAnimMode == CseConfig.LOADING_ANIM_BAR
                ) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.loading_bar_thickness),
                        description = stringResource(R.string.loading_bar_thickness_desc),
                        value = config.loadingBarThickness.toFloat(),
                        valueRange = CseConfig.LOADING_BAR_THICKNESS_MIN.toFloat()..
                            CseConfig.LOADING_BAR_THICKNESS_MAX.toFloat(),
                        steps = 21, // (24-2)/1 = 22 段
                        valueText = stringResource(
                            R.string.offset_value,
                            config.loadingBarThickness
                        ),
                        enabled = enabled,
                        onValueChange = { v ->
                            val t = v.toInt().coerceIn(
                                CseConfig.LOADING_BAR_THICKNESS_MIN,
                                CseConfig.LOADING_BAR_THICKNESS_MAX
                            )
                            store.setLoadingBarThickness(t)
                            onConfigChange(config.copy(loadingBarThickness = t))
                        }
                    )
                }
                entry(
                    key = "indicator_offset_x",
                    visible = config.loadingAnimMode != CseConfig.LOADING_ANIM_NONE
                ) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.indicator_offset_x),
                        description = stringResource(R.string.indicator_offset_x_desc),
                        value = config.indicatorOffsetX.toFloat(),
                        valueRange = CseConfig.OFFSET_MIN.toFloat()..CseConfig.OFFSET_MAX.toFloat(),
                        steps = 59,
                        valueText = stringResource(R.string.offset_value, config.indicatorOffsetX),
                        enabled = enabled,
                        onValueChange = { v ->
                            val dp = (v / 10f).toInt() * 10
                            store.setIndicatorOffsetX(dp)
                            onConfigChange(config.copy(indicatorOffsetX = dp))
                        }
                    )
                }
                entry(
                    key = "indicator_offset_y",
                    visible = config.loadingAnimMode != CseConfig.LOADING_ANIM_NONE
                ) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.indicator_offset_y),
                        description = stringResource(R.string.indicator_offset_y_desc),
                        value = config.indicatorOffsetY.toFloat(),
                        valueRange = CseConfig.OFFSET_MIN.toFloat()..CseConfig.OFFSET_MAX.toFloat(),
                        steps = 59,
                        valueText = stringResource(R.string.offset_value, config.indicatorOffsetY),
                        enabled = enabled,
                        onValueChange = { v ->
                            val dp = (v / 10f).toInt() * 10
                            store.setIndicatorOffsetY(dp)
                            onConfigChange(config.copy(indicatorOffsetY = dp))
                        }
                    )
                }
                entry(
                    key = "morph_color",
                    visible = config.loadingAnimMode != CseConfig.LOADING_ANIM_NONE
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

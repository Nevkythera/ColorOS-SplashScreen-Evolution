package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.data.CseConfig
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.dialog.ColorPickDialog
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.ChoiceWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.OptionWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.entry

/**
 * 背景页 —— 对应 RestoreSplashScreen 的 `BackgroundPage`。
 *
 * 搬运「替换背景颜色」相关配置（**不搬运「单独配置应用」部分**）：
 *   - 替换背景颜色：不替换 / 从图标取色 / 莫奈取色 / 自定义颜色；
 *   - 颜色模式：浅色 / 暗色 / 跟随系统（仅「从图标取色」「莫奈取色」时显示）；
 *   - 自定义背景颜色：浅色 / 暗色两个颜色选择器（仅「自定义颜色」时显示）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundPage(
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    masterEnabled: Boolean,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    BasePanelPage(
        title = stringResource(R.string.feature_background),
        onBackClick = onBackClick,
        onRestartClick = onRestartClick
    ) { paddingValues, scrollBehavior, _ ->
        BackgroundPageContent(
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
private fun BackgroundPageContent(
    paddingValues: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    enabled: Boolean
) {
    // ★ 已删除「从图标取色」选项（值 1 已废弃，因该功能在 ColorOS 上取色不生效）。
    //   保留底层枚举值不变（0=不替换 / 2=莫奈 / 3=自定义），仅在 UI 层做索引↔值映射。
    //   避免改动已存配置、破坏老用户升级后的设置。
    val changeBgValueList = listOf(0, 2, 3)
    val changeBgOptions = listOf(
        stringResource(R.string.not_change_bg_color),
        stringResource(R.string.from_monet),
        stringResource(R.string.from_custom)
    )
    val colorModeOptions = listOf(
        stringResource(R.string.light_color),
        stringResource(R.string.dark_color),
        stringResource(R.string.follow_system)
    )

    var showLightPicker by remember { mutableStateOf(false) }
    var showDarkPicker by remember { mutableStateOf(false) }

    if (showLightPicker) {
        ColorPickDialog(
            title = stringResource(R.string.set_custom_bg_color_light),
            initialColor = parseColorOrWhite(config.customBgColor),
            onDismissRequest = { showLightPicker = false },
            onConfirmation = { argb ->
                val hex = String.format("#%08X", argb)
                store.setCustomBgColor(hex)
                onConfigChange(config.copy(customBgColor = hex))
                showLightPicker = false
            }
        )
    }
    if (showDarkPicker) {
        ColorPickDialog(
            title = stringResource(R.string.set_custom_bg_color_dark),
            initialColor = parseColorOrBlack(config.customBgColorNight),
            onDismissRequest = { showDarkPicker = false },
            onConfirmation = { argb ->
                val hex = String.format("#%08X", argb)
                store.setCustomBgColorNight(hex)
                onConfigChange(config.copy(customBgColorNight = hex))
                showDarkPicker = false
            }
        )
    }

    // ★ 用稳定槽位：条件显隐的条目常驻列表、只切 visible，
    //   这样"多出/收起一个设置块"会播竖向推挤过渡而非瞬间增删。
    val items = buildList {
        entry("change_bg_color") {
            ChoiceWidget(
                iconRes = R.drawable.format_color_fill,
                title = stringResource(R.string.change_bg_color),
                selectedIndex = changeBgValueList.indexOf(config.changeBgColorType).coerceAtLeast(0),
                options = changeBgOptions,
                enabled = enabled,
                onSelect = {
                    val value = changeBgValueList.getOrElse(it) { 0 }
                    store.setChangeBgColorType(value)
                    onConfigChange(config.copy(changeBgColorType = value))
                }
            )
        }
        // 颜色模式：仅「莫奈取色」时显示
        entry(
            key = "color_mode",
            visible = config.changeBgColorType == 2
        ) {
            ChoiceWidget(
                iconRes = R.drawable.architecture,
                title = stringResource(R.string.color_mode),
                selectedIndex = config.bgColorMode,
                options = colorModeOptions,
                enabled = enabled,
                onSelect = {
                    store.setBgColorMode(it)
                    onConfigChange(config.copy(bgColorMode = it))
                }
            )
        }
        // 自定义颜色：仅「自定义颜色」时显示浅色 / 暗色两个取色入口
        entry(
            key = "custom_light",
            visible = config.changeBgColorType == 3
        ) {
            OptionWidget(
                iconRes = R.drawable.format_color_fill,
                title = stringResource(R.string.set_custom_bg_color_light),
                description = config.customBgColor,
                enabled = enabled,
                onClick = { showLightPicker = true }
            )
        }
        entry(
            key = "custom_dark",
            visible = config.changeBgColorType == 3
        ) {
            OptionWidget(
                iconRes = R.drawable.format_color_fill,
                title = stringResource(R.string.set_custom_bg_color_dark),
                description = config.customBgColorNight,
                enabled = enabled,
                onClick = { showDarkPicker = true }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
    ) {
        SplicedColumnGroup(
            title = stringResource(R.string.feature_background),
            entries = items
        )
    }
}

/** 解析 "#RRGGBB" 或 "#AARRGGBB" 为 ARGB Int，失败回退白色。 */
private fun parseColorOrWhite(hex: String): Int =
    runCatching { AndroidColor.parseColor(hex) }.getOrDefault(AndroidColor.WHITE)

/** 解析颜色失败回退黑色。 */
private fun parseColorOrBlack(hex: String): Int =
    runCatching { AndroidColor.parseColor(hex) }.getOrDefault(AndroidColor.BLACK)

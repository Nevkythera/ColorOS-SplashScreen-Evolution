package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.data.CseConfig
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.dialog.ColorPickDialog
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.ChoiceWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.OptionWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SliderWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SwitchWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.entry
import com.Nevkythera.ColorOSSplashScreenEvolution.util.AppNameFontStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


/**
 * 底部页 —— 控制启动遮罩**底部**的内容：
 *   - 品牌图：移除应用自带的底部品牌图；
 *   - 应用名称：显示被启动应用的名称，含文本大小 / 粗细 / 位移 / 取色 / 英语文本 / 自定义字体。
 *
 * 与「图标」页的应用图标设置相互独立。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomPage(
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    masterEnabled: Boolean,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    BasePanelPage(
        title = stringResource(R.string.feature_bottom),
        onBackClick = onBackClick,
        onRestartClick = onRestartClick
    ) { paddingValues, scrollBehavior, _ ->
        BottomPageContent(
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
private fun BottomPageContent(
    paddingValues: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior,
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    enabled: Boolean
) {
    val colorOptions = listOf(
        stringResource(R.string.app_name_color_icon),
        stringResource(R.string.app_name_color_custom),
        stringResource(R.string.app_name_color_system)
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showColorPicker by remember { mutableStateOf(false) }
    var fontBusy by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickDialog(
            title = stringResource(R.string.app_name_custom_color),
            initialColor = runCatching { AndroidColor.parseColor(config.appNameCustomColor) }
                .getOrDefault(AndroidColor.WHITE),
            onDismissRequest = { showColorPicker = false },
            onConfirmation = { argb ->
                val hex = String.format("#%08X", argb)
                store.setAppNameCustomColor(hex)
                onConfigChange(config.copy(appNameCustomColor = hex))
                showColorPicker = false
            }
        )
    }

    // 选字体：任意文件，拷贝到私有目录后记下显示名与版本号（供 SystemUI 缓存失效）。
    val pickFont = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        fontBusy = true
        scope.launch {
            val name = withContext(Dispatchers.IO) { AppNameFontStore.save(context, uri) }
            fontBusy = false
            if (name != null) {
                val version = System.currentTimeMillis()
                store.setAppNameFontName(name)
                store.setAppNameFontVersion(version)
                onConfigChange(config.copy(appNameFontName = name, appNameFontVersion = version))
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.app_name_font_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
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
            title = stringResource(R.string.group_branding),
            entries = buildList {
                entry("remove_branding_image") {
                    SwitchWidget(
                        iconRes = R.drawable.grid_off_filled,
                        title = stringResource(R.string.remove_branding_image),
                        description = stringResource(R.string.remove_branding_image_desc),
                        checked = config.removeBrandingImage,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setRemoveBrandingImage(it)
                            onConfigChange(config.copy(removeBrandingImage = it))
                        }
                    )
                }
            }
        )

        SplicedColumnGroup(
            title = stringResource(R.string.group_app_name),
            entries = buildList {
                entry("show_app_name") {
                    SwitchWidget(
                        iconRes = R.drawable.dock_to_bottom_filled,
                        title = stringResource(R.string.show_app_name),
                        description = stringResource(R.string.show_app_name_desc),
                        checked = config.showAppName,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setShowAppName(it)
                            onConfigChange(config.copy(showAppName = it))
                        }
                    )
                }
                entry("app_name_position", visible = config.showAppName) {
                    ChoiceWidget(
                        iconRes = R.drawable.dock_to_bottom_filled,
                        title = stringResource(R.string.app_name_position),
                        description = stringResource(R.string.app_name_position_desc),
                        selectedIndex = config.appNamePosition,
                        options = listOf(
                            stringResource(R.string.app_name_position_below_icon),
                            stringResource(R.string.app_name_position_bottom)
                        ),
                        enabled = enabled,
                        onSelect = {
                            store.setAppNamePosition(it)
                            onConfigChange(config.copy(appNamePosition = it))
                        }
                    )
                }
                entry("app_name_text_size", visible = config.showAppName) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.app_name_text_size),
                        description = stringResource(R.string.app_name_text_size_desc),
                        value = config.appNameTextSize.toFloat(),
                        valueRange = CseConfig.APP_NAME_TEXT_SIZE_MIN.toFloat()..
                            CseConfig.APP_NAME_TEXT_SIZE_MAX.toFloat(),
                        steps = 39, // (48-8)/1 = 40 段
                        valueText = stringResource(
                            R.string.app_name_text_size_value,
                            config.appNameTextSize
                        ),
                        enabled = enabled,
                        onValueChange = { v ->
                            val sp = v.roundToInt().coerceIn(
                                CseConfig.APP_NAME_TEXT_SIZE_MIN,
                                CseConfig.APP_NAME_TEXT_SIZE_MAX
                            )
                            store.setAppNameTextSize(sp)
                            onConfigChange(config.copy(appNameTextSize = sp))
                        }
                    )
                }
                entry("app_name_font_weight", visible = config.showAppName) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.app_name_font_weight),
                        description = stringResource(R.string.app_name_font_weight_desc),
                        value = config.appNameFontWeight.toFloat(),
                        valueRange = CseConfig.APP_NAME_FONT_WEIGHT_MIN.toFloat()..
                            CseConfig.APP_NAME_FONT_WEIGHT_MAX.toFloat(),
                        steps = 7, // (900-100)/100 = 8 段
                        valueText = config.appNameFontWeight.toString(),
                        enabled = enabled,
                        onValueChange = { v ->
                            val w = (v / 100f).roundToInt() * 100
                            store.setAppNameFontWeight(w)
                            onConfigChange(config.copy(appNameFontWeight = w))
                        }
                    )
                }
                entry("app_name_offset_x", visible = config.showAppName) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.app_name_offset_x),
                        description = stringResource(R.string.app_name_offset_x_desc),
                        value = config.appNameOffsetX.toFloat(),
                        valueRange = CseConfig.OFFSET_MIN.toFloat()..CseConfig.OFFSET_MAX.toFloat(),
                        steps = 59,
                        valueText = stringResource(R.string.offset_value, config.appNameOffsetX),
                        enabled = enabled,
                        onValueChange = { v ->
                            val dp = (v / 10f).toInt() * 10
                            store.setAppNameOffsetX(dp)
                            onConfigChange(config.copy(appNameOffsetX = dp))
                        }
                    )
                }
                entry("app_name_offset_y", visible = config.showAppName) {
                    SliderWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.app_name_offset_y),
                        description = stringResource(R.string.app_name_offset_y_desc),
                        value = config.appNameOffsetY.toFloat(),
                        valueRange = CseConfig.OFFSET_MIN.toFloat()..CseConfig.OFFSET_MAX.toFloat(),
                        steps = 59,
                        valueText = stringResource(R.string.offset_value, config.appNameOffsetY),
                        enabled = enabled,
                        onValueChange = { v ->
                            val dp = (v / 10f).toInt() * 10
                            store.setAppNameOffsetY(dp)
                            onConfigChange(config.copy(appNameOffsetY = dp))
                        }
                    )
                }
                entry("app_name_color", visible = config.showAppName) {
                    ChoiceWidget(
                        iconRes = R.drawable.format_color_fill,
                        title = stringResource(R.string.app_name_color),
                        description = stringResource(R.string.app_name_color_desc),
                        selectedIndex = config.appNameColorType,
                        options = colorOptions,
                        enabled = enabled,
                        onSelect = {
                            store.setAppNameColorType(it)
                            onConfigChange(config.copy(appNameColorType = it))
                        }
                    )
                }
                entry(
                    key = "app_name_custom_color",
                    visible = config.showAppName &&
                        config.appNameColorType == CseConfig.APP_NAME_COLOR_CUSTOM
                ) {
                    OptionWidget(
                        iconRes = R.drawable.format_color_fill,
                        title = stringResource(R.string.app_name_custom_color),
                        description = config.appNameCustomColor,
                        enabled = enabled,
                        onClick = { showColorPicker = true }
                    )
                }
                entry("app_name_use_english", visible = config.showAppName) {
                    SwitchWidget(
                        iconRes = R.drawable.language,
                        title = stringResource(R.string.app_name_use_english),
                        description = stringResource(R.string.app_name_use_english_desc),
                        checked = config.appNameUseEnglish,
                        enabled = enabled,
                        onCheckedChange = {
                            store.setAppNameUseEnglish(it)
                            onConfigChange(config.copy(appNameUseEnglish = it))
                        }
                    )
                }
                entry("app_name_font", visible = config.showAppName) {
                    OptionWidget(
                        iconRes = R.drawable.tile,
                        title = stringResource(R.string.app_name_font),
                        description = config.appNameFontName.ifBlank {
                            stringResource(R.string.app_name_font_none)
                        },
                        enabled = enabled && !fontBusy,
                        onClick = { pickFont.launch(arrayOf("*/*")) }
                    )
                }
                entry(
                    key = "app_name_font_clear",
                    visible = config.showAppName && config.appNameFontName.isNotBlank()
                ) {
                    OptionWidget(
                        iconRes = R.drawable.grid_off_filled,
                        title = stringResource(R.string.app_name_font_clear),
                        enabled = enabled,
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { AppNameFontStore.clear(context) }
                                store.setAppNameFontName("")
                                store.setAppNameFontVersion(0L)
                                onConfigChange(
                                    config.copy(appNameFontName = "", appNameFontVersion = 0L)
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

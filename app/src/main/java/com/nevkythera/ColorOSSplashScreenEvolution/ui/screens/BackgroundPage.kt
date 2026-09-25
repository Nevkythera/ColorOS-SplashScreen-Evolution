package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.ImageCropActivity
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
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SplashMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickBusy by remember { mutableStateOf(false) }

    // 裁切完成后写回结果：裁切页已把媒体落到私有目录，这里记录源 URI 与裁切参数。
    val cropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pickBusy = false
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val src = data?.data?.toString().orEmpty()
            val kind = data?.getStringExtra(ImageCropActivity.EXTRA_RESULT_KIND)
                ?: SplashMedia.KIND_IMAGE
            val transform = data?.getStringExtra(ImageCropActivity.EXTRA_RESULT_TRANSFORM).orEmpty()
            store.setSplashImageUri(src)
            store.setSplashMediaKind(kind)
            store.setSplashMediaTransform(transform)
            store.setSplashImageEnabled(true)
            onConfigChange(
                config.copy(
                    splashImageUri = src,
                    splashMediaKind = kind,
                    splashMediaTransform = transform,
                    splashImageEnabled = true
                )
            )
        }
    }

    // 相册返回的 content URI 读授权不保证跨 Activity 可用；先拷进应用缓存，再把
    // 文件路径传给裁切页解码。支持图片 / GIF / 视频。
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pickBusy = true
        scope.launch {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val kind = when {
                mime.startsWith("video/") -> SplashMedia.KIND_VIDEO
                mime == "image/gif" -> SplashMedia.KIND_GIF
                else -> SplashMedia.KIND_IMAGE
            }
            val cached = withContext(Dispatchers.IO) { copyToCache(context, uri) }
            pickBusy = false
            if (cached != null) {
                cropLauncher.launch(
                    ImageCropActivity.intent(context, cached.absolutePath, uri.toString(), kind)
                )
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.splash_image_save_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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
        entry("splash_image") {
            SwitchWidget(
                iconRes = R.drawable.image,
                title = stringResource(R.string.splash_image),
                description = stringResource(R.string.splash_image_desc),
                checked = config.splashImageEnabled,
                enabled = enabled,
                onCheckedChange = {
                    store.setSplashImageEnabled(it)
                    onConfigChange(config.copy(splashImageEnabled = it))
                }
            )
        }
        entry(
            key = "splash_image_pick",
            visible = config.splashImageEnabled
        ) {
            OptionWidget(
                iconRes = R.drawable.image,
                title = stringResource(R.string.splash_image_pick),
                description = mediaKindLabel(config),
                enabled = enabled && !pickBusy,
                onClick = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
            )
        }
        entry(
            key = "splash_image_alpha",
            visible = config.splashImageEnabled
        ) {
            SliderWidget(
                iconRes = R.drawable.image,
                title = stringResource(R.string.splash_image_alpha),
                value = config.splashImageAlpha.toFloat(),
                valueRange = 0f..100f,
                valueText = "${config.splashImageAlpha}%",
                enabled = enabled,
                onValueChange = { v ->
                    val alpha = v.roundToInt().coerceIn(0, 100)
                    store.setSplashImageAlpha(alpha)
                    onConfigChange(config.copy(splashImageAlpha = alpha))
                }
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

/** 已选媒体的格式标签，供「选择图片」项展示。 */
@Composable
private fun mediaKindLabel(config: CseConfig): String = when {
    config.splashImageUri.isBlank() -> stringResource(R.string.splash_image_none)
    config.splashMediaKind == SplashMedia.KIND_VIDEO -> stringResource(R.string.splash_image_kind_video)
    config.splashMediaKind == SplashMedia.KIND_GIF -> stringResource(R.string.splash_image_kind_gif)
    else -> stringResource(R.string.splash_image_kind_png)
}

/** 把相册返回的图片拷贝到应用缓存，供裁切页解码（规避跨 Activity 的 URI 授权问题）。 */
private fun copyToCache(context: Context, uri: Uri): File? = runCatching {
    val dst = File(context.cacheDir, "crop_src")
    context.contentResolver.openInputStream(uri)?.use { input ->
        dst.outputStream().use { input.copyTo(it) }
    } ?: return null
    dst
}.getOrNull()

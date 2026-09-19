package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.data.CseConfig
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.dialog.ExitAnimDialog
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.OptionWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SliderWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.entry
import kotlin.math.roundToInt

/** 时长拖拽条的步长（毫秒）。 */
private const val DURATION_STEP_MS = 50

/** Slider 的离散档位数：区间 1800ms / 50ms = 36 段 → steps = 36 - 1。 */
private const val EXIT_DURATION_STEPS = 35

/**
 * 「退出动画」三级页（设置页 → 动画 → 退出动画）。
 *
 * 内容：
 *  1. **退出动画** 分组：一个对话框样式的选择器（默认 / 粒子消失）。
 *     点它弹出 [ExitAnimDialog]（RadioButton 两态），不再是 ChoiceWidget 的浮动列表。
 *  2. **粒子参数** 分组：动画时长拖拽条。仅在选了「粒子消失」时可用，
 *     选「默认」时置灰（值保留，切回粒子即复用）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitAnimationPage(
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val modeOptions = listOf(
        stringResource(R.string.exit_anim_default),
        stringResource(R.string.exit_anim_particle),
        stringResource(R.string.exit_anim_dissolve)
    )

    if (showDialog) {
        ExitAnimDialog(
            title = stringResource(R.string.exit_anim_mode),
            options = modeOptions,
            selectedIndex = config.exitAnimMode,
            onDismissRequest = { showDialog = false },
            onSelect = { index ->
                store.setExitAnimMode(index)
                onConfigChange(config.copy(exitAnimMode = index))
                showDialog = false
            }
        )
    }

    val particleEnabled = config.exitAnimMode != CseConfig.EXIT_MODE_DEFAULT

    BasePanelPage(
        title = stringResource(R.string.exit_animation),
        onBackClick = onBackClick,
        onRestartClick = onRestartClick
    ) { paddingValues, scrollBehavior, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
        ) {
            SplicedColumnGroup(
                title = stringResource(R.string.exit_animation),
                entries = buildList {
                    entry("exit_anim_mode") { shape ->
                        OptionWidget(
                            modifier = Modifier.clip(shape),
                            iconRes = R.drawable.animation,
                            title = stringResource(R.string.exit_anim_mode),
                            description = modeOptions.getOrElse(config.exitAnimMode) { modeOptions[0] },
                            onClick = { showDialog = true }
                        )
                    }
                }
            )

            SplicedColumnGroup(
                title = stringResource(R.string.exit_particle_params),
                entries = buildList {
                    entry("exit_particle_speed") {
                        SliderWidget(
                            iconRes = R.drawable.bolt,
                            title = stringResource(R.string.exit_particle_speed),
                            description = stringResource(R.string.exit_particle_speed_desc),
                            value = config.particleTimeMs.toFloat(),
                            valueRange = 100f..2000f,
                            steps = 37,
                            valueText = String.format(
                                stringResource(R.string.exit_particle_speed_value),
                                config.particleTimeMs
                            ),
                            enabled = particleEnabled,
                            onValueChange = { v ->
                                val ms = ((v / 50f).toInt() * 50).coerceIn(100, 2000)
                                store.setParticleTimeMs(ms)
                                onConfigChange(config.copy(particleTimeMs = ms))
                            }
                        )
                    }
                    entry("exit_particle_duration") {
                        SliderWidget(
                            iconRes = R.drawable.animation,
                            title = stringResource(R.string.exit_particle_duration),
                            description = stringResource(R.string.exit_particle_duration_desc),
                            value = config.exitParticleDurationMs.toFloat(),
                            valueRange = CseConfig.EXIT_DURATION_MIN.toFloat()..
                                CseConfig.EXIT_DURATION_MAX.toFloat(),
                            steps = EXIT_DURATION_STEPS,
                            valueText = stringResource(
                                R.string.exit_particle_duration_value,
                                config.exitParticleDurationMs
                            ),
                            enabled = particleEnabled,
                            onValueChange = { v ->
                                val ms = (v / DURATION_STEP_MS).roundToInt() * DURATION_STEP_MS
                                store.setExitParticleDurationMs(ms)
                                onConfigChange(config.copy(exitParticleDurationMs = ms))
                            }
                        )
                    }
                }
            )
        }
    }
}

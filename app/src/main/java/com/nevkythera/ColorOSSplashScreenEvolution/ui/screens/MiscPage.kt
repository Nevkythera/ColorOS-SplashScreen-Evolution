package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.data.CseConfig
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.StableEntry
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SwitchWidget


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscPage(
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    masterEnabled: Boolean,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    BasePanelPage(
        title = stringResource(R.string.feature_misc),
        onBackClick = onBackClick,
        onRestartClick = onRestartClick
    ) { paddingValues, scrollBehavior, _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
        ) {
            SplicedColumnGroup(
                title = stringResource(R.string.group_module_function),
                entries = listOf(
                    StableEntry("hot_start_splash") {
                        SwitchWidget(
                            iconRes = R.drawable.bolt,
                            title = stringResource(R.string.enable_hot_start_splash),
                            description = stringResource(R.string.enable_hot_start_splash_desc),
                            checked = config.enableHotStartSplash,
                            enabled = masterEnabled,
                            onCheckedChange = {
                                store.setEnableHotStartSplash(it)
                                onConfigChange(config.copy(enableHotStartSplash = it))
                            }
                        )
                    }
                )
            )
        }
    }
}

package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.data.CseConfig
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SwitchWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.entry

/**
 * 「附加功能」二级页 —— 收纳不属于主功能链路的附加开关。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraFeaturesPage(
    config: CseConfig,
    store: ConfigStore,
    onConfigChange: (CseConfig) -> Unit,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    BasePanelPage(
        title = stringResource(R.string.feature_extra),
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
                title = stringResource(R.string.feature_extra),
                entries = buildList {
                    entry("aosp_transition") {
                        SwitchWidget(
                            iconRes = R.drawable.animation,
                            title = stringResource(R.string.aosp_transition),
                            description = stringResource(R.string.aosp_transition_desc),
                            checked = config.aospTransition,
                            onCheckedChange = {
                                store.setAospTransition(it)
                                onConfigChange(config.copy(aospTransition = it))
                            }
                        )
                    }
                }
            )
        }
    }
}

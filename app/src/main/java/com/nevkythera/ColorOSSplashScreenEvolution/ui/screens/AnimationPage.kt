package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.ExitAnimationActivity
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.OptionWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.entry

/**
 * 「动画」二级页。
 *
 * 结构 1:1 照抄 [MiscPage]：顶栏 + 一个功能入口，入口指向独立的
 * [ExitAnimationActivity]（系统原生 Activity 转场 + 预测式返回）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationPage(
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit
) {
    val context = LocalContext.current
    BasePanelPage(
        title = stringResource(R.string.feature_animation),
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
                title = stringResource(R.string.group_module_function),
                entries = buildList {
                    entry("exit_animation") { shape ->
                        OptionWidget(
                            modifier = Modifier.clip(shape),
                            iconRes = R.drawable.animation,
                            title = stringResource(R.string.exit_animation),
                            description = stringResource(R.string.exit_animation_desc),
                            onClick = { ExitAnimationActivity.start(context) }
                        )
                    }
                }
            )
        }
    }
}

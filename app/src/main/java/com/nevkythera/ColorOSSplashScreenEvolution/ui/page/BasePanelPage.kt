package com.Nevkythera.ColorOSSplashScreenEvolution.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme.baseHazeStyle
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.AppTopBar
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.RestartDialog
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.RestartOverlay
import com.Nevkythera.ColorOSSplashScreenEvolution.util.SystemUiRestarter
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * 二级功能页的通用骨架。
 *
 * 承载：
 *
 *  1. **跨页面转场**：由 NavHost 的默认转场负责，页面本身不再参与共享元素动画；
 *
 *  2. **渐变模糊标题**：顶栏施加 `hazeEffect` + `HazeProgressive.verticalGradient`，
 *     列表滚动到标题下方时内容被"上实下虚"地模糊淡出。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasePanelPage(
    title: String,
    onBackClick: () -> Unit,
    onRestartClick: () -> Unit,
    content: @Composable (
        paddingValues: PaddingValues,
        scrollBehavior: TopAppBarScrollBehavior,
        hazeState: HazeState
    ) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults
        .exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())
    val hazeState = rememberHazeState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showRestartDialog by remember { mutableStateOf(false) }
    var restartBusy by remember { mutableStateOf(false) }
    fun rebootSystem() {
        if (restartBusy) return
        restartBusy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { SystemUiRestarter.rebootSystem() }
            Toast.makeText(
                context,
                if (ok) context.getString(R.string.reboot_done)
                else context.getString(R.string.restart_failed),
                Toast.LENGTH_SHORT
            ).show()
            restartBusy = false
        }
    }
    // 重启菜单：页面内浮层（渲染在下面带 hazeSource 的 Box 内，才能模糊页面内容）
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                modifier = Modifier.hazeEffect(
                    state = hazeState,
                    style = baseHazeStyle()
                ) {
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = 1f,
                        endIntensity = 0f
                    )
                },
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = scrollBehavior,
                isBackAvailable = true,
                onBackClick = onBackClick,
                isRestartAvailable = true,
                onRestartClick = { showRestartDialog = true }
            )
        }
    ) { paddingValues ->
        //   hazeEffect 没有内容可模糊，表现为"顶栏完全没有模糊"）。
        //   同时**去掉 top padding**：让内容延伸到顶栏下方（各页面内部用
        //   TOP_BAR_SPACER 占位），滚动时内容穿过顶栏，模糊才看得见。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            content(
                PaddingValues(bottom = paddingValues.calculateBottomPadding()),
                scrollBehavior,
                hazeState
            )
            if (showRestartDialog) {
                RestartOverlay(
                    hazeState = hazeState,
                    onDismiss = { showRestartDialog = false },
                    onRestartSystemUi = onRestartClick,
                    onRebootSystem = { rebootSystem() }
                )
            }
        }
    }
}

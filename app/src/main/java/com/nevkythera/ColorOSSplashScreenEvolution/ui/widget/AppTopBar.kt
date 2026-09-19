package com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Nevkythera.ColorOSSplashScreenEvolution.R

/**
 * 大标题顶栏 —— 对应 MCGA 的 `AppTopBar`。
 *
 * 从原 `MainScreen` 内的私有 `AppTopBar` 拆出成独立组件，供主界面与
 * 三个二级功能页（图标/背景/杂项）复用。相比旧版做了三处调整：
 *
 *  1. 新增 [isBackAvailable] / [onBackClick]：二级页显示返回箭头；
 *  2. 重启按钮改回纯 IconButton（不再带"执行热重载"下拉菜单 —— 热重载已删除）；
 *  3. 顶栏本身不内置 haze，模糊由 [com.Nevkythera.ColorOSSplashScreenEvolution.ui.page.BasePanelPage]
 *     在外层通过 `Modifier.hazeEffect` 施加，保持职责单一。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppTopBar(
    modifier: Modifier = Modifier,
    /**
     * 顶栏展开高度。
     * - 二级页用默认 128.dp（大标题，滚动时折叠）；
     * - 主界面传 64.dp：顶栏固定为**大标题缩小后的高度**，
     *   模糊区域因此固定不变，不必随列表滚动反复重采样（省性能）。
     */
    expandedHeight: Dp = 128.dp,
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    isHomePage: Boolean = false,
    isBackAvailable: Boolean = false,
    onBackClick: () -> Unit = {},
    isRestartAvailable: Boolean = false,
    restartEnabled: Boolean = true,
    onRestartClick: () -> Unit = {}
) {
    LargeTopAppBar(
        title = title,
        modifier = modifier.padding(start = 6.dp),
        navigationIcon = {
            if (isHomePage) {
                //   会"隐形"（用户截图里的白色残影），所以垫一层启动器同款深色背景，
                //   外观与桌面图标一致。
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colorResource(R.color.ic_launcher_background)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (isBackAvailable) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            if (isRestartAvailable) {
                IconButton(
                    onClick = onRestartClick,
                    enabled = restartEnabled
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.refresh),
                        contentDescription = stringResource(R.string.restart_systemui),
                        tint = if (restartEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        },
        expandedHeight = expandedHeight,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        scrollBehavior = scrollBehavior
    )
}

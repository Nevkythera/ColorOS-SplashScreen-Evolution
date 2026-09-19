package com.Nevkythera.ColorOSSplashScreenEvolution.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.Nevkythera.ColorOSSplashScreenEvolution.PanelActivity
import com.Nevkythera.ColorOSSplashScreenEvolution.R
import com.Nevkythera.ColorOSSplashScreenEvolution.data.ConfigStore
import com.Nevkythera.ColorOSSplashScreenEvolution.data.CseConfig
import com.Nevkythera.ColorOSSplashScreenEvolution.data.XposedRepo
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.navgation.Destination
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.theme.baseHazeStyle
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.AppTopBar
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.CapsuleShapes
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.OptionWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SplicedColumnGroup
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.StableEntry
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.entry
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.SwitchWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.ui.widget.ChoiceWidget
import com.Nevkythera.ColorOSSplashScreenEvolution.util.LauncherIconController
import com.Nevkythera.ColorOSSplashScreenEvolution.util.LocaleManager
import com.Nevkythera.ColorOSSplashScreenEvolution.util.RootManagerDetector
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


internal val TOP_BAR_SPACER = 132.dp


private enum class Tab(
    @StringRes val labelRes: Int,
    val icon: Int,
    val focusIcon: Int
) {
    HOME(R.string.tab_home, R.drawable.home_outline, R.drawable.home_filled),
    SETTING(R.string.tab_setting, R.drawable.settings_outline, R.drawable.settings_filled),
    ABOUT(R.string.tab_about, R.drawable.info_outline, R.drawable.info_filled)
}

/**
 * 主界面。
 *
 * 结构照 MCGA 迁移：
 *   Scaffold
 *    ├ LargeTopAppBar（大标题 + 首页显示应用图标 + 设置页右上角重启按钮）
 *    ├ HorizontalPager（三页签：首页 / 设置 / 关于）
 *    └ NavigationBar（底部三页签，选中态用 filled 图标）
 *
 * 本次改动：
 *   1. 删除「执行热重载」全部相关代码（无效功能）；
 *   2. 设置页新增「图标 / 背景 / 杂项」三个功能入口，共享元素过渡跳转；
 *   3. 「关闭截图覆盖」从设置页移除，迁入图标页；
 *   4. 「强制原生启动遮罩」升级为总开关，关闭时置灰所有小开关。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repo = remember { XposedRepo.getInstance(context) }
    val store = remember { ConfigStore(context) }

    var config by remember { mutableStateOf(store.read()) }
    var rootInfo by remember { mutableStateOf(RootManagerDetector.RootInfo()) }
    var rootLoading by remember { mutableStateOf(true) }
    var isActive by remember { mutableStateOf(repo.isActive) }
    var framework by remember { mutableStateOf(repo.frameworkInfo()) }

    //   真正的语言落地交给 AppCompatDelegate（见 LocaleManager），
    //   它会在 setApplicationLocales 后**自动重建 Activity**，
    //   重建后本状态会由 LocaleManager.current() 重新读回，因此这里只需记住索引。
    var languageIndex by remember { mutableStateOf(LocaleManager.current(context)) }
    val languageOptions = listOf(
        stringResource(R.string.language_follow_system),
        stringResource(R.string.language_zh_cn),
        stringResource(R.string.language_en)
    )

    //   向上滚动折叠到 64dp 后**保持不动**，模糊区域随之固定，
    //   不会在列表滚动过程中反复改变尺寸（性能要求）。
    val scrollBehavior = TopAppBarDefaults
        .exitUntilCollapsedScrollBehavior(state = rememberTopAppBarState())

    val pagerState = rememberPagerState(initialPage = 0) { Tab.entries.size }
    val scope = rememberCoroutineScope()

    // 首次进入：探测 root、校正桌面图标开关的真实状态
    LaunchedEffect(Unit) {
        config = config.copy(showLauncherIcon = LauncherIconController.isIconVisible(context))
        store.setShowLauncherIcon(config.showLauncherIcon)

        rootInfo = RootManagerDetector.detect()
        rootLoading = false
    }

    // 订阅框架服务状态。服务绑定是异步的，必须在组合期注册回调。
    DisposableEffect(repo) {
        repo.onActiveChanged = { active ->
            isActive = active
            if (active) framework = repo.frameworkInfo()
        }
        repo.onServiceInfoChanged = { info -> framework = info }
        onDispose {
            repo.onActiveChanged = null
            repo.onServiceInfoChanged = null
        }
    }

    val current = Tab.entries[pagerState.currentPage]

    //   不能读 about_hero_title —— 它在英文下是 "COS SplashScreen Evolution"，
    //   放进只有一行的顶栏会被截成 "COS SplashScreen Evol…"，
    //   既难看又和「设置 / 关于」两个页签宽度不齐。
    //   设置/关于页仍显示页签名。
    val topBarTitle = if (current == Tab.HOME) {
        stringResource(R.string.about_hero_short)
    } else {
        stringResource(current.labelRes)
    }

    // 重启 SystemUI / 热重载的统一入口。
    var busy by remember { mutableStateOf(false) }
    var showRestartMenu by remember { mutableStateOf(false) }
    fun restartSystemUi() {
        if (busy) return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                com.Nevkythera.ColorOSSplashScreenEvolution.util.SystemUiRestarter
                    .restartSystemUi(context)
            }
            Toast.makeText(
                context,
                if (ok) context.getString(R.string.restart_done)
                else context.getString(R.string.restart_failed),
                Toast.LENGTH_SHORT
            ).show()
            busy = false
        }
    }

    // 重启确认框
    if (showRestartMenu) {
        AlertDialog(
            onDismissRequest = { showRestartMenu = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.restart_systemui)) },
            text = { Text(stringResource(R.string.restart_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartMenu = false
                        restartSystemUi()
                    }
                ) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestartMenu = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    //   二级页（图标/背景/杂项）改为**独立 Activity**（PanelActivity），
    //   转场交给系统原生的 Activity 动画。
    //   这里只保留一层背景色容器 + Scaffold，结构比之前的 NavHost 简单一层。
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        //   下方 Pager 内容用 hazeSource 作为模糊来源（与二级页 BasePanelPage 一致）。
        val hazeState = rememberHazeState()

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
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
                        //   折叠后固定为 64dp 小标题（模糊区域不再变化）。
                        title = {
                            Text(topBarTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        scrollBehavior = scrollBehavior,
                        isHomePage = current == Tab.HOME,
                        isRestartAvailable = current == Tab.SETTING,
                        restartEnabled = rootInfo.hasSu,
                        onRestartClick = { showRestartMenu = true }
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        Tab.entries.forEachIndexed { index, dest ->
                            val selected = pagerState.currentPage == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (selected) dest.focusIcon else dest.icon
                                        ),
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .hazeSource(state = hazeState)
                        //   这样顶栏的 haze 才有内容可采样 —— 否则顶栏背后永远是空白，
                        //   模糊自然"看不出效果"。各页面顶部用 TOP_BAR_SPACER 占位避免遮挡。
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) { page ->
                    val pageModifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)

                    when (Tab.entries[page]) {
                        Tab.HOME -> HomePage(
                            modifier = pageModifier,
                            config = config,
                            rootInfo = rootInfo,
                            rootLoading = rootLoading,
                            isActive = isActive,
                            framework = framework,
                            languageIndex = languageIndex,
                            languageOptions = languageOptions,
                            onLanguageChange = { languageIndex = it },
                            onConfigChange = { config = it },
                            store = store
                        )

                        Tab.SETTING -> SettingPage(
                            modifier = pageModifier,
                            config = config,
                            onConfigChange = { config = it },
                            store = store
                        )

                        Tab.ABOUT -> AboutPage(modifier = pageModifier)
                    }
                }
            }
    }
}

// ==================================================================== 首页

@Composable
private fun HomePage(
    modifier: Modifier,
    config: CseConfig,
    rootInfo: RootManagerDetector.RootInfo,
    rootLoading: Boolean,
    isActive: Boolean,
    framework: XposedRepo.FrameworkInfo?,
    languageIndex: Int,
    languageOptions: List<String>,
    onLanguageChange: (Int) -> Unit,
    onConfigChange: (CseConfig) -> Unit,
    store: ConfigStore
) {
    val context = LocalContext.current

    // 设备信息：安卓版本 / 机型，读一次即可
    val androidVersion = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）"
        } else {
            "Android ${Build.VERSION.RELEASE}"
        }
    }
    val deviceModel = remember {
        val brand = Build.BRAND.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        if (model.startsWith(brand, ignoreCase = true)) model else "$brand $model"
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
    ) {
        // ---- 状态卡：模块激活状态 + root 状态 ----
        StatusCard(
            isActive = isActive,
            isRootAvailable = rootInfo.hasSu,
            framework = framework
        )

        // ---- 设备信息（含 Root 管理器信息） ----
        SplicedColumnGroup(
            title = stringResource(R.string.group_device_info),
            entries = buildList {
                entry("android_version") {
                    OptionWidget(
                        iconRes = R.drawable.android,
                        title = stringResource(R.string.android_version),
                        description = androidVersion,
                        onClick = {}
                    )
                }
                entry("device_model") {
                    OptionWidget(
                        iconRes = R.drawable.aod_tablet,
                        title = stringResource(R.string.device_model),
                        description = deviceModel,
                        onClick = {}
                    )
                }
                entry("root_manager") {
                    OptionWidget(
                        iconRes = R.drawable.family_history,
                        title = stringResource(R.string.root_manager),
                        description = if (rootLoading) {
                            stringResource(R.string.state_detecting)
                        } else {
                            rootInfo.nameLiteral ?: stringResource(rootInfo.nameRes)
                        },
                        isError = !rootLoading && rootInfo.nameRes == R.string.root_not_detected &&
                            rootInfo.nameLiteral == null,
                        onClick = {}
                    )
                }
                entry("root_manager_version") {
                    OptionWidget(
                        iconRes = R.drawable.difference,
                        title = stringResource(R.string.root_manager_version),
                        description = if (rootLoading) {
                            stringResource(R.string.state_detecting)
                        } else {
                            rootInfo.versionLiteral ?: stringResource(rootInfo.versionRes)
                        },
                        onClick = {}
                    )
                }
            }
        )

        // ---- 应用配置 ----
        SplicedColumnGroup(
            title = stringResource(R.string.group_app_config),
            entries = buildList {
                entry("show_launcher_icon") {
                    SwitchWidget(
                        iconRes = R.drawable.shortcut,
                        title = stringResource(R.string.show_launcher_icon),
                        checked = config.showLauncherIcon,
                        onCheckedChange = { on ->
                            if (LauncherIconController.setIconVisible(context, on)) {
                                store.setShowLauncherIcon(on)
                                onConfigChange(config.copy(showLauncherIcon = on))
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.switch_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
                entry("language") {
                    //   顺序很重要 —— **先持久化，再重建**。
                    //   LocaleManager.apply() 内部会 recreate()，新 Activity 起来时
                    //   必须已经能读到新语言（由 persist 保证）。
                    ChoiceWidget(
                        //   放语言切换上会误导。
                        iconRes = R.drawable.language,
                        title = stringResource(R.string.language),
                        description = stringResource(R.string.language_desc),
                        selectedIndex = languageIndex,
                        options = languageOptions,
                        onSelect = { index ->
                            if (index != languageIndex) {
                                LocaleManager.persist(context, index)
                                LocaleManager.apply(index)
                                onLanguageChange(index)
                            }
                        }
                    )
                }
                if (context.resources.configuration.locales[0].language == "en") {
                    entry("language_ai_note") {
                        Text(
                            text = stringResource(R.string.language_ai_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        )
    }
}

/**
 * 状态卡片。
 *
 * 三态（照 MCGA 的 `XpStateCard`）：
 *   - 模块未激活        → errorContainer   + warning_filled
 *   - 已激活但 root 不可用 → tertiaryContainer + info_filled
 *   - 一切正常          → primary         + check_circle
 */
@Composable
private fun StatusCard(
    isActive: Boolean,
    isRootAvailable: Boolean,
    framework: XposedRepo.FrameworkInfo?
) {
    val container = when {
        !isActive -> MaterialTheme.colorScheme.errorContainer
        !isRootAvailable -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    val onContainer = when {
        !isActive -> MaterialTheme.colorScheme.onErrorContainer
        !isRootAvailable -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimary
    }
    val iconRes = when {
        !isActive -> R.drawable.warning_filled
        !isRootAvailable -> R.drawable.info_filled
        else -> R.drawable.check_circle
    }
    val headline = when {
        !isActive -> stringResource(R.string.module_not_activated)
        !isRootAvailable -> stringResource(R.string.root_not_available)
        else -> stringResource(R.string.activated)
    }

    // 副标题：激活后显示框架名与版本，未激活时给出操作指引
    val sub = if (isActive && framework != null) {
        buildString {
            append(framework.name.ifBlank { "LSPosed" })
            if (framework.version.isNotBlank()) append(" ").append(framework.version)
            append(" · API ").append(framework.apiVersion)
        }
    } else {
        stringResource(R.string.activation_hint)
    }

    Row(
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 16.dp)
            .fillMaxWidth()
            .clip(CapsuleShapes.single(24.dp))
            .background(container)
            .padding(vertical = 18.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = headline,
            tint = onContainer,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.padding(start = 18.dp)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = onContainer
            )
            Text(
                text = sub,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = onContainer
            )
        }
    }
}

// ==================================================================== 设置

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingPage(
    modifier: Modifier,
    config: CseConfig,
    onConfigChange: (CseConfig) -> Unit,
    store: ConfigStore
) {
    val masterEnabled = config.masterSwitch
    //     本 Composable 的宿主就是 MainActivity，二者解析到同一个实例；
    //     而 PanelActivity.start() 内部会判断 Context 是否为 Activity，
    //     据此决定要不要补 NEW_TASK（详细原因见该函数注释：
    //     加了 NEW_TASK 会让系统把转场从 activityOpen* 换成 taskOpen*，
    //     也就是用户看到的"淡入淡出"）。
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
    ) {
        SplicedColumnGroup(
            title = stringResource(R.string.group_module_function),
            entries = buildList {
                entry("master_switch") {
                    SwitchWidget(
                        iconRes = R.drawable.architecture,
                        title = stringResource(R.string.force_native_sc),
                        description = stringResource(R.string.force_native_sc_desc),
                        checked = config.masterSwitch,
                        onCheckedChange = {
                            store.setMasterSwitch(it)
                            onConfigChange(config.copy(masterSwitch = it))
                        }
                    )
                }
                entry("feature_icon") { shape ->
                    OptionWidget(
                        modifier = Modifier.clip(shape),
                        iconRes = R.drawable.android,
                        title = stringResource(R.string.feature_icon),
                        description = stringResource(R.string.feature_icon_desc),
                        enabled = masterEnabled,
                        onClick = {
                            PanelActivity.start(context, Destination.ICON)
                        }
                    )
                }
                entry("feature_background") { shape ->
                    OptionWidget(
                        modifier = Modifier.clip(shape),
                        iconRes = R.drawable.format_color_fill,
                        title = stringResource(R.string.feature_background),
                        description = stringResource(R.string.feature_background_desc),
                        enabled = masterEnabled,
                        onClick = {
                            PanelActivity.start(context, Destination.BACKGROUND)
                        }
                    )
                }
                entry("feature_animation") { shape ->
                    OptionWidget(
                        modifier = Modifier.clip(shape),
                        iconRes = R.drawable.animation,
                        title = stringResource(R.string.feature_animation),
                        description = stringResource(R.string.feature_animation_desc),
                        enabled = masterEnabled,
                        onClick = {
                            PanelActivity.start(context, Destination.ANIMATION)
                        }
                    )
                }
                entry("feature_misc") { shape ->
                    OptionWidget(
                        modifier = Modifier.clip(shape),
                        iconRes = R.drawable.grid_off_filled,
                        title = stringResource(R.string.feature_misc),
                        description = stringResource(R.string.feature_misc_desc),
                        enabled = masterEnabled,
                        onClick = {
                            PanelActivity.start(context, Destination.MISC)
                        }
                    )
                }
            }
        )
    }
}

// ==================================================================== 关于

@Composable
private fun AboutPage(modifier: Modifier) {
    val context = LocalContext.current

    //   格式：「0.3 · ID 33」—— versionName · versionCode。
    //   加版本 ID 的原因：用户/维护者自查问题时，光看 0.3 无法区分是第几次
    //   编译（同一 versionName 会对应多个内部构建），versionCode 才是唯一标识。
    val versionText = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            context.getString(
                R.string.about_version_with_id,
                pi.versionName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode.toInt()
                else @Suppress("DEPRECATION") pi.versionCode
            )
        }.getOrDefault("—")
    }
    val comingSoon = stringResource(R.string.coming_soon)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = TOP_BAR_SPACER, bottom = 16.dp)
    ) {
        // ---- 应用展示区（参照 COUI Expressive 的应用介绍页） ----
        // 主标题：应用名（中文名 / 英文名）；副行：互补的另一个名字 + 版本胶囊
        //
        //     - 中文环境：主标题「COS遮罩进化」 → 副行「CSE」      （补英文缩写）
        //     - 英文环境：主标题「COS SplashScreen Evolution」
        //                 → 副行「ColorOS SplashScreen Evolution」（补官方全称）
        //   两个名字都从资源取，切语言后同时改变，不会出现
        //   "主标题已经是英文名了、副行还挂着 CSE" 这种信息重复。
        //   资源归属见各 strings.xml 中 about_hero_title / _title_en 的注释。
        val heroTitle = stringResource(R.string.about_hero_title)
        val heroSecondary = stringResource(R.string.about_hero_title_en)
        val heroSubtitle = stringResource(R.string.about_hero_subtitle)
        AboutHero(
            appName = heroTitle,
            appNameEn = heroSecondary,
            subtitle = heroSubtitle,
            version = versionText
        )

        // ---- 作者 ----
        SplicedColumnGroup(
            title = stringResource(R.string.group_author),
            entries = buildList {
                entry("module_author") {
                    OptionWidget(
                        iconRes = R.drawable.ic_avatar_tiger,
                        title = stringResource(R.string.module_author),
                        description = AUTHOR_NAME,
                        tintIcon = false,
                        onClick = {}
                    )
                }
                entry("coolapk") {
                    OptionWidget(
                        //   原用 dock_to_bottom_filled（通用 Material 图标），与品牌无关。
                        iconRes = R.drawable.coolapk,
                        title = stringResource(R.string.coolapk_homepage),
                        description = COOLAPK_URL,
                        onClick = { openCoolapk(context, COOLAPK_URL) }
                    )
                }
            }
        )

        // ---- 关于 ----
        SplicedColumnGroup(
            title = stringResource(R.string.group_about),
            entries = buildList {
                entry("module_version") {
                    OptionWidget(
                        iconRes = R.drawable.info_filled,
                        title = stringResource(R.string.module_version),
                        description = versionText,
                        onClick = {}
                    )
                }
                entry("open_source") {
                    val url = stringResource(R.string.open_source_url)
                    OptionWidget(
                        iconRes = R.drawable.github,
                        title = stringResource(R.string.open_source_address),
                        description = url,
                        onClick = { openUrl(context, url) }
                    )
                }
            }
        )

        // ---- 鸣谢（鸣谢借鉴的项目） ----
        val urlSplashEvolution = stringResource(R.string.project_splash_evolution_url)
        val urlMcga = stringResource(R.string.project_mcga_url)
        SplicedColumnGroup(
            title = stringResource(R.string.group_credits),
            entries = buildList {
                entry("credits_title") {
                    OptionWidget(
                        iconRes = R.drawable.family_history,
                        title = stringResource(R.string.credits_title),
                        description = stringResource(R.string.credits_desc),
                        onClick = {}
                    )
                }
                entry("project_splash_evolution") {
                    OptionWidget(
                        iconRes = R.drawable.github,
                        title = stringResource(R.string.project_splash_evolution),
                        description = urlSplashEvolution,
                        onClick = { openUrl(context, urlSplashEvolution) }
                    )
                }
                entry("project_mcga") {
                    OptionWidget(
                        iconRes = R.drawable.github,
                        title = stringResource(R.string.project_mcga),
                        description = urlMcga,
                        onClick = { openUrl(context, urlMcga) }
                    )
                }
            }
        )
    }
}


@Composable
private fun AboutHero(
    appName: String,
    appNameEn: String,
    subtitle: String,
    version: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---- 应用图标：背景形状与首页左上角图标完全一致 ----
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colorResource(R.color.ic_launcher_background)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = appName,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- 主标题：应用中文名 ----
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        // ---- 英文缩写：紧贴主标题下方，字号小一档 ----
        if (appNameEn.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = appNameEn,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ---- 副标题：一句话定位（居中，允许折行） ----
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- 版本胶囊：版本名 + 版本 ID ----
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = version,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private const val AUTHOR_NAME = "云屿沫辰"
private const val COOLAPK_URL = "https://www.coolapk.com/u/39292342"

/** 酷安包名们：优先拉起酷安客户端打开链接，失败则回退到系统浏览器。 */
private val COOLAPK_PACKAGES = listOf(
    "com.coolapk.market",   // 酷安
    "com.coolapk.market.hd" // 酷安 HD
)

/**
 * 打开酷安链接：
 *   1. 先尝试用酷安客户端的 Scheme（coolmarket://）深链打开；
 *   2. 不行则尝试用酷安包名 + ACTION_VIEW 打开；
 *   3. 都不行则回退到系统浏览器。
 */
private fun openCoolapk(context: Context, url: String) {
    // 1) 酷安自定义 Scheme
    val schemeUrl = "coolmarket://u/39292342"
    val schemeOk = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(schemeUrl)).apply {
                setPackage(COOLAPK_PACKAGES.first())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        true
    }.getOrDefault(false)
    if (schemeOk) return

    // 2) 用酷安包名直接打开 https 链接
    for (pkg in COOLAPK_PACKAGES) {
        val ok = runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage(pkg)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            true
        }.getOrDefault(false)
        if (ok) return
    }

    // 3) 回退系统浏览器
    openUrl(context, url)
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}

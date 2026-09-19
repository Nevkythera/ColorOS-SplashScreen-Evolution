package com.Nevkythera.ColorOSSplashScreenEvolution.ui.navgation

/**
 * 二级功能页标识 —— 对应 MCGA 的 `Destination`。
 *
 * 主界面三个页签（首页/设置/关于）用 `HorizontalPager` 承载，**不是**页面路由；
 * 只有从设置页点进「图标 / 背景 / 杂项」这类二级页时才产生一次跳转。
 *
 * ★ 本版起二级页由**独立 Activity**（`PanelActivity`）承载，
 *   而非 `NavHost` 的 `composable(route)`。原因见 `PanelActivity` 类注释：
 *   用户要求跨页面动画走系统原生的 Activity 转场。
 *   [route] 于是从「NavHost 路由字符串」变成了「Activity 参数值」，
 *   语义不变但不再依赖 Navigation 组件。
 */
enum class Destination(val route: String) {
    ICON(route = "icon"),
    BACKGROUND(route = "background"),
    ANIMATION(route = "animation"),
    EXIT_ANIMATION(route = "exit_animation"),
    MISC(route = "misc");

    companion object {
        /** 按 [route] 反查，未匹配返回 null（供 `PanelActivity` 解析 Intent 参数）。 */
        fun fromRoute(route: String?): Destination? =
            entries.firstOrNull { it.route == route }
    }
}

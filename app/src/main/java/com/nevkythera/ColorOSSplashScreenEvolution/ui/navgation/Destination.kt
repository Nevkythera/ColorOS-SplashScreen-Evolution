package com.Nevkythera.ColorOSSplashScreenEvolution.ui.navgation


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

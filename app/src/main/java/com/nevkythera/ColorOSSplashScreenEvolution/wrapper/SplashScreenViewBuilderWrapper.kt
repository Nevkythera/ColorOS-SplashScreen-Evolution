package com.Nevkythera.ColorOSSplashScreenEvolution.wrapper

import android.graphics.drawable.Drawable

/**
 * SplashScreenView.Builder 的反射包装类 —— 从 RestoreSplashScreen 迁移。
 *
 * 原项目用 YukiHookAPI 的 `instance.current().field{...}` 反射语法，
 * 这里改用标准 Java 反射，适配 libxposed 的 Hook 环境。
 *
 * 仅保留"替换背景颜色"所需的方法（setBackgroundColor 等），
 * 其余 setter/getter 按需补充。
 */
class SplashScreenViewBuilderWrapper private constructor(private val builder: Any) {

    companion object {
        private val instances: MutableMap<Any, SplashScreenViewBuilderWrapper> = mutableMapOf()

        /**
         * 获取给定 SplashScreenView.Builder 对应的单例实例。
         */
        fun getInstance(builder: Any): SplashScreenViewBuilderWrapper {
            if (builder.javaClass.name != "android.window.SplashScreenView\$Builder") {
                throw IllegalArgumentException("Builder must be of type SplashScreenView\$Builder")
            }
            return instances.getOrPut(builder) { SplashScreenViewBuilderWrapper(builder) }
        }
    }

    private fun field(name: String): java.lang.reflect.Field =
        builder.javaClass.getDeclaredField(name).apply { isAccessible = true }

    private fun method(name: String, vararg types: Class<*>): java.lang.reflect.Method =
        builder.javaClass.getDeclaredMethod(name, *types).apply { isAccessible = true }

    /** Get the rectangle size for the center view. */
    fun getIconSize(): Int = field("mIconSize").getInt(builder)

    /** Get the background color for the view. */
    fun getBackgroundColor(): Int = field("mBackgroundColor").getInt(builder)

    /** Get the Drawable object to fill the entire view. */
    fun getOverlayDrawable(): Drawable? = field("mOverlayDrawable").get(builder) as? Drawable

    /** Get the Drawable object to fill the center view. */
    fun getCenterViewDrawable(): Drawable? = field("mIconDrawable").get(builder) as? Drawable

    /** Get the background color for the icon. */
    fun getIconBackground(): Drawable? = field("mIconBackground").get(builder) as? Drawable

    /** Get the Drawable object and size for the branding view. */
    fun getBrandingDrawable(): Drawable? = field("mBrandingDrawable").get(builder) as? Drawable

    /** Set the background color for the view. */
    fun setBackgroundColor(backgroundColor: Int) =
        method("setBackgroundColor", Int::class.javaPrimitiveType!!).invoke(builder, backgroundColor)

    /** Set the Drawable object to fill the entire view. */
    fun setOverlayDrawable(drawable: Drawable?) =
        method("setOverlayDrawable", Drawable::class.java).invoke(builder, drawable)

    /** Set the Drawable object to fill the center view. */
    fun setCenterViewDrawable(drawable: Drawable?) =
        method("setCenterViewDrawable", Drawable::class.java).invoke(builder, drawable)

    /** Set the background color for the icon. */
    fun setIconBackground(iconBackground: Drawable) =
        method("setIconBackground", Drawable::class.java).invoke(builder, iconBackground)

    /** Set the Drawable object and size for the branding view. */
    fun setBrandingDrawable(branding: Drawable?, width: Int, height: Int) =
        method("setBrandingDrawable", Drawable::class.java, Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!).invoke(builder, branding, width, height)

    /** Set the rectangle size for the center view. */
    fun setIconSize(iconSize: Int) =
        method("setIconSize", Int::class.javaPrimitiveType!!).invoke(builder, iconSize)
}

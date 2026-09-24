package com.android.wm.shell.back

import android.content.res.Configuration
import android.window.BackNavigationInfo

interface BackAnimation

open class ShellBackAnimation(vararg args: Any?) {
    open fun onConfigurationChanged(newConfig: Configuration): Unit = Unit
    open fun getRunner(): BackAnimationRunner = BackAnimationRunner()
    open fun prepareNextAnimation(
        animationInfo: BackNavigationInfo.CustomAnimationInfo?,
        letterboxColor: Int
    ): Any? = null
    open fun onBackStarted(vararg a: Any?): Unit = Unit
    open fun onBackProgressed(vararg a: Any?): Unit = Unit
    open fun onBackCancelled(vararg a: Any?): Unit = Unit
    open fun onBackInvoked(vararg a: Any?): Unit = Unit
}

open class BackAnimationRunner(vararg args: Any?) {
    open fun onBackStarted(vararg a: Any?): Unit = Unit
    open fun onBackProgressed(vararg a: Any?): Unit = Unit
    open fun onBackCancelled(vararg a: Any?): Unit = Unit
    open fun onBackInvoked(vararg a: Any?): Unit = Unit
}

object BackAnimationConstants {
    @JvmField val BACK_GESTURE_ANIM_DURATION_MS: Int = 300
    @JvmField val BACK_GESTURE_MAX_DURATION_MS: Int = 500
    @JvmField val UPDATE_SYSUI_FLAGS_THRESHOLD: Int = 0
}

class BackAnimationBackground(vararg args: Any?) {
    fun ensureBackground(vararg a: Any?): Unit = Unit
    fun removeBackground(vararg a: Any?): Unit = Unit
    fun customizeStatusBarAppearance(vararg a: Any?): Unit = Unit
    fun resetStatusBarCustomization(vararg a: Any?): Unit = Unit
}

class ProgressVelocityTracker(vararg args: Any?) {
    fun addPosition(vararg a: Any?): Unit = Unit
    fun resetTracking(vararg a: Any?): Unit = Unit
    fun calculateVelocity(vararg a: Any?): Float = 0f
}

class StatusBarCustomizer(vararg args: Any?) {
    fun customizeStatusBarAppearance(vararg a: Any?): Unit = Unit
    fun resetStatusBarCustomization(vararg a: Any?): Unit = Unit
}

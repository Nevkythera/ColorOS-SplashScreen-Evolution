package com.nevkythera.cse.back

import android.content.Context
import android.os.Handler
import android.view.SurfaceControl
import android.window.BackEvent
import com.android.wm.shell.RootTaskDisplayAreaOrganizer
import com.android.wm.shell.back.BackAnimationBackground

/**
 * 最小具体子类：把 AOSP 的 CrossActivityBackAnimation 实例化。
 * 四个抽象成员是 ROM 侧的策略值，这里采用 AOSP 默认口径。
 */
class AospBackAnimation(
    context: Context,
    background: BackAnimationBackground,
    rootTaskDisplayAreaOrganizer: RootTaskDisplayAreaOrganizer,
    transaction: SurfaceControl.Transaction,
    handler: Handler,
) : CrossActivityBackAnimation(context, background, rootTaskDisplayAreaOrganizer, transaction, handler) {

    override val allowEnteringYShift: Boolean = false

    override fun preparePreCommitClosingRectMovement(@BackEvent.SwipeEdge swipeEdge: Int) = Unit

    override fun preparePreCommitEnteringRectMovement() = Unit

    override fun getPostCommitAnimationDuration(): Long = 300L
}

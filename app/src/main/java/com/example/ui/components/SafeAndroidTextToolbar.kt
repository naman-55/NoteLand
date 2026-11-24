package com.example.ui.components

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.core.view.isNotEmpty
import androidx.core.view.size
import kotlin.math.roundToInt

/**
 * Lifecycle-safe Compose TextToolbar that guards against double-finishing
 * Android's TYPE_FLOATING ActionMode when selection actions (such as Paste/Cut)
 * trigger TextToolbar.hide() before ActionMode.Callback.onActionItemClicked returns.
 */
class SafeAndroidTextToolbar(
    private val view: View
) : TextToolbar {

    private var actionMode: ActionMode? = null
    private var isFinishingOrDestroyed: Boolean = false

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    private var contentRect: Rect = Rect.Zero
    private var onCopyRequested: (() -> Unit)? = null
    private var onPasteRequested: (() -> Unit)? = null
    private var onCutRequested: (() -> Unit)? = null
    private var onSelectAllRequested: (() -> Unit)? = null

    private val actionModeCallback = object : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            requireNotNull(menu) { "onCreateActionMode requires a non-null menu" }
            requireNotNull(mode) { "onCreateActionMode requires a non-null mode" }
            populateMenu(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) return false
            val previousSize = menu.size
            menu.clear()
            populateMenu(menu)
            return menu.isNotEmpty() || previousSize > 0
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                MENU_ID_COPY -> onCopyRequested?.invoke()
                MENU_ID_PASTE -> onPasteRequested?.invoke()
                MENU_ID_CUT -> onCutRequested?.invoke()
                MENU_ID_SELECT_ALL -> onSelectAllRequested?.invoke()
                else -> return false
            }
            finishActionModeSafely(mode)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            if (actionMode === mode || mode == null) {
                actionMode = null
                isFinishingOrDestroyed = true
                status = TextToolbarStatus.Hidden
            }
        }

        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: android.graphics.Rect?) {
            outRect?.set(
                contentRect.left.roundToInt(),
                contentRect.top.roundToInt(),
                contentRect.right.roundToInt(),
                contentRect.bottom.roundToInt()
            )
        }
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        this.contentRect = rect
        this.onCopyRequested = onCopyRequested
        this.onPasteRequested = onPasteRequested
        this.onCutRequested = onCutRequested
        this.onSelectAllRequested = onSelectAllRequested

        val hasAnyAction = onCopyRequested != null ||
            onPasteRequested != null ||
            onCutRequested != null ||
            onSelectAllRequested != null
        if (!hasAnyAction) {
            hide()
            return
        }

        val existing = actionMode
        if (existing != null && !isFinishingOrDestroyed) {
            status = TextToolbarStatus.Shown
            existing.invalidateContentRect()
            existing.invalidate()
        } else {
            isFinishingOrDestroyed = false
            val started = view.startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING)
            actionMode = started
            status = if (started != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
        }
    }

    override fun hide() {
        finishActionModeSafely(actionMode)
        status = TextToolbarStatus.Hidden
    }

    private fun finishActionModeSafely(targetMode: ActionMode?) {
        val current = actionMode
        if (targetMode == null || current !== targetMode || isFinishingOrDestroyed) {
            return
        }
        isFinishingOrDestroyed = true
        actionMode = null
        status = TextToolbarStatus.Hidden
        current.finish()
    }

    private fun populateMenu(menu: Menu) {
        if (onCopyRequested != null) {
            menu.add(Menu.NONE, MENU_ID_COPY, ORDER_COPY, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (onPasteRequested != null) {
            menu.add(Menu.NONE, MENU_ID_PASTE, ORDER_PASTE, android.R.string.paste)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (onCutRequested != null) {
            menu.add(Menu.NONE, MENU_ID_CUT, ORDER_CUT, android.R.string.cut)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        if (onSelectAllRequested != null) {
            menu.add(Menu.NONE, MENU_ID_SELECT_ALL, ORDER_SELECT_ALL, android.R.string.selectAll)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
    }

    private companion object {
        const val MENU_ID_COPY = 0
        const val MENU_ID_PASTE = 1
        const val MENU_ID_CUT = 2
        const val MENU_ID_SELECT_ALL = 3

        const val ORDER_COPY = 0
        const val ORDER_PASTE = 1
        const val ORDER_CUT = 2
        const val ORDER_SELECT_ALL = 3
    }
}

package com.mahi.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Screen automation: text dhore tap, input-e type, scroll, back/home. */
class MahiAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: MahiAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun tapText(text: String): String {
        val root = rootInActiveWindow ?: return "Screen dekha jacche na."
        for (node in root.findAccessibilityNodeInfosByText(text)) {
            var cur: AccessibilityNodeInfo? = node
            while (cur != null) {
                if (cur.isClickable) {
                    cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return "'$text' e tap korlam."
                }
                cur = cur.parent
            }
        }
        return "'$text' screen e pawa jay ni."
    }

    fun typeText(text: String): String {
        val root = rootInActiveWindow ?: return "Screen dekha jacche na."
        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: return "Kono input field focus e nei."
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return "Likhe diyechi."
    }

    fun scroll(down: Boolean): String {
        val root = rootInActiveWindow ?: return "Screen dekha jacche na."
        val node = findScrollable(root) ?: return "Scroll kora jacche na."
        node.performAction(
            if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        )
        return if (down) "Niche scroll korlam." else "Upore scroll korlam."
    }

    fun globalAction(action: Int): String {
        performGlobalAction(action)
        return "Hoyeche."
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findScrollable(child)?.let { return it }
        }
        return null
    }
}

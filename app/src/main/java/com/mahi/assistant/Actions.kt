package com.mahi.assistant

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SmsManager
import org.json.JSONObject

/** Gemini theke asa action ke phone-e execute kore. Ferot dey ekta chhoto status text. */
object Actions {

    fun run(ctx: Context, a: JSONObject): String = try {
        when (a.optString("type")) {
            "open_app" -> openApp(ctx, a.optString("app"))
            "call" -> call(ctx, a.optString("number"))
            "sms" -> sms(a.optString("number"), a.optString("text"))
            "answer_call" -> answerCall(ctx)
            "tap_text" -> withA11y { it.tapText(a.optString("text")) }
            "type_text" -> withA11y { it.typeText(a.optString("text")) }
            "scroll_down" -> withA11y { it.scroll(true) }
            "scroll_up" -> withA11y { it.scroll(false) }
            "back" -> withA11y { it.globalAction(AccessibilityService.GLOBAL_ACTION_BACK) }
            "home" -> withA11y { it.globalAction(AccessibilityService.GLOBAL_ACTION_HOME) }
            else -> ""
        }
    } catch (e: SecurityException) {
        "Permission nei: ${e.message}"
    } catch (e: Exception) {
        "Kaj korte parlam na: ${e.message}"
    }

    private fun withA11y(block: (MahiAccessibilityService) -> String): String {
        val service = MahiAccessibilityService.instance
            ?: return "Accessibility chalu kora nai Boss. Settings e giye MAHI ke on koro."
        return block(service)
    }

    private fun openApp(ctx: Context, name: String): String {
        val pm = ctx.packageManager
        val target = name.trim().lowercase()
        if (target.isEmpty()) return "Kon app kholbo bolo."
        val pkg = if (target.contains('.')) target
        else pm.getInstalledApplications(0)
            .firstOrNull { pm.getApplicationLabel(it).toString().lowercase().contains(target) }
            ?.packageName
            ?: return "'$name' naam-er app khuje paini."
        val intent = pm.getLaunchIntentForPackage(pkg)
            ?: return "App ta kholte parchi na."
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        return "$name khulchi."
    }

    private fun call(ctx: Context, number: String): String {
        if (number.isBlank()) return "Number ta bujhte parchi na."
        ctx.startActivity(
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return "$number e call kortesi."
    }

    @Suppress("DEPRECATION")
    private fun sms(number: String, text: String): String {
        if (number.isBlank() || text.isBlank()) return "Number ar message dui-ta-i lagbe."
        val mgr: SmsManager = if (Build.VERSION.SDK_INT >= 31) {
            MahiApp.context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
        mgr.sendTextMessage(number, null, text, null, null)
        return "$number ke SMS pathano holo."
    }

    private fun answerCall(ctx: Context): String {
        val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        tm.acceptRingingCall()
        return "Call receive korlam."
    }
}

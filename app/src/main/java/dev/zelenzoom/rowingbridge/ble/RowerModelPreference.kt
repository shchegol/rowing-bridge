package dev.zelenzoom.rowingbridge.ble

import android.content.Context

private const val PREFS_NAME = "rower_settings"
private const val KEY_MODEL_OVERRIDE = "model_override"

/** Manual override for which RowerModel profile to use; null means auto-detect by BLE device name (RowerQuirksRegistry). */
object RowerModelPreference {

    fun load(context: Context): RowerModel? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODEL_OVERRIDE, null) ?: return null
        return runCatching { RowerModel.valueOf(stored) }.getOrNull()
    }

    fun save(context: Context, model: RowerModel?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            if (model == null) remove(KEY_MODEL_OVERRIDE) else putString(KEY_MODEL_OVERRIDE, model.name)
        }.apply()
    }
}

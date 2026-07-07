package dev.zelenzoom.rowingbridge.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.zelenzoom.rowingbridge.BuildConfig
import dev.zelenzoom.rowingbridge.strava.StravaAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private const val PREFS_NAME = "settings"
private const val KEY_THEME_MODE = "theme_mode"

/**
 * Theme choice is our own concern (persisted in plain SharedPreferences,
 * applied reactively - no Activity recreation needed). Language is not
 * stored by us at all: AppCompatDelegate.setApplicationLocales() already
 * persists the choice between launches and recreates the Activity itself
 * (see ui/theme + MainActivity for why AppCompatActivity is required).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val stravaAuthManager = StravaAuthManager(application)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _stravaConnected = MutableStateFlow(stravaAuthManager.isConnected())
    val stravaConnected: StateFlow<Boolean> = _stravaConnected.asStateFlow()

    /**
     * Strava requires a paid API plan the user doesn't want to buy unless/
     * until they actually need this feature - disable the whole UI entry
     * point until real credentials land in local.properties, rather than
     * letting a click open a broken OAuth page with an empty client_id.
     */
    val stravaAvailable: Boolean = BuildConfig.STRAVA_CLIENT_ID.isNotBlank()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    /** Current app-language override tag (e.g. "ru"), or null for "follow system". */
    fun currentLanguageTag(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales[0]?.toLanguageTag()
    }

    /** Pass null to follow the system language. */
    fun setLanguageTag(tag: String?) {
        AppCompatDelegate.setApplicationLocales(
            if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    fun connectStrava() = stravaAuthManager.launchAuthorize()

    fun disconnectStrava() {
        stravaAuthManager.disconnect()
        _stravaConnected.value = false
    }

    /** Called from MainActivity's onCreate/onNewIntent when the OAuth Custom Tab redirects back. */
    fun handleStravaRedirect(uri: Uri) {
        viewModelScope.launch {
            if (stravaAuthManager.handleRedirect(uri)) {
                _stravaConnected.value = stravaAuthManager.isConnected()
            }
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }
}

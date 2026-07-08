package dev.zelenzoom.rowingbridge.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zelenzoom.rowingbridge.BuildConfig
import dev.zelenzoom.rowingbridge.R
import dev.zelenzoom.rowingbridge.support.openSponsorsPage

/** Settings screen: theme (applied immediately, no restart) and app language (recreates the Activity via AppCompatDelegate). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    currentLanguageTag: String?,
    onLanguageChange: (String?) -> Unit,
    stravaAvailable: Boolean,
    stravaConnected: Boolean,
    onConnectStrava: () -> Unit,
    onDisconnectStrava: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp)) {
            SectionHeader(stringResource(R.string.settings_theme_section))
            RadioRow(stringResource(R.string.theme_system), themeMode == ThemeMode.SYSTEM) { onThemeModeChange(ThemeMode.SYSTEM) }
            RadioRow(stringResource(R.string.theme_light), themeMode == ThemeMode.LIGHT) { onThemeModeChange(ThemeMode.LIGHT) }
            RadioRow(stringResource(R.string.theme_dark), themeMode == ThemeMode.DARK) { onThemeModeChange(ThemeMode.DARK) }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(stringResource(R.string.settings_language_section))
            LanguageSelect(currentLanguageTag, onLanguageChange)

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(stringResource(R.string.settings_strava_section))
            StravaSection(stravaAvailable, stravaConnected, onConnectStrava, onDisconnectStrava)

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(stringResource(R.string.settings_support_section))
            SupportSection()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(
                    R.string.settings_version_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.GIT_SHA,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun StravaSection(available: Boolean, connected: Boolean, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    if (connected) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(stringResource(R.string.strava_connected), modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onDisconnect) {
                Text(stringResource(R.string.strava_disconnect))
            }
        }
    } else {
        Button(
            onClick = onConnect,
            enabled = available,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Text(stringResource(R.string.strava_connect))
        }
        if (!available) {
            Text(
                text = stringResource(R.string.strava_not_available),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SupportSection() {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { openSponsorsPage(context) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text(stringResource(R.string.support_github_sponsors))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

/** Compact select/dropdown (not a radio list) - stays small as more languages/settings get added later. */
@Composable
private fun LanguageSelect(currentLanguageTag: String?, onLanguageChange: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        null to stringResource(R.string.language_system),
        "en" to stringResource(R.string.language_english),
        "ru" to stringResource(R.string.language_russian),
        "es" to stringResource(R.string.language_spanish),
    )
    val currentLabel = options.firstOrNull { it.first == currentLanguageTag }?.second ?: options[0].second

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(currentLabel, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { (tag, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onLanguageChange(tag); expanded = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                )
            }
        }
    }
}

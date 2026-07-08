package dev.zelenzoom.rowingbridge.support

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

private const val SPONSORS_URL = "https://github.com/sponsors/shchegol"

/** Opens the GitHub Sponsors page via Custom Tabs - no in-app payment API exists, this is the standard way to link out to it. */
fun openSponsorsPage(context: Context) {
    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(SPONSORS_URL))
}

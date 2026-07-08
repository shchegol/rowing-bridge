package dev.zelenzoom.rowingbridge.support

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/shchegol"

/** Opens the Buy Me a Coffee page via Custom Tabs - no in-app payment API exists, this is the standard way to link out to it. Doesn't require the supporter to have a GitHub account, unlike Sponsors (which stays linked from README only). */
fun openBuyMeACoffeePage(context: Context) {
    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(BUY_ME_A_COFFEE_URL))
}

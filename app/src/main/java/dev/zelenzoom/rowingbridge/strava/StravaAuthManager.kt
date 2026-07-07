package dev.zelenzoom.rowingbridge.strava

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import dev.zelenzoom.rowingbridge.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val REDIRECT_URI = "rowingbridge://oauth-callback"
private const val AUTHORIZE_URL = "https://www.strava.com/oauth/mobile/authorize"
private const val TOKEN_URL = "https://www.strava.com/oauth/token"

/**
 * Hand-rolled Strava OAuth2 flow (Custom Tabs + authorization-code exchange)
 * - deliberately not using AppAuth, which would be overkill for Strava's
 * simple two-step flow (no PKCE/discovery document required). Requires the
 * user to have registered their own Strava API app and put the Client ID/
 * Secret in local.properties (see the project plan for the exact
 * Authorization Callback Domain needed: "oauth-callback").
 */
class StravaAuthManager(private val context: Context) {

    private val tokenStore = TokenStore(context)
    private val httpClient = OkHttpClient()

    fun isConnected(): Boolean = tokenStore.load() != null

    fun disconnect() = tokenStore.clear()

    fun launchAuthorize() {
        val url = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:write")
            .build()
        CustomTabsIntent.Builder().build().launchUrl(context, url)
    }

    /** Call with the redirect deep-link Intent's data Uri. Returns true if it was ours and tokens were saved. */
    suspend fun handleRedirect(uri: Uri): Boolean {
        if (uri.scheme != "rowingbridge" || uri.host != "oauth-callback") return false
        val code = uri.getQueryParameter("code") ?: return false
        return exchangeCodeForTokens(code)
    }

    private suspend fun exchangeCodeForTokens(code: String): Boolean = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .build()
        runCatching {
            httpClient.newCall(Request.Builder().url(TOKEN_URL).post(body).build()).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val json = JSONObject(response.body?.string().orEmpty())
                tokenStore.save(
                    StravaTokens(
                        accessToken = json.getString("access_token"),
                        refreshToken = json.getString("refresh_token"),
                        expiresAtEpochSeconds = json.getLong("expires_at"),
                    ),
                )
                true
            }
        }.getOrDefault(false)
    }

    /** A valid access token, refreshing first if expired/near-expiry. Null if not connected or refresh failed. */
    suspend fun ensureFreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val tokens = tokenStore.load() ?: return@withContext null
        val nowPlusBuffer = System.currentTimeMillis() / 1000 + 60
        if (tokens.expiresAtEpochSeconds > nowPlusBuffer) return@withContext tokens.accessToken

        val body = FormBody.Builder()
            .add("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .add("client_secret", BuildConfig.STRAVA_CLIENT_SECRET)
            .add("grant_type", "refresh_token")
            .add("refresh_token", tokens.refreshToken)
            .build()
        runCatching {
            httpClient.newCall(Request.Builder().url(TOKEN_URL).post(body).build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val json = JSONObject(response.body?.string().orEmpty())
                val newTokens = StravaTokens(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.getString("refresh_token"),
                    expiresAtEpochSeconds = json.getLong("expires_at"),
                )
                tokenStore.save(newTokens)
                newTokens.accessToken
            }
        }.getOrNull()
    }
}

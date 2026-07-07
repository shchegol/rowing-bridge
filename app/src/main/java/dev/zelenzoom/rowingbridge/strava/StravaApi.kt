package dev.zelenzoom.rowingbridge.strava

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private const val UPLOAD_URL = "https://www.strava.com/api/v3/uploads"
private const val MAX_POLL_ATTEMPTS = 15
private const val POLL_INTERVAL_MS = 2000L

/** Uploads a finished workout's FIT file to Strava (POST /uploads, then polls until processed). */
class StravaApi(private val authManager: StravaAuthManager) {

    private val httpClient = OkHttpClient()

    suspend fun upload(fitBytes: ByteArray, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val accessToken = authManager.ensureFreshAccessToken()
                ?: return@withContext Result.failure(IllegalStateException("Not connected to Strava"))

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data_type", "fit")
                .addFormDataPart("file", fileName, fitBytes.toRequestBody("application/octet-stream".toMediaType()))
                .build()
            val uploadRequest = Request.Builder()
                .url(UPLOAD_URL)
                .header("Authorization", "Bearer $accessToken")
                .post(body)
                .build()

            val uploadId = httpClient.newCall(uploadRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Upload request failed: ${response.code}")
                JSONObject(response.body?.string().orEmpty()).getLong("id")
            }

            pollUntilDone(uploadId, accessToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pollUntilDone(uploadId: Long, accessToken: String): Result<Unit> {
        val statusRequest = Request.Builder()
            .url("$UPLOAD_URL/$uploadId")
            .header("Authorization", "Bearer $accessToken")
            .build()

        repeat(MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            val status = httpClient.newCall(statusRequest).execute().use { response ->
                if (!response.isSuccessful) null else JSONObject(response.body?.string().orEmpty())
            } ?: return@repeat

            val error = status.optString("error")
            if (error.isNotEmpty()) return Result.failure(IllegalStateException(error))
            if (!status.isNull("activity_id")) return Result.success(Unit)
        }
        return Result.failure(IllegalStateException("Strava upload timed out waiting for processing"))
    }
}

package io.zwiftboard.android

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

internal data class ZwiftUnlockTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
)

internal class ZwiftUnlockClient(context: Context) {
    private val prefs = context.getSharedPreferences("zwift-unlock", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()

    fun ready(): Boolean = prefs.getString(KEY_REFRESH_TOKEN, null)?.isNotBlank() == true

    fun clear() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).remove(KEY_EXPIRES_AT).apply()
    }

    fun login(email: String, password: String): Boolean {
        val tokens = requestTokenGrant(
            grantType = "password",
            username = email,
            password = password,
        ) ?: return false
        saveTokens(tokens)
        return true
    }

    fun loginAsync(email: String, password: String, callback: (Boolean) -> Unit) {
        executor.execute {
            callback(login(email, password))
        }
    }

    fun solveAsync(challenge: ByteArray, callback: (ByteArray?) -> Unit) {
        executor.execute {
            try {
                callback(solve(challenge))
            } catch (error: Exception) {
                Log.w(TAG, "zwift unlock solve failed", error)
                callback(null)
            }
        }
    }

    fun solve(challenge: ByteArray): ByteArray? {
        if (challenge.isEmpty()) return null
        val tokens = loadTokens() ?: return null
        val now = System.currentTimeMillis()
        val token = if (now >= tokens.expiresAtMillis - 60_000L) {
            val refreshed = refreshToken(tokens.refreshToken) ?: return null
            saveTokens(refreshed)
            refreshed
        } else {
            tokens
        }
        return authenticate(token.accessToken, challenge)
    }

    private fun authenticate(accessToken: String, challenge: ByteArray): ByteArray? {
        val conn = (URL(DEFAULT_DLOCK_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/x-protobuf-lite")
            setRequestProperty("Accept", "application/x-protobuf-lite")
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
        }
        return try {
            conn.outputStream.use { it.write(challenge) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.use { it.readBytes() } ?: ByteArray(0)
            when {
                code == 401 -> {
                    Log.w(TAG, "d-lock request unauthorized (HTTP 401)")
                    null
                }
                code != 200 -> {
                    Log.w(TAG, "d-lock request failed with HTTP $code")
                    null
                }
                body.isEmpty() -> null
                else -> body
            }
        } catch (error: Exception) {
            Log.w(TAG, "d-lock request failed", error)
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun refreshToken(refreshToken: String): ZwiftUnlockTokens? {
        val tokens = requestTokenGrant(
            grantType = "refresh_token",
            refreshToken = refreshToken,
        ) ?: return null
        if (tokens.refreshToken.isBlank()) {
            return tokens.copy(refreshToken = refreshToken)
        }
        return tokens
    }

    private fun requestTokenGrant(
        grantType: String,
        username: String? = null,
        password: String? = null,
        refreshToken: String? = null,
    ): ZwiftUnlockTokens? {
        val form = buildString {
            append("grant_type=").append(URLEncoder.encode(grantType, StandardCharsets.UTF_8.name()))
            append("&client_id=").append(URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8.name()))
            if (username != null) {
                append("&username=").append(URLEncoder.encode(username, StandardCharsets.UTF_8.name()))
            }
            if (password != null) {
                append("&password=").append(URLEncoder.encode(password, StandardCharsets.UTF_8.name()))
            }
            if (refreshToken != null) {
                append("&refresh_token=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8.name()))
            }
        }
        val conn = (URL(DEFAULT_TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
        }
        return try {
            conn.outputStream.use { it.write(form.toByteArray(StandardCharsets.UTF_8)) }
            val code = conn.responseCode
            val raw = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.use { it.readBytes() } ?: ByteArray(0)
            if (code != 200) {
                val error = try {
                    JSONObject(String(raw, StandardCharsets.UTF_8)).optString("error", "")
                } catch (_: Exception) {
                    ""
                }
                Log.w(TAG, "token request failed: HTTP $code error=$error")
                return null
            }
            val json = JSONObject(String(raw, StandardCharsets.UTF_8))
            val access = json.optString("access_token", "")
            val refresh = json.optString("refresh_token", "")
            val expiresIn = json.optLong("expires_in", 0L)
            if (access.isBlank() || (refresh.isBlank() && refreshToken == null)) {
                return null
            }
            val refreshValue = if (refresh.isNotBlank()) refresh else refreshToken ?: ""
            val expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L)
            ZwiftUnlockTokens(access, refreshValue, expiresAtMillis)
        } catch (error: Exception) {
            Log.w(TAG, "token request failed", error)
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun loadTokens(): ZwiftUnlockTokens? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (accessToken.isBlank() || refreshToken.isBlank() || expiresAtMillis <= 0L) {
            return null
        }
        return ZwiftUnlockTokens(accessToken, refreshToken, expiresAtMillis)
    }

    private fun saveTokens(tokens: ZwiftUnlockTokens) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            .putLong(KEY_EXPIRES_AT, tokens.expiresAtMillis)
            .apply()
    }

    companion object {
        private const val TAG = "ZwiftUnlock"
        private const val CLIENT_ID = "Zwift Game Client"
        private const val DEFAULT_TOKEN_URL = "https://secure.zwift.com/auth/realms/zwift/protocol/openid-connect/token"
        private const val DEFAULT_DLOCK_URL = "https://us-or-rly101.zwift.com/api/d-lock-service/device/authenticate"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}

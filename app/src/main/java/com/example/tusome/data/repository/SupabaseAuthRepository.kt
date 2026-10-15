package com.example.tusome.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val roles: List<String>,
    val gradeId: String?,
    val schoolId: String?,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long = 0L
)

data class SignUpResult(val session: AuthSession?, val message: String)

object SupabaseAuthRepository {
    private const val BASE_URL = "https://wkwnoflielrotknvoflz.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_jZsmwIcIMdu3Eq97AfU3xQ_929Bxo4J"
    private const val PREFS = "tusome_secure_session"
    private const val SESSION_KEY = "encrypted_session"
    private const val KEY_ALIAS = "TusomeSessionEncryptionKey"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun currentSession(context: Context): AuthSession? =
        runCatching { readSession(context.applicationContext) }.getOrNull()

    fun signIn(
        context: Context,
        email: String,
        password: String,
        onResult: (Result<AuthSession>) -> Unit
    ) {
        val appContext = context.applicationContext
        executor.execute {
            val result = runCatching {
                val payload = JSONObject()
                    .put("email", email.trim())
                    .put("password", password)
                val auth = request("/auth/v1/token?grant_type=password", payload, null) as JSONObject
                val session = sessionFromAuth(auth, appContext)
                saveSession(appContext, session)
                session
            }
            mainHandler.post { onResult(result) }
        }
    }

    fun signUp(
        context: Context,
        displayName: String,
        email: String,
        password: String,
        onResult: (Result<SignUpResult>) -> Unit
    ) {
        val appContext = context.applicationContext
        executor.execute {
            val result = runCatching {
                val payload = JSONObject()
                    .put("email", email.trim())
                    .put("password", password)
                    .put("data", JSONObject().put("display_name", displayName.trim()))
                val auth = request("/auth/v1/signup", payload, null) as JSONObject
                val accessToken = auth.optString("access_token")
                if (accessToken.isBlank()) {
                    SignUpResult(
                        session = null,
                        message = "Account created. Check your email to confirm the account, then sign in."
                    )
                } else {
                    val session = sessionFromAuth(auth, appContext)
                    saveSession(appContext, session)
                    SignUpResult(session, "Account created.")
                }
            }
            mainHandler.post { onResult(result) }
        }
    }

    /** Refreshes a session before its access token expires. Network failures preserve the
     * encrypted session so the app can continue to provide its offline-capable screens. */
    fun refreshSessionIfNeeded(
        context: Context,
        onResult: (Result<AuthSession?>) -> Unit
    ) {
        val appContext = context.applicationContext
        executor.execute {
            val result = runCatching {
                val existing = readSession(appContext) ?: return@runCatching null
                if (existing.expiresAtMillis > System.currentTimeMillis() + 60_000L) {
                    return@runCatching existing
                }
                if (existing.refreshToken.isBlank()) {
                    throw IllegalStateException("Your session has expired. Please sign in again.")
                }
                val payload = JSONObject().put("refresh_token", existing.refreshToken)
                val auth = request("/auth/v1/token?grant_type=refresh_token", payload, null) as JSONObject
                val refreshed = sessionFromAuth(auth, appContext)
                saveSession(appContext, refreshed)
                refreshed
            }
            mainHandler.post { onResult(result) }
        }
    }

    /** Calls the server-authorized school/user management function using the signed-in JWT. */
    fun manageSchoolUsers(
        context: Context,
        payload: JSONObject,
        onResult: (Result<JSONObject>) -> Unit
    ) {
        refreshSessionIfNeeded(context) { refreshed ->
            val session = refreshed.getOrNull()
            if (session == null) {
                onResult(Result.failure(refreshed.exceptionOrNull()
                    ?: IllegalStateException("Sign in is required to manage school users.")))
                return@refreshSessionIfNeeded
            }
            executor.execute {
                val result = runCatching {
                    request("/functions/v1/manage-school-users", payload, session.accessToken) as JSONObject
                }
                mainHandler.post { onResult(result) }
            }
        }
    }

    /**
     * Refreshes profile and role assignments from Supabase after admin changes.
     * If the network is unavailable, the encrypted cached session remains intact.
     */
    fun refreshCurrentUserData(
        context: Context,
        onResult: (Result<AuthSession>) -> Unit
    ) {
        val appContext = context.applicationContext
        refreshSessionIfNeeded(appContext) { refreshed ->
            val session = refreshed.getOrNull()
            if (session == null) {
                onResult(Result.failure(refreshed.exceptionOrNull()
                    ?: IllegalStateException("Sign in is required to refresh your profile.")))
                return@refreshSessionIfNeeded
            }

            executor.execute {
                val result = runCatching {
                    val profileRows = request(
                        "/rest/v1/user_profiles?user_id=eq.${session.userId}&select=display_name,grade_id,school_id",
                        null,
                        session.accessToken
                    ) as? JSONArray ?: JSONArray()
                    val profile = profileRows.optJSONObject(0) ?: JSONObject()

                    val roleRows = request(
                        "/rest/v1/user_role_assignments?user_id=eq.${session.userId}&select=role_key",
                        null,
                        session.accessToken
                    ) as? JSONArray ?: JSONArray()
                    val roles = buildList {
                        for (index in 0 until roleRows.length()) {
                            roleRows.optJSONObject(index)?.optString("role_key")
                                ?.takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }.distinct()

                    val updated = session.copy(
                        displayName = profile.optString("display_name")
                            .ifBlank { session.displayName.ifBlank { session.email.substringBefore("@") } },
                        roles = roles,
                        gradeId = profile.optString("grade_id")
                            .takeIf { it.isNotBlank() && it != "null" },
                        schoolId = profile.optString("school_id")
                            .takeIf { it.isNotBlank() && it != "null" }
                    )
                    saveSession(appContext, updated)
                    updated
                }
                mainHandler.post { onResult(result) }
            }
        }
    }

    fun signOut(context: Context) {
        val appContext = context.applicationContext
        val session = currentSession(appContext)
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(SESSION_KEY)
            .apply()

        // Revoke the server-side refresh session when possible; local credentials are cleared immediately.
        if (session != null && session.accessToken.isNotBlank()) {
            executor.execute {
                runCatching {
                    val connection = (URL("$BASE_URL/auth/v1/logout").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 8000
                        readTimeout = 8000
                        doOutput = true
                        setRequestProperty("apikey", PUBLISHABLE_KEY)
                        setRequestProperty("Authorization", "Bearer ${session.accessToken}")
                        setRequestProperty("Content-Type", "application/json")
                    }
                    try {
                        connection.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
                        connection.responseCode
                    } finally {
                        connection.disconnect()
                    }
                }
            }
        }
    }

    private fun sessionFromAuth(auth: JSONObject, context: Context): AuthSession {
        val token = auth.optString("access_token")
        val refreshToken = auth.optString("refresh_token")
        val user = auth.optJSONObject("user")
            ?: throw IllegalStateException("Supabase did not return a user account.")
        val userId = user.optString("id")
        val email = user.optString("email")
        if (token.isBlank() || userId.isBlank()) {
            throw IllegalStateException("The account is not signed in yet. Confirm your email if prompted.")
        }

        val profileRows = request(
            "/rest/v1/user_profiles?user_id=eq.$userId&select=display_name,grade_id,school_id",
            null,
            token
        )
        val profile = if (profileRows is JSONArray && profileRows.length() > 0) {
            profileRows.getJSONObject(0)
        } else {
            JSONObject()
        }

        val roleRows = request(
            "/rest/v1/user_role_assignments?user_id=eq.$userId&select=role_key",
            null,
            token
        ) as? JSONArray ?: JSONArray()
        val roles = buildList {
            for (index in 0 until roleRows.length()) {
                roleRows.optJSONObject(index)?.optString("role_key")
                    ?.takeIf { it.isNotBlank() }?.let(::add)
            }
        }.distinct()

        return AuthSession(
            userId = userId,
            email = email,
            displayName = profile.optString("display_name").ifBlank {
                user.optJSONObject("user_metadata")?.optString("display_name").orEmpty().ifBlank {
                    email.substringBefore("@").ifBlank { "Tusome user" }
                }
            },
            roles = roles,
            gradeId = profile.optString("grade_id").takeIf { it.isNotBlank() && it != "null" },
            schoolId = profile.optString("school_id").takeIf { it.isNotBlank() && it != "null" },
            accessToken = token,
            refreshToken = refreshToken,
            expiresAtMillis = System.currentTimeMillis() + (auth.optLong("expires_in", 3600L).coerceAtLeast(1L) * 1000L)
        )
    }

    private fun request(path: String, payload: JSONObject?, accessToken: String?): Any {
        val connection = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
            requestMethod = if (payload == null) "GET" else "POST"
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            if (accessToken != null) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            } else {
                setRequestProperty("Authorization", "Bearer $PUBLISHABLE_KEY")
            }
            if (payload != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }

        try {
            if (payload != null) {
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { JSONObject(body) }.getOrNull()
                val message = error?.optString("msg")?.takeIf { it.isNotBlank() }
                    ?: error?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: error?.optString("error_description")?.takeIf { it.isNotBlank() }
                    ?: error?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Request failed (HTTP $status). Check your details and try again."
                throw IllegalStateException(message)
            }
            return if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body.ifBlank { "{}" })
        } finally {
            connection.disconnect()
        }
    }

    private fun saveSession(context: Context, session: AuthSession) {
        val json = JSONObject()
            .put("userId", session.userId)
            .put("email", session.email)
            .put("displayName", session.displayName)
            .put("roles", JSONArray(session.roles))
            .put("gradeId", session.gradeId)
            .put("schoolId", session.schoolId)
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("expiresAtMillis", session.expiresAtMillis)
            .toString()
        val encrypted = encrypt(json.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(SESSION_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    private fun readSession(context: Context): AuthSession? {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(SESSION_KEY, null) ?: return null
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val json = JSONObject(String(decrypt(bytes), Charsets.UTF_8))
        val rolesJson = json.optJSONArray("roles") ?: JSONArray()
        val roles = buildList {
            for (i in 0 until rolesJson.length()) rolesJson.optString(i).takeIf { it.isNotBlank() }?.let(::add)
        }
        val token = json.optString("accessToken")
        if (token.isBlank()) return null
        return AuthSession(
            userId = json.optString("userId"),
            email = json.optString("email"),
            displayName = json.optString("displayName"),
            roles = roles,
            gradeId = json.optString("gradeId").takeIf { it.isNotBlank() && it != "null" },
            schoolId = json.optString("schoolId").takeIf { it.isNotBlank() && it != "null" },
            accessToken = token,
            refreshToken = json.optString("refreshToken"),
            expiresAtMillis = json.optLong("expiresAtMillis", 0L)
        )
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plainText)
        return cipher.iv + encrypted
    }

    private fun decrypt(input: ByteArray): ByteArray {
        require(input.size > 12) { "Invalid encrypted session." }
        val iv = input.copyOfRange(0, 12)
        val encrypted = input.copyOfRange(12, input.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }
}

package com.example.tusome.data.repository

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.example.tusome.data.model.LearningContentBlock
import com.example.tusome.data.model.LearningContentBlockType
import com.example.tusome.data.model.LearningResource
import com.example.tusome.data.model.LearningResourceSection
import com.example.tusome.data.model.LearningResourceSource
import com.example.tusome.data.model.LearningResourceStatus
import com.example.tusome.data.model.LearningResourceType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

data class LearningResourceFetchResult(
    val resources: List<LearningResource>,
    val fromCache: Boolean,
    val lastUpdatedAtMillis: Long?
)

/**
 * Read-only learner access to reviewed, published supplementary resources.
 *
 * Writes and AI generation are deliberately excluded from the Android client. Those
 * operations must go through authenticated Edge Functions so role checks, review history
 * and provider credentials remain server-side.
 */
object LearningResourceRepository {
    private const val BASE_URL = "https://wkwnoflielrotknvoflz.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_jZsmwIcIMdu3Eq97AfU3xQ_929Bxo4J"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Loads resources for one canonical sub-strand. Cached content is used automatically
     * when the network is unavailable; learners never need the AI service to study.
     */
    fun fetchPublishedForSubStrand(
        context: Context,
        gradeCode: String,
        learningAreaId: String,
        strandId: String,
        subStrandId: String,
        languageCode: String? = null,
        onResult: (Result<LearningResourceFetchResult>) -> Unit
    ) {
        val appContext = context.applicationContext
        val normalizedGrade = gradeCode.trim().lowercase()
        val normalizedLanguage = languageCode?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        if (normalizedGrade.isBlank() || subStrandId.isBlank()) {
            mainHandler.post {
                onResult(Result.failure(IllegalArgumentException("A grade and sub-strand are required.")))
            }
            return
        }

        SupabaseAuthRepository.refreshSessionIfNeeded(appContext) { refreshed ->
            val session = refreshed.getOrNull()
                ?: SupabaseAuthRepository.currentSession(appContext)
            val cacheKey = cacheKey(
                normalizedGrade,
                subStrandId,
                normalizedLanguage,
                session?.userId,
                session?.schoolId
            )
            val accessToken = session?.takeIf {
                it.expiresAtMillis > System.currentTimeMillis()
            }?.accessToken

            executor.execute {
                val result = runCatching {
                    val body = requestPublishedResources(
                        gradeCode = normalizedGrade,
                        learningAreaId = learningAreaId,
                        strandId = strandId,
                        subStrandId = subStrandId,
                        languageCode = normalizedLanguage,
                        accessToken = accessToken
                    )
                    val resources = parseResources(body)
                    saveCache(appContext, cacheKey, body)
                    LearningResourceFetchResult(
                        resources = resources,
                        fromCache = false,
                        lastUpdatedAtMillis = System.currentTimeMillis()
                    )
                }.recoverCatching { networkError ->
                    val cached = readCache(appContext, cacheKey)
                        ?: throw networkError
                    val savedAt = cacheFile(appContext, cacheKey).lastModified()
                    LearningResourceFetchResult(
                        resources = parseResources(cached),
                        fromCache = true,
                        lastUpdatedAtMillis = savedAt.takeIf { it > 0L }
                    )
                }
                mainHandler.post { onResult(result) }
            }
        }
    }

    private fun requestPublishedResources(
        gradeCode: String,
        learningAreaId: String,
        strandId: String,
        subStrandId: String,
        languageCode: String?,
        accessToken: String?
    ): String {
        val filters = buildList {
            add("select=id,grade_id,learning_area_id,strand_id,sub_strand_id,resource_type,title,summary,language_code,status,source,version,estimated_minutes,learning_outcomes,content,created_at,updated_at,published_at")
            add("grade_id=eq.${Uri.encode(gradeCode)}")
            add("sub_strand_id=eq.${Uri.encode(subStrandId)}")
            add("status=eq.published")
            if (learningAreaId.isNotBlank()) add("learning_area_id=eq.${Uri.encode(learningAreaId)}")
            if (strandId.isNotBlank()) add("strand_id=eq.${Uri.encode(strandId)}")
            if (languageCode != null) add("language_code=eq.${Uri.encode(languageCode)}")
            add("order=published_at.desc")
        }
        val endpoint = "$BASE_URL/rest/v1/learning_resources?${filters.joinToString("&")}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            if (accessToken != null) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            setRequestProperty("Accept", "application/json")
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
            }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { JSONObject(body) }.getOrNull()
                val message = error?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: error?.optString("details")?.takeIf { it.isNotBlank() }
                    ?: "Unable to load published learning resources (HTTP $status)."
                throw IllegalStateException(message)
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResources(body: String): List<LearningResource> {
        val rows = JSONArray(body)
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = row.optString("id")
                val subStrandId = row.optString("sub_strand_id")
                val type = LearningResourceType.fromKey(row.optString("resource_type"))
                val status = LearningResourceStatus.fromKey(row.optString("status"))
                val source = LearningResourceSource.fromKey(row.optString("source"))
                if (id.isBlank() || subStrandId.isBlank() || type == null ||
                    status != LearningResourceStatus.PUBLISHED || source == null
                ) continue

                val content = row.optJSONObject("content") ?: JSONObject()
                val sectionsJson = content.optJSONArray("sections") ?: JSONArray()
                val sections = buildList {
                    for (sectionIndex in 0 until sectionsJson.length()) {
                        val section = sectionsJson.optJSONObject(sectionIndex) ?: continue
                        val blocksJson = section.optJSONArray("blocks") ?: JSONArray()
                        val blocks = buildList {
                            for (blockIndex in 0 until blocksJson.length()) {
                                val block = blocksJson.optJSONObject(blockIndex) ?: continue
                                val blockType = LearningContentBlockType.fromKey(
                                    block.optString("type")
                                ) ?: LearningContentBlockType.PARAGRAPH
                                add(
                                    LearningContentBlock(
                                        id = block.optString("id").ifBlank {
                                            "block-${sectionIndex + 1}-${blockIndex + 1}"
                                        },
                                        type = blockType,
                                        text = block.optString("text"),
                                        items = stringArray(block.optJSONArray("items")),
                                        mediaUrl = block.optString("media_url")
                                            .takeIf { it.isNotBlank() && it != "null" },
                                        caption = block.optString("caption")
                                            .takeIf { it.isNotBlank() && it != "null" }
                                    )
                                )
                            }
                        }
                        add(
                            LearningResourceSection(
                                id = section.optString("id").ifBlank { "section-${sectionIndex + 1}" },
                                title = section.optString("title"),
                                description = section.optString("description"),
                                blocks = blocks
                            )
                        )
                    }
                }

                val resource = LearningResource(
                    id = id,
                    curriculumNodeId = subStrandId,
                    gradeCode = row.optString("grade_id").uppercase(),
                    learningAreaId = row.optString("learning_area_id"),
                    strandId = row.optString("strand_id"),
                    subStrandId = subStrandId,
                    type = type,
                    title = row.optString("title"),
                    summary = row.optString("summary"),
                    languageCode = row.optString("language_code", "en"),
                    status = status,
                    source = source,
                    version = row.optInt("version", 1),
                    estimatedMinutes = row.optInt("estimated_minutes").takeIf {
                        !row.isNull("estimated_minutes") && it > 0
                    },
                    learningOutcomes = stringArray(row.optJSONArray("learning_outcomes")),
                    sections = sections,
                    createdAt = row.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                    updatedAt = row.optString("updated_at").takeIf { it.isNotBlank() && it != "null" },
                    publishedAt = row.optString("published_at").takeIf { it.isNotBlank() && it != "null" }
                )
                if (resource.hasUsableContent && resource.isAvailableToLearners) add(resource)
            }
        }
    }

    private fun stringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun cacheKey(
        gradeCode: String,
        subStrandId: String,
        languageCode: String?,
        userId: String?,
        schoolId: String?
    ): String {
        val language = languageCode ?: "all"
        val userScope = userId ?: "anonymous"
        val schoolScope = schoolId ?: "global"
        return "resources_${gradeCode}_${subStrandId}_${language}_${userScope}_$schoolScope"
    }

    /**
     * Lesson bodies can be substantially larger than progress/settings values, so keep
     * offline resource payloads in private app files rather than SharedPreferences.
     */
    private fun cacheFile(context: Context, key: String): File {
        val directory = File(context.filesDir, "published_learning_resources")
        if (!directory.exists()) directory.mkdirs()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(directory, "$digest.json")
    }

    private fun saveCache(context: Context, key: String, body: String) {
        val destination = cacheFile(context, key)
        val temporary = File(destination.parentFile, destination.name + ".tmp")
        temporary.writeText(body, Charsets.UTF_8)
        if (!temporary.renameTo(destination)) {
            destination.writeText(body, Charsets.UTF_8)
            temporary.delete()
        }
    }

    private fun readCache(context: Context, key: String): String? {
        val file = cacheFile(context, key)
        return file.takeIf { it.isFile && it.length() > 0L }?.readText(Charsets.UTF_8)
    }
}

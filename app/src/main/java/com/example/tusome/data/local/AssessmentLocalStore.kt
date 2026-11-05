package com.example.tusome.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Durable offline storage for assessment papers and learner attempts.
 * Paper payloads are cached per published version; attempts are isolated by local user ID.
 */
class AssessmentLocalStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cached_assessment_papers (
                paper_id TEXT NOT NULL,
                paper_version INTEGER NOT NULL,
                grade_id TEXT NOT NULL,
                learning_area_id TEXT NOT NULL,
                paper_json TEXT NOT NULL,
                cached_at INTEGER NOT NULL,
                PRIMARY KEY (paper_id, paper_version)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX cached_assessment_grade_area_idx
            ON cached_assessment_papers(grade_id, learning_area_id, cached_at DESC)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE learner_assessment_attempts_local (
                attempt_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                paper_id TEXT NOT NULL,
                paper_version INTEGER NOT NULL,
                answers_json TEXT NOT NULL DEFAULT '{}',
                started_at INTEGER NOT NULL,
                submitted_at INTEGER,
                sync_state TEXT NOT NULL DEFAULT 'pending'
                    CHECK (sync_state IN ('pending', 'synced', 'failed')),
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, attempt_id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX learner_assessment_pending_idx
            ON learner_assessment_attempts_local(user_id, sync_state, updated_at)
            """.trimIndent()
        )
        createMarkingGuideTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createMarkingGuideTable(db)
    }

    private fun createMarkingGuideTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cached_assessment_marking_guides (
                paper_id TEXT NOT NULL,
                paper_version INTEGER NOT NULL,
                marking_json TEXT NOT NULL,
                cached_at INTEGER NOT NULL,
                PRIMARY KEY (paper_id, paper_version)
            )
            """.trimIndent()
        )
    }

    fun cacheMarkingGuide(paperId: String, paperVersion: Int, markingJson: String) {
        require(paperId.isNotBlank() && paperVersion > 0)
        JSONArray(markingJson)
        val values = ContentValues().apply {
            put("paper_id", paperId)
            put("paper_version", paperVersion)
            put("marking_json", markingJson)
            put("cached_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "cached_assessment_marking_guides", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getCachedMarkingGuide(paperId: String, paperVersion: Int): String? =
        readableDatabase.query(
            "cached_assessment_marking_guides",
            arrayOf("marking_json"),
            "paper_id = ? AND paper_version = ?",
            arrayOf(paperId, paperVersion.toString()),
            null, null, null, "1"
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    fun cachePaper(
        paperId: String,
        paperVersion: Int,
        gradeId: String,
        learningAreaId: String,
        paperJson: String
    ) {
        require(paperId.isNotBlank() && gradeId.isNotBlank() && learningAreaId.isNotBlank())
        require(paperVersion > 0)
        JSONObject(paperJson) // Reject malformed payloads instead of caching unusable records.
        val values = ContentValues().apply {
            put("paper_id", paperId)
            put("paper_version", paperVersion)
            put("grade_id", gradeId)
            put("learning_area_id", learningAreaId)
            put("paper_json", paperJson)
            put("cached_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "cached_assessment_papers",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getCachedPapers(gradeId: String, learningAreaId: String): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.query(
            "cached_assessment_papers",
            arrayOf("paper_json"),
            "grade_id = ? AND learning_area_id = ?",
            arrayOf(gradeId, learningAreaId),
            null,
            null,
            "cached_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.getString(0)
        }
        return result
    }

    fun getCachedPaper(paperId: String, paperVersion: Int? = null): String? {
        val selection = if (paperVersion == null) "paper_id = ?" else "paper_id = ? AND paper_version = ?"
        val args = if (paperVersion == null) arrayOf(paperId) else arrayOf(paperId, paperVersion.toString())
        return readableDatabase.query(
            "cached_assessment_papers",
            arrayOf("paper_json"),
            selection,
            args,
            null,
            null,
            "paper_version DESC",
            "1"
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }

    fun saveAttempt(
        attemptId: String,
        userId: String,
        paperId: String,
        paperVersion: Int,
        answersJson: String,
        startedAt: Long,
        submittedAt: Long?,
        syncState: String = "pending"
    ) {
        require(attemptId.isNotBlank() && userId.isNotBlank() && paperId.isNotBlank())
        require(paperVersion > 0)
        require(syncState in setOf("pending", "synced", "failed"))
        JSONObject(answersJson)
        val values = ContentValues().apply {
            put("attempt_id", attemptId)
            put("user_id", userId)
            put("paper_id", paperId)
            put("paper_version", paperVersion)
            put("answers_json", answersJson)
            put("started_at", startedAt)
            if (submittedAt == null) putNull("submitted_at") else put("submitted_at", submittedAt)
            put("sync_state", syncState)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "learner_assessment_attempts_local",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getLatestAttemptForPaper(userId: String, paperId: String): JSONObject? {
        return readableDatabase.query(
            "learner_assessment_attempts_local",
            arrayOf("attempt_id", "paper_version", "answers_json", "started_at", "submitted_at", "sync_state"),
            "user_id = ? AND paper_id = ?",
            arrayOf(userId, paperId),
            null,
            null,
            "updated_at DESC",
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            JSONObject()
                .put("attempt_id", cursor.getString(0))
                .put("paper_version", cursor.getInt(1))
                .put("answers", JSONObject(cursor.getString(2)))
                .put("started_at", cursor.getLong(3))
                .put("submitted_at", if (cursor.isNull(4)) JSONObject.NULL else cursor.getLong(4))
                .put("sync_state", cursor.getString(5))
        }
    }

    fun getPendingAttempts(userId: String): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        readableDatabase.query(
            "learner_assessment_attempts_local",
            arrayOf(
                "attempt_id", "user_id", "paper_id", "paper_version",
                "answers_json", "started_at", "submitted_at", "updated_at"
            ),
            "user_id = ? AND sync_state <> 'synced'",
            arrayOf(userId),
            null,
            null,
            "updated_at ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += JSONObject()
                    .put("attempt_id", cursor.getString(0))
                    .put("user_id", cursor.getString(1))
                    .put("paper_id", cursor.getString(2))
                    .put("paper_version", cursor.getInt(3))
                    .put("answers", JSONObject(cursor.getString(4)))
                    .put("started_at", cursor.getLong(5))
                    .put("submitted_at", if (cursor.isNull(6)) JSONObject.NULL else cursor.getLong(6))
                    .put("client_updated_at", cursor.getLong(7))
            }
        }
        return result
    }

    fun markAttemptSynced(userId: String, attemptId: String) {
        val values = ContentValues().apply {
            put("sync_state", "synced")
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(
            "learner_assessment_attempts_local",
            values,
            "user_id = ? AND attempt_id = ?",
            arrayOf(userId, attemptId)
        )
    }

    companion object {
        private const val DATABASE_NAME = "tusome_assessments.db"
        private const val DATABASE_VERSION = 2
    }
}

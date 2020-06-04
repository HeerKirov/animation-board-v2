package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.RecordProgress
import com.heerkirov.animation.util.ktorm.NullableDateTimeListConverter
import com.heerkirov.animation.util.ktorm.json
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object RecordProgresses : BaseTable<RecordProgress>("record_progress") {
    val id by long("id").primaryKey()
    val recordId by long("record_id")
    val ordinal by int("ordinal")
    val watchedEpisodes by int("watched_episodes")
    val watchedRecord by json("watched_record", NullableDateTimeListConverter())
    val startTime by datetime("start_time")
    val finishTime by datetime("finish_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = RecordProgress(
            id = row[id]!!,
            recordId = row[recordId]!!,
            ordinal = row[ordinal]!!,
            watchedEpisodes = row[watchedEpisodes]!!,
            watchedRecord = row[watchedRecord]!!,
            startTime = row[startTime],
            finishTime = row[finishTime]
    )
}
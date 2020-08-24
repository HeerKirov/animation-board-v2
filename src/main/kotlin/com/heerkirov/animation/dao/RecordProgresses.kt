package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.RecordProgress
import com.heerkirov.animation.util.ktorm.DateTimeConverter
import com.heerkirov.animation.util.ktorm.NullableListConverter
import com.heerkirov.animation.util.ktorm.json
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object RecordProgresses : BaseTable<RecordProgress>("record_progress") {
    val id = long("id").primaryKey()
    val recordId = long("record_id")
    val ordinal = int("ordinal")
    val watchedEpisodes = int("watched_episodes")
    val watchedRecord = json("watched_record", NullableListConverter(DateTimeConverter()))
    val startTime = datetime("start_time")
    val finishTime = datetime("finish_time")

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
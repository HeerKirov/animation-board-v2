package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.Record
import com.heerkirov.animation.model.data.WatchedRecord
import com.heerkirov.animation.util.ktorm.enum
import com.heerkirov.animation.util.ktorm.json
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Records : BaseTable<Record>("record") {
    val id by long("id").primaryKey()
    val ownerId by int("owner_id")
    val animationId by int("animation_id")
    val seenOriginal by boolean("seen_original")
    val status by enum("status", typeRef<RecordStatus>())
    val inDiary by boolean("in_diary")
    val watchedEpisodes by int("watched_quantity")
    val progressCount by int("progress_count")
    val latestProgressId by long("latest_progress_id")
    val watchedRecord by json("watched_record", typeRef<List<WatchedRecord>>())
    val subscriptionTime by datetime("subscription_time")
    val finishTime by datetime("finish_time")
    val lastActiveTime by datetime("last_active_time")
    val lastActiveEvent by json("last_active_event", typeRef<ActiveEvent>())
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Record(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            animationId = row[animationId]!!,
            seenOriginal = row[seenOriginal]!!,
            status = row[status]!!,
            inDiary = row[inDiary]!!,
            watchedEpisodes = row[watchedEpisodes]!!,
            progressCount = row[progressCount]!!,
            latestProgressId = row[latestProgressId],
            watchedRecord = row[watchedRecord]!!,
            subscriptionTime = row[subscriptionTime],
            finishTime = row[finishTime],
            lastActiveTime = row[lastActiveTime],
            lastActiveEvent = row[lastActiveEvent]!!,
            createTime = row[createTime]!!,
            updateTime = row[updateTime]!!
    )
}
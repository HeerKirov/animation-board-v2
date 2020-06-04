package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.Record
import com.heerkirov.animation.model.data.ScatterRecord
import com.heerkirov.animation.util.ktorm.json
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Records : BaseTable<Record>("record") {
    val id by long("id").primaryKey()
    val ownerId by int("owner_id")
    val animationId by int("animation_id")
    val seenOriginal by boolean("seen_original")
    val inDiary by boolean("in_diary")
    val scatterRecord by json("scatter_record", typeRef<List<ScatterRecord>>())
    val progressCount by int("progress_count")
    val lastActiveTime by datetime("last_active_time")
    val lastActiveEvent by json("last_active_event", typeRef<ActiveEvent>())
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Record(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            animationId = row[animationId]!!,
            seenOriginal = row[seenOriginal]!!,
            inDiary = row[inDiary]!!,
            progressCount = row[progressCount]!!,
            scatterRecord = row[scatterRecord]!!,
            lastActiveTime = row[lastActiveTime],
            lastActiveEvent = row[lastActiveEvent]!!,
            createTime = row[createTime]!!,
            updateTime = row[updateTime]!!
    )
}
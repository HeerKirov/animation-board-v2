package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.Record
import com.heerkirov.animation.model.data.ScatterRecord
import com.heerkirov.animation.util.ktorm.json
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.*

object Records : BaseTable<Record>("record") {
    val id = long("id").primaryKey()
    val ownerId = int("owner_id")
    val animationId = int("animation_id")
    val seenOriginal = boolean("seen_original")
    val inDiary = boolean("in_diary")
    val scatterRecord = json("scatter_record", typeRef<List<ScatterRecord>>())
    val progressCount = int("progress_count")
    val lastActiveTime = datetime("last_active_time")
    val lastActiveEvent = json("last_active_event", typeRef<ActiveEvent>())
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")

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
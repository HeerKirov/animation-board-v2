package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.util.logger
import org.ktorm.database.Database
import org.ktorm.dsl.insert
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy

class TransformDiary(private val userLoader: UserLoader,
                     private val v1Database: Database,
                     private val database: Database) {
    private val log = logger<TransformDiary>()

    fun transform(animationIdMap: Map<Long, Int>) {
        var num = 0
        for (v1Diary in v1Database.sequenceOf(V1Diaries).sortedBy { it.id }) {
            if(v1Diary.status != "GIVE_UP") {
                val id = database.insertAndGenerateKey(Records) {
                    set(it.ownerId, userLoader[v1Diary.ownerId].id)
                    set(it.animationId, animationIdMap[v1Diary.animationId])
                    set(it.seenOriginal, v1Diary.watchOriginalWork)
                    set(it.inDiary, v1Diary.status == "READY" || v1Diary.status == "WATCHING")
                    set(it.scatterRecord, emptyList())
                    set(it.progressCount, 1)

                    set(it.lastActiveEvent, ActiveEvent(ActiveEventType.CREATE_RECORD, null))
                    set(it.lastActiveTime, null)
                    set(it.createTime, v1Diary.createTime.toV2Time())
                    set(it.updateTime, v1Diary.updateTime?.toV2Time() ?: v1Diary.createTime.toV2Time())
                } as Long

                database.insert(RecordProgresses) {
                    set(it.recordId, id)
                    set(it.ordinal, 1)
                    set(it.watchedEpisodes, v1Diary.watchedQuantity)
                    set(it.watchedRecord, v1Diary.watchedRecord.map { t -> t?.toV2Time() })
                    set(it.startTime, v1Diary.subscriptionTime?.toV2Time())
                    set(it.finishTime, v1Diary.finishTime?.toV2Time())
                }
                num += 1
            }
        }

        log.info("Transform $num records from v1.")
    }
}
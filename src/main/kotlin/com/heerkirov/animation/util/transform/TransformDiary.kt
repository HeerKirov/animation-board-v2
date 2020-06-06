package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.insert
import me.liuwj.ktorm.dsl.insertAndGenerateKey
import me.liuwj.ktorm.entity.sequenceOf

class TransformDiary(private val userLoader: UserLoader,
                     private val v1Database: Database,
                     private val database: Database) {
    private val log = logger<TransformDiary>()

    fun transform(animationIdMap: Map<Long, Int>) {
        var num = 0
        for (v1Diary in v1Database.sequenceOf(V1Diaries)) {
            if(v1Diary.status != "GIVE_UP") {
                val id = database.insertAndGenerateKey(Records) {
                    it.ownerId to userLoader[v1Diary.ownerId].id
                    it.animationId to animationIdMap[v1Diary.animationId]
                    it.seenOriginal to v1Diary.watchOriginalWork
                    it.inDiary to (v1Diary.status == "READY" || v1Diary.status == "WATCHING")
                    it.scatterRecord to emptyList()
                    it.progressCount to 1

                    it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_RECORD, null)
                    it.lastActiveTime to null
                    it.createTime to v1Diary.createTime.toV2Time()
                    it.updateTime to (v1Diary.updateTime?.toV2Time() ?: v1Diary.createTime.toV2Time())
                } as Long

                database.insert(RecordProgresses) {
                    it.recordId to id
                    it.ordinal to 1
                    it.watchedEpisodes to v1Diary.watchedQuantity
                    it.watchedRecord to v1Diary.watchedRecord.map { t -> t?.toV2Time() }
                    it.startTime to v1Diary.subscriptionTime?.toV2Time()
                    it.finishTime to v1Diary.finishTime?.toV2Time()
                }
                num += 1
            }
        }

        log.info("Transform $num records from v1.")
    }
}
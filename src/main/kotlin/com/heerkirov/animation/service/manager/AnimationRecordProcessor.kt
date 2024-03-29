package com.heerkirov.animation.service.manager

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.util.DateTimeUtil
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimationRecordProcessor(@Autowired private val database: Database) {
    fun updateRecord(animationId: Int, totalEpisodes: Int, publishedEpisodes: Int, oldTotalEpisodes: Int, oldPublishedEpisodes: Int) {
        //总体来说，total或published发生更新，有以下几种情况：
        // - total增加：由于上限增加，所有已完成的progress的记录的watchedEpisodes跟不上。经过考虑，因为实在太难遇到会引起困惑的情况了，只做最简单处理，去掉finishTime。
        // - total减少：total减少不一定伴随published减少，且可能导致watched == total，从而需要更新finishTime。
        // - published减少：所有progress的watchedEpisode可能超过published。需要将它们减去。
        // - published增加：可能只是正常的番剧更新，所以也不需要做什么。
        if(publishedEpisodes < oldPublishedEpisodes || totalEpisodes < oldTotalEpisodes) {
            val rowSets = database.from(RecordProgresses)
                    .innerJoin(Records, RecordProgresses.recordId eq Records.id)
                    .innerJoin(Animations, Records.animationId eq Animations.id)
                    .select(RecordProgresses.id, RecordProgresses.watchedEpisodes, RecordProgresses.watchedRecord)
                    .where { (Animations.id eq animationId) and (RecordProgresses.watchedEpisodes greaterEq publishedEpisodes) }

            val now = DateTimeUtil.now()

            database.batchUpdate(RecordProgresses) {
                rowSets.forEach { row ->
                    val id = row[RecordProgresses.id]!!
                    val watchedEpisodes = row[RecordProgresses.watchedEpisodes]!!
                    val newWatchedEpisodes = if(watchedEpisodes > publishedEpisodes) publishedEpisodes else watchedEpisodes
                    val watchedRecord = row[RecordProgresses.watchedRecord]!!
                    val newFinishTime = if(newWatchedEpisodes >= totalEpisodes) watchedRecord[newWatchedEpisodes] ?: now else null
                    item {
                        where { it.id eq id }
                        set(it.watchedEpisodes, newWatchedEpisodes)
                        set(it.finishTime, newFinishTime)
                    }
                }
            }
        }
        if(totalEpisodes > oldTotalEpisodes) {
            val rowSets = database.from(RecordProgresses)
                    .innerJoin(Records, RecordProgresses.recordId eq Records.id)
                    .innerJoin(Animations, Records.animationId eq Animations.id)
                    .select(RecordProgresses.id)
                    .where { (Animations.id eq animationId) and (RecordProgresses.finishTime.isNotNull()) }

            database.batchUpdate(RecordProgresses) {
                rowSets.forEach { row ->
                    val id = row[RecordProgresses.id]!!
                    item {
                        where { it.id eq id }
                        set(it.finishTime, null)
                    }
                }
            }
        }
    }

    fun deleteRecords(animationId: Int) {
        val ids = database.from(Records).select(Records.id).where { Records.animationId eq animationId }.map { it[Records.id]!! }
        if(ids.isNotEmpty()) database.delete(RecordProgresses) { it.recordId inList ids }
        database.delete(Records) { it.animationId eq animationId }
    }
}
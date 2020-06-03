package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.ProgressCreateForm
import com.heerkirov.animation.model.result.NextRes
import com.heerkirov.animation.model.result.ProgressRes
import com.heerkirov.animation.service.RecordProgressService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecordProgressServiceImpl(@Autowired private val database: Database,
                                @Autowired private val recordProcessor: RecordProcessor) : RecordProgressService {
    @Transactional
    override fun nextEpisode(animationId: Int, user: User): NextRes {
        val rowSet = database.from(Records)
                .innerJoin(Animations, Animations.id eq Records.animationId)
                .leftJoin(RecordProgresses, RecordProgresses.id eq Records.latestProgressId)
                .select(Records.id, Records.status, Records.watchedEpisodes, Records.progressCount, Records.latestProgressId,
                        Animations.totalEpisodes, Animations.publishedEpisodes,
                        RecordProgresses.watchedRecord)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
        val watchedEpisodes = rowSet[Records.watchedEpisodes]!!

        //这种情况直接返回，是无法更新的
        if(watchedEpisodes >= publishedEpisodes) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "No next episode.")
        }

        val recordId = rowSet[Records.id]!!
        val latestProgressId = rowSet[Records.latestProgressId]
        val progressCount = rowSet[Records.progressCount]!!
        val totalEpisodes = rowSet[Animations.totalEpisodes]!!

        val toCompleted = watchedEpisodes + 1 >= totalEpisodes
        val now = DateTimeUtil.now()

        val newProgressId = if(latestProgressId == null) {
            //没有进度，需要先创建一个进度
            database.insertAndGenerateKey(RecordProgresses) {
                it.ordinal to 1
                it.recordId to recordId
                it.watchedRecord to arrayListOf(now)
                it.startTime to now
                it.finishTime to if(toCompleted) { now }else{ null }
            } as Long
        }else{
            //有进度，那么更新此进度
            database.update(RecordProgresses) {
                where { it.id eq latestProgressId }
                it.watchedRecord to recordProcessor.calculateProgressWatchedRecord(rowSet[RecordProgresses.watchedRecord]!!, watchedEpisodes, watchedEpisodes + 1, now)
                if(toCompleted) it.finishTime to now
            }
            null
        }

        database.update(Records) {
            where { it.id eq recordId }
            it.watchedEpisodes to watchedEpisodes + 1
            //这属于活跃行为，因此更新last active
            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(if(toCompleted) { ActiveEventType.WATCH_COMPLETE }else{ ActiveEventType.WATCH_EPISODE }, listOf(watchedEpisodes + 1))
            if(newProgressId != null) {
                //新建进度
                it.latestProgressId to newProgressId
                it.progressCount to 1
            }
            if(toCompleted) {
                if(newProgressId != null || progressCount == 1) {
                    //新建进度且完成，那么直接标记finish time
                    //或使用现有进度，完成，且进度是第一个，那么标记record的finish time
                    it.finishTime to now
                }
                it.status to RecordStatus.COMPLETED
                it.inDiary to false
            }else if(newProgressId != null) {
                it.status to RecordStatus.WATCHING
            }
            it.updateTime to now
        }

        return NextRes(watchedEpisodes + 1)
    }

    override fun getProgressList(animationId: Int, user: User): List<ProgressRes> {
        val rowSet = database.from(Records).select(Records.id)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record not found.")
        val recordId = rowSet[Records.id]!!

        return database.from(RecordProgresses).select(RecordProgresses.ordinal, RecordProgresses.startTime, RecordProgresses.finishTime)
                .where { RecordProgresses.recordId eq recordId }
                .orderBy(RecordProgresses.ordinal.asc())
                .map {
                    ProgressRes(
                            it[RecordProgresses.ordinal]!!,
                            it[RecordProgresses.startTime]?.toDateTimeString(),
                            it[RecordProgresses.finishTime]?.toDateTimeString()
                    )
                }
    }

    @Transactional
    override fun createProgress(animationId: Int, form: ProgressCreateForm, user: User): ProgressRes {
        TODO("Not yet implemented")
    }

    @Transactional
    override fun deleteProgress(animationId: Int, ordinal: Int, user: User) {
        TODO("Not yet implemented")
    }
}
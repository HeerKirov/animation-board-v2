package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.InternalException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.ProgressCreateForm
import com.heerkirov.animation.model.form.ProgressUpdateForm
import com.heerkirov.animation.model.result.NextRes
import com.heerkirov.animation.model.result.ProgressRes
import com.heerkirov.animation.service.RecordProgressService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.arrayListFor
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
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
                .select(Records.id, Records.progressCount,
                        Animations.totalEpisodes, Animations.publishedEpisodes)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")

        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!

        if(publishedEpisodes == 0) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "No next episode.")
        }

        val recordId = rowSet[Records.id]!!
        val progressCount = rowSet[Records.progressCount]!!
        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val now = DateTimeUtil.now()

        if(progressCount == 0) {
            //没有进度，那么创建一个
            database.insert(RecordProgresses) {
                it.recordId to recordId
                it.ordinal to 1
                it.watchedEpisodes to 1
                it.watchedRecord to listOf(now)
                it.startTime to now
                it.finishTime to if(1 >= totalEpisodes) now else null
            }
            database.update(Records) {
                where { it.id eq recordId }
                it.progressCount to 1
                it.lastActiveTime to now
                it.lastActiveEvent to ActiveEvent(ActiveEventType.WATCH_EPISODE, listOf(1))
                it.updateTime to now
                if(1 >= totalEpisodes) it.inDiary to false
            }
            return NextRes(1)
        }else{
            //有进度，那么查找此进度
            val progress = database.sequenceOf(RecordProgresses)
                    .find { (it.recordId eq recordId) and (it.ordinal eq progressCount) }
                    ?: throw InternalException("Cannot find progress $progressCount of Record $recordId.")
            val watchedEpisodes = progress.watchedEpisodes
            if(watchedEpisodes >= publishedEpisodes) {
                throw BadRequestException(ErrCode.INVALID_OPERATION, "No next episode.")
            }
            database.update(RecordProgresses) {
                where { it.id eq progress.id }
                it.watchedEpisodes to watchedEpisodes + 1
                it.watchedRecord to recordProcessor.calculateProgressWatchedRecord(progress.watchedRecord, watchedEpisodes, watchedEpisodes + 1, now)
                if(watchedEpisodes + 1 >= totalEpisodes) it.finishTime to now
            }
            database.update(Records) {
                where { it.id eq recordId }
                it.lastActiveTime to now
                it.lastActiveEvent to ActiveEvent(ActiveEventType.WATCH_EPISODE, listOf(watchedEpisodes + 1))
                it.updateTime to now
                if(watchedEpisodes + 1 >= totalEpisodes) it.inDiary to false
            }
            return NextRes(watchedEpisodes + 1)
        }
    }

    override fun getProgressList(animationId: Int, user: User): List<ProgressRes> {
        val rowSet = database.from(Records).select(Records.id)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record not found.")
        val recordId = rowSet[Records.id]!!

        return database.from(RecordProgresses)
                .select(RecordProgresses.ordinal, RecordProgresses.watchedEpisodes, RecordProgresses.startTime, RecordProgresses.finishTime)
                .where { RecordProgresses.recordId eq recordId }
                .orderBy(RecordProgresses.ordinal.asc())
                .map {
                    ProgressRes(
                            it[RecordProgresses.ordinal]!!,
                            it[RecordProgresses.watchedEpisodes]!!,
                            it[RecordProgresses.startTime]?.toDateTimeString(),
                            it[RecordProgresses.finishTime]?.toDateTimeString()
                    )
                }
    }

    @Transactional
    override fun createProgress(animationId: Int, form: ProgressCreateForm, user: User): ProgressRes {
        val rowSet = database.from(Records).select(Records.id, Records.progressCount)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")

        val recordId = rowSet[Records.id]!!
        val progressCount = rowSet[Records.progressCount]!!
        val latestProgress = if(progressCount == 0) null else database.sequenceOf(RecordProgresses)
                .find { (RecordProgresses.recordId eq recordId) and (RecordProgresses.ordinal eq progressCount) }

        if(latestProgress != null && latestProgress.finishTime == null) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "Cannot create new progress because latest progress is not completed.")
        }

        val now = DateTimeUtil.now()

        if(form.supplement) {
            //补充模式创建
            if(form.finishTime == null && form.watchedEpisodes == null) {
                throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'finish_time' or 'watched_episodes' is required.")
            }
            if(form.startTime != null && form.finishTime != null && form.startTime > form.finishTime) {
                throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'start_time' must be greater than finish_time.")
            }
            if(latestProgress != null) {
                //在存在前一个进度时，需要对时间先后进行校验
                val newTime = form.startTime ?: form.finishTime
                if(newTime != null && latestProgress.finishTime!! > newTime) {
                    throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'start_time'/'finish_time' must be greater than previous finish_time[${latestProgress.finishTime.toDateTimeString()}].")
                }
            }

            val publishedEpisodes = database.from(Animations).select(Animations.publishedEpisodes)
                    .where { Animations.id eq animationId }
                    .first()[Animations.publishedEpisodes]!!

            val watchedEpisodes = if(form.finishTime != null || form.watchedEpisodes!! > publishedEpisodes) publishedEpisodes else form.watchedEpisodes

            database.insert(RecordProgresses) {
                it.recordId to recordId
                it.ordinal to (progressCount + 1)
                it.watchedEpisodes to watchedEpisodes
                it.watchedRecord to arrayListFor(watchedEpisodes) { null }
                it.startTime to form.startTime
                it.finishTime to form.finishTime
            }

            database.update(Records) {
                where { it.id eq recordId }
                it.progressCount to progressCount + 1
                it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_PROGRESS)
                it.lastActiveTime to now
                it.updateTime to now
            }

            return ProgressRes(progressCount + 1, watchedEpisodes, form.startTime?.toDateTimeString(), form.finishTime?.toDateTimeString())
        }else{
            //普通新增
            database.insert(RecordProgresses) {
                it.recordId to recordId
                it.ordinal to progressCount + 1
                it.watchedEpisodes to 0
                it.watchedRecord to emptyList()
                it.startTime to now
                it.finishTime to null
            }

            database.update(Records) {
                where { it.id eq recordId }
                it.progressCount to (progressCount + 1)
                it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_PROGRESS)
                it.lastActiveTime to now
                it.updateTime to now
            }

            return ProgressRes(progressCount + 1, 0, now.toDateTimeString(), null)
        }
    }

    @Transactional
    override fun updateLatestProgress(animationId: Int, form: ProgressUpdateForm, user: User): ProgressRes {
        val rowSet = database.from(Records)
                .innerJoin(Animations, Animations.id eq Records.animationId)
                .select(Records.id, Records.progressCount,
                        Animations.totalEpisodes, Animations.publishedEpisodes)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")


        val recordId = rowSet[Records.id]!!
        val progressCount = rowSet[Records.progressCount]!!
        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
        val now = DateTimeUtil.now()

        if(progressCount == 0) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "Record of animation $animationId has no progress. Please create one first.")
        }

        val progress = database.sequenceOf(RecordProgresses)
                .find { (RecordProgresses.recordId eq recordId) and (RecordProgresses.ordinal eq progressCount) }
                ?: throw InternalException("Cannot find progress $progressCount of Record $recordId.")

        val newWatchedEpisodes = if(form.watchedEpisodes > publishedEpisodes) publishedEpisodes else form.watchedEpisodes

        if(newWatchedEpisodes != progress.watchedEpisodes) {
            database.update(RecordProgresses) {
                where { it.id eq progress.id }
                it.watchedEpisodes to newWatchedEpisodes
                it.watchedRecord to recordProcessor.calculateProgressWatchedRecord(progress.watchedRecord, progress.watchedEpisodes, newWatchedEpisodes, now)
                if(newWatchedEpisodes >= totalEpisodes) it.finishTime to now
                else if(progress.finishTime != null) it.finishTime to null
            }

            database.update(Records) {
                where { it.id eq recordId }
                if(newWatchedEpisodes >= totalEpisodes) it.inDiary to false
                if(newWatchedEpisodes > progress.watchedEpisodes) {
                    it.lastActiveEvent to ActiveEvent(ActiveEventType.WATCH_EPISODE, IntRange(progress.watchedEpisodes + 1, newWatchedEpisodes).toList())
                    it.lastActiveTime to now
                }
                it.updateTime to now
            }

            return ProgressRes(progress.ordinal,
                    newWatchedEpisodes,
                    progress.startTime?.toDateTimeString(),
                    if(newWatchedEpisodes >= totalEpisodes) now.toDateTimeString() else progress.finishTime?.toDateTimeString())
        }else{
            return ProgressRes(progress.ordinal,
                    progress.watchedEpisodes,
                    progress.startTime?.toDateTimeString(),
                    progress.finishTime?.toDateTimeString())
        }

    }

    @Transactional
    override fun deleteProgress(animationId: Int, ordinal: Int, user: User) {
        val rowSet = database.from(Records).select(Records.id, Records.progressCount)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")

        val recordId = rowSet[Records.id]!!
        val progressCount = rowSet[Records.progressCount]!!
        val now = DateTimeUtil.now()

        if(ordinal < 1 || ordinal > progressCount) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "Ordinal $ordinal is not exists.")
        }

        database.update(Records) {
            where { it.id eq recordId }
            it.progressCount to (progressCount - 1)
            it.updateTime to now
        }
        database.delete(RecordProgresses) { (it.recordId eq recordId) and (it.ordinal eq ordinal) }
        database.batchUpdate(RecordProgresses) {
            for(i in (ordinal + 1)..progressCount) {
                item {
                    where { (it.recordId eq recordId) and (it.ordinal eq i) }
                    it.ordinal to (i - 1)
                }
            }
        }

    }
}
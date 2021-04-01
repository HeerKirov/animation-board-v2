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
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
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
                set(it.recordId, recordId)
                set(it.ordinal, 1)
                set(it.watchedEpisodes, 1)
                set(it.watchedRecord, listOf(now))
                set(it.startTime, now)
                set(it.finishTime, if(1 >= totalEpisodes) now else null)
            }
            database.update(Records) {
                where { it.id eq recordId }
                set(it.progressCount, 1)
                set(it.lastActiveTime, now)
                set(it.lastActiveEvent, ActiveEvent(ActiveEventType.WATCH_EPISODE, listOf(1)))
                set(it.updateTime, now)
                if(1 >= totalEpisodes) set(it.inDiary, false)
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
                set(it.watchedEpisodes, watchedEpisodes + 1)
                set(it.watchedRecord, recordProcessor.calculateProgressWatchedRecord(progress.watchedRecord, watchedEpisodes, watchedEpisodes + 1, now))
                if(watchedEpisodes + 1 >= totalEpisodes) set(it.finishTime, now)
            }
            database.update(Records) {
                where { it.id eq recordId }
                set(it.lastActiveTime, now)
                set(it.lastActiveEvent, ActiveEvent(ActiveEventType.WATCH_EPISODE, listOf(watchedEpisodes + 1)))
                set(it.updateTime, now)
                if(watchedEpisodes + 1 >= totalEpisodes) set(it.inDiary, false)
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

        val now = DateTimeUtil.now()

        if(form.supplement) {
            //补充模式创建
            if(form.finishTime == null && form.watchedEpisodes == null) {
                throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'finish_time' or 'watched_episodes' is required.")
            }
            if(form.startTime != null && form.finishTime != null && form.startTime > form.finishTime) {
                throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'start_time' must be greater than finish_time.")
            }

            val progresses = database.sequenceOf(RecordProgresses)
                    .filter { (RecordProgresses.recordId eq recordId) }
                    .sortedBy { it.ordinal }
                    .toList()

            val ordinal = when {
                //不存在已有进度，那么新进度可以以任意方式插入
                progresses.isEmpty() -> 1
                //存在已有进度，且指定了新进度的完成时间，需要比对确定新进度的插入位置
                form.finishTime != null -> progresses.asSequence().map { progress ->
                    when {
                        //遇到了未完成的记录，那么新记录将插入在未完成记录的前面，也就是使用此未完成记录的ordinal
                        //遇到了更大的时间点记录，也就是排在此记录的前面
                        progress.finishTime == null || form.finishTime < progress.finishTime -> progress.ordinal
                        //遇到了具有相等时间点的记录，按照规则排在此记录的后面一位
                        form.finishTime == progress.finishTime -> progress.ordinal + 1
                        else -> null
                    }
                }.filterNotNull().firstOrNull() ?: (progresses.size + 1)
                //存在已有进度，未指定新进度的完成时间，不存在已完成的进度，直接追加到末尾
                progresses.last().finishTime != null -> progresses.size + 1
                //存在已有进度，未指定新进度的完成时间，且存在未完成的进度时，提示无法插入
                else -> throw BadRequestException(ErrCode.INVALID_OPERATION, "Cannot create new progress because latest progress is not completed.")
            }

            val needUpdate = progresses.filter { it.ordinal >= ordinal }
            if(needUpdate.isNotEmpty()) {
                //如果存在大于等于新ordinal的进度，将这些进度的ordinal向后延1
                database.batchUpdate(RecordProgresses) {
                    for (progress in needUpdate) {
                        item {
                            where { it.id eq progress.id }
                            set(it.ordinal, progress.ordinal + 1)
                        }
                    }
                }
            }

            val publishedEpisodes = database.from(Animations).select(Animations.publishedEpisodes)
                    .where { Animations.id eq animationId }
                    .first()[Animations.publishedEpisodes]!!

            val watchedEpisodes = if(form.finishTime != null || form.watchedEpisodes!! > publishedEpisodes) publishedEpisodes else form.watchedEpisodes

            database.insert(RecordProgresses) {
                set(it.recordId, recordId)
                set(it.ordinal, ordinal)
                set(it.watchedEpisodes, watchedEpisodes)
                set(it.watchedRecord, arrayListFor(watchedEpisodes) { null })
                set(it.startTime, form.startTime)
                set(it.finishTime, form.finishTime)
            }

            database.update(Records) {
                where { it.id eq recordId }
                set(it.progressCount, progressCount + 1)
                set(it.lastActiveEvent, ActiveEvent(ActiveEventType.CREATE_PROGRESS))
                set(it.lastActiveTime, now)
                set(it.updateTime, now)
            }

            return ProgressRes(ordinal, watchedEpisodes, form.startTime?.toDateTimeString(), form.finishTime?.toDateTimeString())
        }else{
            val latestProgress = if(progressCount == 0) null else database.sequenceOf(RecordProgresses)
                    .find { (RecordProgresses.recordId eq recordId) and (RecordProgresses.ordinal eq progressCount) }
            if(latestProgress != null && latestProgress.finishTime == null) {
                throw BadRequestException(ErrCode.INVALID_OPERATION, "Cannot create new progress because latest progress is not completed.")
            }

            //普通新增
            database.insert(RecordProgresses) {
                set(it.recordId, recordId)
                set(it.ordinal, progressCount + 1)
                set(it.watchedEpisodes, 0)
                set(it.watchedRecord, emptyList())
                set(it.startTime, now)
                set(it.finishTime, null)
            }

            database.update(Records) {
                where { it.id eq recordId }
                set(it.progressCount, progressCount + 1)
                set(it.lastActiveEvent, ActiveEvent(ActiveEventType.CREATE_PROGRESS))
                set(it.lastActiveTime, now)
                set(it.updateTime, now)
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
                set(it.watchedEpisodes, newWatchedEpisodes)
                set(it.watchedRecord, recordProcessor.calculateProgressWatchedRecord(progress.watchedRecord, progress.watchedEpisodes, newWatchedEpisodes, now))
                if(newWatchedEpisodes >= totalEpisodes) set(it.finishTime, now)
                else if(progress.finishTime != null) set(it.finishTime, null)
            }

            database.update(Records) {
                where { it.id eq recordId }
                if(newWatchedEpisodes >= totalEpisodes) set(it.inDiary, false)
                if(newWatchedEpisodes > progress.watchedEpisodes) {
                    set(it.lastActiveEvent, ActiveEvent(ActiveEventType.WATCH_EPISODE, IntRange(progress.watchedEpisodes + 1, newWatchedEpisodes).toList()))
                    set(it.lastActiveTime, now)
                }
                set(it.updateTime, now)
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
            set(it.progressCount, progressCount - 1)
            set(it.updateTime, now)
        }
        database.delete(RecordProgresses) { (it.recordId eq recordId) and (it.ordinal eq ordinal) }
        database.batchUpdate(RecordProgresses) {
            for(i in (ordinal + 1)..progressCount) {
                item {
                    where { (it.recordId eq recordId) and (it.ordinal eq i) }
                    set(it.ordinal, i - 1)
                }
            }
        }

    }
}
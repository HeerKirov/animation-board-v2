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
import com.heerkirov.animation.model.form.RecordCreateForm
import com.heerkirov.animation.model.form.RecordPartialForm
import com.heerkirov.animation.model.result.RecordDetailRes
import com.heerkirov.animation.service.RecordSetterService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecordSetterServiceImpl(@Autowired private val database: Database,
                              @Autowired private val recordProcessor: RecordProcessor) : RecordSetterService {

    @Transactional
    override fun create(form: RecordCreateForm, user: User) {
        database.sequenceOf(Records).find { (Records.animationId eq form.animationId) and (Records.ownerId eq user.id) }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Record of animation ${form.animationId} is already exists.")
        }
        if(database.sequenceOf(Animations).find { Animations.id eq form.animationId } == null) {
            throw BadRequestException(ErrCode.NOT_EXISTS, "Animation ${form.animationId} is not exists.")
        }

        //创建进度模型并回存到记录表
        when (form.createType) {
            RecordCreateForm.CreateType.SUBSCRIBE -> recordProcessor.createSubscribe(form, user)
            RecordCreateForm.CreateType.SUPPLEMENT -> recordProcessor.createSupplement(form, user)
            RecordCreateForm.CreateType.RECORD -> recordProcessor.createRecord(form, user)
        }
    }

    @Transactional
    override fun partialUpdate(animationId: Int, form: RecordPartialForm, user: User) {
        val record = database.sequenceOf(Records).find { (it.animationId eq animationId) and (it.ownerId eq user.id) }
                ?: throw NotFoundException("Record of animation $animationId not found.")

        //计算watched episodes。当不为null时表示需要更新此值
        val (totalEpisodes, watchedEpisodes) = if(form.watchedEpisodes == null || form.watchedEpisodes == record.watchedEpisodes) { Pair(null, null) }else{
            if(record.latestProgressId == null) {
                throw BadRequestException(ErrCode.INVALID_OPERATION, "Record of animation $animationId has no progress. Please create one first.")
            }
            val (totalEpisodes, publishedEpisodes) = database.from(Animations).select(Animations.totalEpisodes, Animations.publishedEpisodes)
                    .where { Animations.id eq animationId }.first()
                    .let { row -> Pair(row[Animations.totalEpisodes]!!, row[Animations.publishedEpisodes]!!) }
            val watchedEpisodes = if(form.watchedEpisodes > publishedEpisodes) { publishedEpisodes }else{ form.watchedEpisodes }
            Pair(totalEpisodes, watchedEpisodes)
        }
        //当需要更新watched时，计算status的更新目标
        val status = when {
            watchedEpisodes == null -> null
            watchedEpisodes >= totalEpisodes!! -> RecordStatus.COMPLETED
            record.progressCount > 1 -> RecordStatus.REWATCHING
            else -> RecordStatus.WATCHING
        }?.let { if(it == record.status) { null }else{ it } }
        //状态被更新为completed，说明此record的状态发生了完成时切换
        val toCompleted = status == RecordStatus.COMPLETED
        //取用form的值，没有值，且完成时，会自动切换为false
        val inDiary = when {
            form.inDiary != null -> form.inDiary
            toCompleted -> false
            else -> null
        }

        val now = DateTimeUtil.now()

        database.update(Records) {
            where { it.id eq record.id }
            if(form.seenOriginal != null) it.seenOriginal to form.seenOriginal
            if(inDiary != null) it.inDiary to inDiary
            if(watchedEpisodes != null) {
                //更新watched episodes
                it.watchedEpisodes to watchedEpisodes
                //不为null说明status也发生了变化
                if(status != null) it.status to status
                //更新为已完成，且当前进度为1时，设置finish time
                if(toCompleted && record.progressCount == 1) it.finishTime to now
                //这属于活跃行为，因此更新last active
                it.lastActiveTime to now
                it.lastActiveEvent to ActiveEvent(if(toCompleted) { ActiveEventType.WATCH_COMPLETE }else{ ActiveEventType.WATCH_EPISODE }, listOf(watchedEpisodes))
            }

            it.updateTime to now
        }

        if(watchedEpisodes != null) {
            val progress = database.sequenceOf(RecordProgresses).find { it.id eq record.latestProgressId!! }!!
            database.update(RecordProgresses) {
                where { it.id eq progress.id }
                //当更新为已完成时，设置finish time
                if(toCompleted) it.finishTime to now
                //计算时间点记录表
                it.watchedRecord to recordProcessor.calculateProgressWatchedRecord(progress.watchedRecord, record.watchedEpisodes, watchedEpisodes, now)
            }
        }
    }

    @Transactional
    override fun delete(animationId: Int, user: User) {
        val id = database.from(Records).select(Records.id)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull()?.get(Records.id) ?: throw NotFoundException("Record of animation $animationId not found.")
        database.delete(Records) { it.id eq id }
        database.delete(RecordProgresses) { it.recordId eq id }
    }
}
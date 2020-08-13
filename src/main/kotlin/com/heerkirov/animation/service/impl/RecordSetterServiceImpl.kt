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
class RecordSetterServiceImpl(@Autowired private val database: Database) : RecordSetterService {

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
            RecordCreateForm.CreateType.SUBSCRIBE -> createSubscribe(form, user)
            RecordCreateForm.CreateType.SUPPLEMENT -> createSupplement(form, user)
            RecordCreateForm.CreateType.RECORD -> createRecord(form, user)
        }
    }

    @Transactional
    override fun partialUpdate(animationId: Int, form: RecordPartialForm, user: User) {
        val record = database.sequenceOf(Records).find { (it.animationId eq animationId) and (it.ownerId eq user.id) }
                ?: throw NotFoundException("Record of animation $animationId not found.")

        val now = DateTimeUtil.now()

        database.update(Records) {
            where { it.id eq record.id }
            if(form.seenOriginal != null) it.seenOriginal to form.seenOriginal
            if(form.inDiary != null) it.inDiary to form.inDiary
            it.updateTime to now
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

    private fun createSubscribe(form: RecordCreateForm, user: User) {
        val now = DateTimeUtil.now()
        //订阅模式创建。创建默认的第一条观看进度
        val id = database.insertAndGenerateKey(Records) {
            it.ownerId to user.id
            it.animationId to form.animationId
            it.seenOriginal to false
            it.inDiary to true

            it.progressCount to 1
            it.scatterRecord to emptyList()

            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_RECORD)
            it.createTime to now
            it.updateTime to now
        } as Long

        database.insert(RecordProgresses) {
            it.recordId to id
            it.ordinal to 1
            it.watchedEpisodes to 0
            it.watchedRecord to emptyList()
            it.startTime to now
            it.finishTime to null
        }
    }

    private fun createSupplement(form: RecordCreateForm, user: User) {
        if(form.progress == null) {
            throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'progress' is required.")
        }
        if(form.progress.isEmpty()) {
            throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'progress' cannot be empty.")
        }

        val animationRowSet = database.from(Animations).select(Animations.totalEpisodes, Animations.publishedEpisodes)
                .where { Animations.id eq form.animationId }
                .limit(0, 1).first()
        val totalEpisodes = animationRowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = animationRowSet[Animations.publishedEpisodes]!!

        if(publishedEpisodes >= totalEpisodes) {
            //完结动画允许创建多个进度。
            //只有最后的进度允许未完成，前面的进度都必须已完成。
            //后面的进度必须不严格晚于前面的进度(即start/finish分别晚于前面)
            //指定finish优先级高于watched episodes。
            for(i in form.progress.indices) {
                val progressForm = form.progress[i]
                //进度分前进度和最后一条进度
                if(i < form.progress.size - 1) {
                    //前进度必须是完成状态
                    if(progressForm.finishTime == null) {
                        throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'progress': finish_time is required for previous item.")
                    }
                    //start必须小于finish
                    if(progressForm.startTime != null && progressForm.startTime > progressForm.finishTime) {
                        throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'progress': start_time cannot be greater than finish_time.")
                    }
                    //与后一条进度比较，要求前一条的时间段严格小于后一条
                    val nextForm = form.progress[i + 1]
                    val nextFormTime = nextForm.startTime ?: nextForm.finishTime
                    if(nextFormTime != null && progressForm.finishTime > nextFormTime) {
                        throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'progress': finish_time cannot be greater than next start_time/finish_time.")
                    }
                }else{
                    //start必须小于finish
                    if(progressForm.startTime != null && progressForm.finishTime != null && progressForm.startTime > progressForm.finishTime) {
                        throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'progress': start_time cannot be greater than finish_time.")
                    }
                    //finish和watched episodes必须提供其一，且优先finish time
                    if(progressForm.finishTime == null && progressForm.watchedEpisodes == null) {
                        throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'finish_time' or 'watched_episodes' is required.")
                    }
                }
            }
        }else{
            //未完结的动画采取不同的进度策略。
            //只允许创建一个进度，并且其策略与多进度的最后一个进度相同。
            if(form.progress.size > 1) {
                throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'progress': can only create 1 progress for unfinished animation.")
            }
            val last = form.progress.last()
            if(last.finishTime != null) {
                throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'progress': cannot set finish_time for unfinished animation.")
            }else if(last.watchedEpisodes == null) {
                throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'progress': watched_episodes is required.")
            }
        }

        val now = DateTimeUtil.now()

        //最后一条进度的watchedEpisodes用于推导record的watchedEpisodes
        val watchedEpisodes = if(form.progress.last().finishTime != null) {
            publishedEpisodes
        } else form.progress.last().watchedEpisodes.let {
            if(it == null || it > publishedEpisodes) {
                publishedEpisodes
            }else{
                it
            }
        }

        //补充模式创建。按照表单提供的记录创建多个进度
        val id = database.insertAndGenerateKey(Records) {
            it.ownerId to user.id
            it.animationId to form.animationId
            it.seenOriginal to false
            it.inDiary to (watchedEpisodes < totalEpisodes)    //当状态为完结时不放入日记

            it.progressCount to form.progress.size
            it.scatterRecord to emptyList()

            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_RECORD)
            it.createTime to now
            it.updateTime to now
        } as Long

        for(i in form.progress.indices) {
            val progressForm = form.progress[i]
            if(i < form.progress.size - 1) {
                database.insert(RecordProgresses) {
                    it.recordId to id
                    it.ordinal to (i + 1)
                    it.watchedEpisodes to totalEpisodes
                    it.watchedRecord to emptyList() //已完结的记录不需要再创建这个，不会再更新了
                    it.startTime to progressForm.startTime
                    it.finishTime to progressForm.finishTime
                }
            }else{
                database.insert(RecordProgresses) {
                    it.recordId to id
                    it.ordinal to (i + 1)
                    it.watchedEpisodes to watchedEpisodes
                    it.watchedRecord to emptyList() //可以写但没有必要。真需要更新时会检查时间点的数目的
                    it.startTime to progressForm.startTime
                    it.finishTime to (progressForm.finishTime ?: if(watchedEpisodes >= totalEpisodes) { now }else{ null }) //finishTime没写而实际已看完时，自动补全finishTime
                }
            }
        }
    }

    private fun createRecord(form: RecordCreateForm, user: User) {
        val now = DateTimeUtil.now()
        //记录模式创建。以无进度模式创建，所有记录都为0
        database.insert(Records) {
            it.ownerId to user.id
            it.animationId to form.animationId
            it.seenOriginal to false
            it.inDiary to false

            it.progressCount to 0
            it.scatterRecord to emptyList()

            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_RECORD)
            it.createTime to now
            it.updateTime to now
        }
    }
}
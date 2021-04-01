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
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
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
            if(form.seenOriginal != null) set(it.seenOriginal, form.seenOriginal)
            if(form.inDiary != null) set(it.inDiary, form.inDiary)
            set(it.updateTime, now)
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
            set(it.ownerId, user.id)
            set(it.animationId, form.animationId)
            set(it.seenOriginal, false)
            set(it.inDiary, true)

            set(it.progressCount, 1)
            set(it.scatterRecord, emptyList())

            set(it.lastActiveTime, now)
            set(it.lastActiveEvent, ActiveEvent(ActiveEventType.CREATE_RECORD))
            set(it.createTime, now)
            set(it.updateTime, now)
        } as Long

        database.insert(RecordProgresses) {
            set(it.recordId, id)
            set(it.ordinal, 1)
            set(it.watchedEpisodes, 0)
            set(it.watchedRecord, emptyList())
            set(it.startTime, now)
            set(it.finishTime, null)
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
            set(it.ownerId, user.id)
            set(it.animationId, form.animationId)
            set(it.seenOriginal, false)
            set(it.inDiary, watchedEpisodes < totalEpisodes)    //当状态为完结时不放入日记

            set(it.progressCount, form.progress.size)
            set(it.scatterRecord, emptyList())

            set(it.lastActiveTime, now)
            set(it.lastActiveEvent, ActiveEvent(ActiveEventType.CREATE_RECORD))
            set(it.createTime, now)
            set(it.updateTime, now)
        } as Long

        for(i in form.progress.indices) {
            val progressForm = form.progress[i]
            if(i < form.progress.size - 1) {
                database.insert(RecordProgresses) {
                    set(it.recordId, id)
                    set(it.ordinal, i + 1)
                    set(it.watchedEpisodes, totalEpisodes)
                    set(it.watchedRecord, emptyList()) //已完结的记录不需要再创建这个，不会再更新了
                    set(it.startTime, progressForm.startTime)
                    set(it.finishTime, progressForm.finishTime)
                }
            }else{
                database.insert(RecordProgresses) {
                    set(it.recordId, id)
                    set(it.ordinal, i + 1)
                    set(it.watchedEpisodes, watchedEpisodes)
                    set(it.watchedRecord, emptyList()) //可以写但没有必要。真需要更新时会检查时间点的数目的
                    set(it.startTime, progressForm.startTime)
                    set(it.finishTime, progressForm.finishTime ?: if(watchedEpisodes >= totalEpisodes) { now }else{ null }) //finishTime没写而实际已看完时，自动补全finishTime
                }
            }
        }
    }

    private fun createRecord(form: RecordCreateForm, user: User) {
        val now = DateTimeUtil.now()
        //记录模式创建。以无进度模式创建，所有记录都为0
        database.insert(Records) {
            set(it.ownerId, user.id)
            set(it.animationId, form.animationId)
            set(it.seenOriginal, false)
            set(it.inDiary, false)

            set(it.progressCount, 0)
            set(it.scatterRecord, emptyList())

            set(it.lastActiveTime, now)
            set(it.lastActiveEvent, ActiveEvent(ActiveEventType.CREATE_RECORD))
            set(it.createTime, now)
            set(it.updateTime, now)
        }
    }
}
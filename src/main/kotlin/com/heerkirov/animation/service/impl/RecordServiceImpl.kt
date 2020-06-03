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
import com.heerkirov.animation.model.data.WatchedRecord
import com.heerkirov.animation.model.form.ProgressForm
import com.heerkirov.animation.model.form.RecordCreateForm
import com.heerkirov.animation.model.form.RecordPartialForm
import com.heerkirov.animation.model.result.ProgressRes
import com.heerkirov.animation.model.result.RecordDetailRes
import com.heerkirov.animation.service.RecordService
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
class RecordServiceImpl(@Autowired private val database: Database) : RecordService {
    private val recordFields = arrayOf(
            Animations.title, Records.seenOriginal, Records.status, Records.inDiary, Records.watchedRecord,
            Animations.totalEpisodes, Animations.publishedEpisodes, Records.watchedEpisodes, Records.progressCount,
            Records.subscriptionTime, Records.finishTime, Records.createTime, Records.updateTime
    )

    override fun get(animationId: Int, user: User): RecordDetailRes {
        val rowSet = database.from(Records)
                .innerJoin(Animations, Records.animationId eq Animations.id)
                .select(*recordFields)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull()
                ?: throw NotFoundException("Record not found.")

        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
        val watchedEpisodes = rowSet[Records.watchedEpisodes]!!
        val watchedRecord = rowSet[Records.watchedRecord]!!
        val progressCount = rowSet[Records.progressCount]!!
        val episodesCount = calculateEpisodesCount(watchedRecord, progressCount, watchedEpisodes, publishedEpisodes)

        return RecordDetailRes(
                animationId = animationId,
                title = rowSet[Animations.title]!!,
                seenOriginal = rowSet[Records.seenOriginal]!!,
                status = rowSet[Records.status]!!,
                inDiary = rowSet[Records.inDiary]!!,
                totalEpisodes = totalEpisodes,
                publishedEpisodes = publishedEpisodes,
                watchedEpisodes = watchedEpisodes,
                progressCount = progressCount,
                episodesCount = episodesCount,
                subscriptionTime = rowSet[Records.subscriptionTime]?.toDateTimeString(),
                finishTime = rowSet[Records.finishTime]?.toDateTimeString(),
                createTime = rowSet[Records.createTime]!!.toDateTimeString(),
                updateTime = rowSet[Records.updateTime]!!.toDateTimeString()
        )
    }

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
        TODO("Not yet implemented")
    }

    @Transactional
    override fun delete(animationId: Int, user: User) {
        TODO("Not yet implemented")
    }

    override fun getProgressList(animationId: Int, user: User): List<ProgressRes> {
        TODO("Not yet implemented")
    }

    @Transactional
    override fun createProgress(animationId: Int, form: ProgressForm, user: User): ProgressRes {
        TODO("Not yet implemented")
    }

    private fun createSubscribe(form: RecordCreateForm, user: User) {
        val now = DateTimeUtil.now()
        //订阅模式创建。创建默认的第一条观看进度
        val id = database.insertAndGenerateKey(Records) {
            it.ownerId to user.id
            it.animationId to form.animationId
            it.seenOriginal to false

            it.status to RecordStatus.WATCHING
            it.inDiary to true
            it.watchedEpisodes to 0
            it.progressCount to 1
            it.latestProgressId to null
            it.watchedRecord to emptyList()
            it.subscriptionTime to now
            it.finishTime to null

            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_RECORD)
            it.createTime to now
            it.updateTime to now
        } as Long

        val progressId = database.insertAndGenerateKey(RecordProgresses) {
            it.recordId to id
            it.ordinal to 1
            it.watchedRecord to emptyList()
            it.startTime to now
            it.finishTime to null
        } as Long

        database.update(Records) {
            it.latestProgressId to progressId
            where { Records.id eq id }
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
        val watchedEpisodes = form.progress.last().watchedEpisodes.let {
            if(it == null || it > publishedEpisodes) {
                publishedEpisodes
            }else{
                it
            }
        }
        //最后进度的watched不小于total时判定为已完成，否则判定为在看或重看
        val status = when {
            watchedEpisodes >= totalEpisodes -> RecordStatus.COMPLETED
            form.progress.size == 1 -> RecordStatus.WATCHING
            else -> RecordStatus.REWATCHING
        }
        //record的订阅时间和完成时间都是首个进度的对应时间
        val subscriptionTime = form.progress.first().startTime
        val finishTime = form.progress.first().finishTime

        //补充模式创建。按照表单提供的记录创建多个进度
        val id = database.insertAndGenerateKey(Records) {
            it.ownerId to user.id
            it.animationId to form.animationId
            it.seenOriginal to false

            it.status to status
            it.inDiary to (status != RecordStatus.COMPLETED)    //当状态为完结时不放入日记
            it.watchedEpisodes to watchedEpisodes
            it.progressCount to form.progress.size
            it.latestProgressId to null
            it.watchedRecord to emptyList()
            it.subscriptionTime to subscriptionTime
            it.finishTime to finishTime

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
                    it.watchedRecord to emptyList() //已完结的记录不需要再创建这个，不会再更新了
                    it.startTime to progressForm.startTime
                    it.finishTime to progressForm.finishTime
                }
            }else{
                val progressId = database.insertAndGenerateKey(RecordProgresses) {
                    it.recordId to id
                    it.ordinal to (i + 1)
                    it.watchedRecord to emptyList() //可以写但没有必要。真需要更新时会检查时间点的数目的
                    it.startTime to progressForm.startTime
                    it.finishTime to (progressForm.finishTime ?: if(status == RecordStatus.COMPLETED) { now }else{ null }) //finishTime没写而实际已看完时，自动补全finishTime
                } as Long

                database.update(Records) {
                    it.latestProgressId to progressId
                    where { it.id eq id }
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

            it.status to RecordStatus.NO_PROGRESS
            it.inDiary to false
            it.watchedEpisodes to 0
            it.progressCount to 0
            it.latestProgressId to null
            it.watchedRecord to emptyList()
            it.subscriptionTime to null
            it.finishTime to null

            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(ActiveEventType.CREATE_RECORD)
            it.createTime to now
            it.updateTime to now
        }
    }

    private fun calculateEpisodesCount(watchedRecord: List<WatchedRecord>, progressCount: Int, watchedEpisodes: Int, publishedEpisodes: Int): List<Int> {
        val size = if (progressCount > 1) publishedEpisodes else watchedEpisodes
        val episodes = arrayListFor(size) { if(progressCount == 0 || it < watchedEpisodes) { progressCount }else{ progressCount - 1 } }
        for ((episode, _) in watchedRecord) {
            if(episode in 1..size) {
                episodes[episode - 1] += 1
            }
        }
        return episodes
    }
}
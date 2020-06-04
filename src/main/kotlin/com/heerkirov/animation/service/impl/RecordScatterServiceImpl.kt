package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.data.ScatterRecord
import com.heerkirov.animation.model.result.ScatterGroupRes
import com.heerkirov.animation.model.result.ScatterItemRes
import com.heerkirov.animation.service.RecordScatterService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.parseDateTime
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

@Service
class RecordScatterServiceImpl(@Autowired private val database: Database,
                               @Autowired private val recordProcessor: RecordProcessor) : RecordScatterService {
    override fun getScatterTable(animationId: Int, user: User): List<ScatterItemRes> {
        val record = database.sequenceOf(Records)
                .find { (it.animationId eq animationId) and (it.ownerId eq user.id) }
                ?: throw NotFoundException("Record not found.")

        val publishedEpisodes = database.from(Animations)
                .select(Animations.publishedEpisodes)
                .where { Animations.id eq animationId }
                .first()[Animations.publishedEpisodes]!!

        val watchedEpisodes = if(record.progressCount == 0) null else database.from(RecordProgresses)
                .select(RecordProgresses.watchedEpisodes)
                .where { (RecordProgresses.recordId eq record.id) and (RecordProgresses.ordinal eq record.progressCount) }
                .first()[RecordProgresses.watchedEpisodes]!!

        val scatterMap = record.scatterRecord.groupBy { it.episode }.map { (episode, list) -> Pair(episode, list.size) }.toMap()

        return (1..publishedEpisodes).map { episode ->
            val progressTimes = if(watchedEpisodes == null) 0 else if(episode > watchedEpisodes) record.progressCount - 1 else record.progressCount
            ScatterItemRes(episode, progressTimes, scatterMap.getOrDefault(episode, 0))
        }
    }

    @Transactional
    override fun watchScattered(animationId: Int, user: User, episode: Int) {
        val rowSet = database.from(Records).innerJoin(Animations, Animations.id eq Records.animationId)
                .select(Records.id, Records.scatterRecord, Animations.totalEpisodes, Animations.publishedEpisodes)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")

        val recordId = rowSet[Records.id]!!
        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!

        if(episode > totalEpisodes) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "Episode $episode cannot be greater than total_episodes $totalEpisodes.")
        }else if(episode > publishedEpisodes) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "Episode $episode is not published.")
        }

        val now = DateTimeUtil.now()

        val watchedRecord = rowSet[Records.scatterRecord]!!
        val newWatchedRecord = watchedRecord + ScatterRecord(episode, now.toDateTimeString())

        database.update(Records) {
            where { it.id eq recordId }
            it.scatterRecord to newWatchedRecord
            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(ActiveEventType.WATCH_EPISODE, listOf(episode))
            it.updateTime to now
        }
    }

    @Transactional
    override fun groupScattered(animationId: Int, user: User): ScatterGroupRes {
        val rowSet = database.from(Records).innerJoin(Animations, Records.animationId eq Animations.id)
                .select(Records.id, Records.progressCount, Records.scatterRecord, Animations.totalEpisodes, Animations.publishedEpisodes)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")
        val recordId = rowSet[Records.id]!!
        val progressCount = rowSet[Records.progressCount]!!
        val scatterRecord = rowSet[Records.scatterRecord]!!
        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
        val now = DateTimeUtil.now()

        val progress = if(progressCount == 0) null else database
                .sequenceOf(RecordProgresses)
                .find { (RecordProgresses.recordId eq recordId) and (RecordProgresses.ordinal eq progressCount) }

        if(progress == null || progress.watchedEpisodes >= totalEpisodes) {
            //沉降到新进度
            val (newScatterRecord, groupedList) = groupInScatterRecord(scatterRecord, 1, publishedEpisodes, now)

            if(groupedList.isEmpty()) {
                return ScatterGroupRes(ScatterGroupRes.GroupToType.NONE, 0, 0, 0)
            }

            database.insert(RecordProgresses) {
                it.recordId to recordId
                it.ordinal to progressCount + 1
                it.watchedEpisodes to groupedList.size
                it.watchedRecord to groupedList
                it.startTime to groupedList.first()
                it.finishTime to if(groupedList.size >= totalEpisodes) groupedList.last() else null
            }

            database.update(Records) {
                where { it.id eq recordId }
                it.progressCount to progressCount + 1
                it.scatterRecord to newScatterRecord
                it.updateTime to now
            }

            return ScatterGroupRes(ScatterGroupRes.GroupToType.NEW, progressCount + 1, groupedList.size, groupedList.size)
        }else{
            //沉降到现有进度
            val (newScatterRecord, groupedList) = groupInScatterRecord(scatterRecord, progress.watchedEpisodes + 1, publishedEpisodes, progress.watchedRecord.lastOrNull() ?: now)

            if(groupedList.isEmpty()) {
                return ScatterGroupRes(ScatterGroupRes.GroupToType.NONE, progress.ordinal, progress.watchedEpisodes, 0)
            }

            val watchedEpisodes = progress.watchedEpisodes + groupedList.size

            database.update(RecordProgresses) {
                where { it.id eq progress.id }
                it.watchedEpisodes to watchedEpisodes
                it.watchedRecord to recordProcessor.calculateProgressWatchedRecord(progress.watchedRecord, progress.watchedEpisodes, progress.watchedEpisodes, now) + groupedList
                if(watchedEpisodes >= totalEpisodes) it.finishTime to now
            }

            database.update(Records) {
                where { it.id eq recordId }
                it.scatterRecord to newScatterRecord
                it.updateTime to now
            }

            return ScatterGroupRes(ScatterGroupRes.GroupToType.CURRENT, progress.ordinal, watchedEpisodes, groupedList.size)
        }
    }

    /**
     * 从离散记录中抽取出可沉降的项。
     */
    private fun groupInScatterRecord(scatterRecord: List<ScatterRecord>, fromEpisode: Int, totalEpisodes: Int, prevItem: LocalDateTime): Pair<List<ScatterRecord>, List<LocalDateTime>> {
        val scatterMap = HashMap<Int, LinkedList<LocalDateTime>>().apply {
            for ((episode, datetime) in scatterRecord) {
                if(episode >= fromEpisode) {
                    this.computeIfAbsent(episode) { LinkedList<LocalDateTime>() }.add(datetime.parseDateTime())
                }
            }
        }
        val groupedList = LinkedList<LocalDateTime>()
        var nextEpisode = fromEpisode
        while (nextEpisode <= totalEpisodes) {
            val list = scatterMap[nextEpisode] ?: break
            val item = getClosestItem(list, groupedList.lastOrNull() ?: prevItem)
            groupedList.add(item)
            nextEpisode += 1
        }

        return Pair(scatterMap.flatMap { (episode, list) -> list.map { ScatterRecord(episode, it.toDateTimeString()) } }, groupedList)
    }

    /**
     * 从链表中取得与目标项最接近的项。这会更改链表。
     */
    private fun getClosestItem(list: LinkedList<LocalDateTime>, prevItem: LocalDateTime): LocalDateTime {
        return list.apply { sortBy { Duration.between(prevItem, it).toMillis() } }.removeFirst()
    }
}
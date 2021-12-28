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
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
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
            set(it.scatterRecord, newWatchedRecord)
            set(it.lastActiveTime, now)
            set(it.lastActiveEvent, ActiveEvent(ActiveEventType.WATCH_EPISODE, listOf(episode)))
            set(it.updateTime, now)
        }
    }

    @Transactional
    override fun undoScattered(animationId: Int, user: User) {
        val rowSet = database.from(Records).innerJoin(Animations, Animations.id eq Records.animationId)
                .select(Records.id, Records.scatterRecord, Animations.totalEpisodes, Animations.publishedEpisodes)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")

        val now = DateTimeUtil.now()
        val undoLine = now.minusHours(1)

        val watchedRecord = rowSet[Records.scatterRecord]!!
        val records = watchedRecord.asSequence()
                .map { Pair(it.episode, it.watchedTime.parseDateTime()) }
                .sortedBy { it.second }
                .toList()

        if(records.isEmpty() || records.last().second < undoLine) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "There is no scatter in near 1 hour.")
        }

        val newRecord = records.subList(0, records.size - 1).map { ScatterRecord(it.first, it.second.toDateTimeString()) }
        val recordId = rowSet[Records.id]!!
        database.update(Records) {
            where { it.id eq recordId }
            set(it.scatterRecord, newRecord)
            set(it.updateTime, now)
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
                set(it.recordId, recordId)
                set(it.ordinal, progressCount + 1)
                set(it.watchedEpisodes, groupedList.size)
                set(it.watchedRecord, groupedList)
                set(it.startTime, groupedList.minOrNull()!!)
                set(it.finishTime, if(groupedList.size >= totalEpisodes) groupedList.maxOrNull()!! else null)
            }

            database.update(Records) {
                where { it.id eq recordId }
                set(it.progressCount, progressCount + 1)
                set(it.scatterRecord, newScatterRecord)
                set(it.updateTime, now)
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
                set(it.watchedEpisodes, watchedEpisodes)
                set(it.watchedRecord, recordProcessor.calculateProgressWatchedRecord(progress.watchedRecord, progress.watchedEpisodes, progress.watchedEpisodes, now) + groupedList)
                if(watchedEpisodes >= totalEpisodes) set(it.finishTime, groupedList.maxOrNull()!!)
            }

            database.update(Records) {
                where { it.id eq recordId }
                set(it.scatterRecord, newScatterRecord)
                set(it.updateTime, now)
            }

            return ScatterGroupRes(ScatterGroupRes.GroupToType.CURRENT, progress.ordinal, watchedEpisodes, groupedList.size)
        }
    }

    companion object {
        /**
         * 从离散记录中抽取出可沉降的项。
         */
        internal fun groupInScatterRecord(scatterRecord: List<ScatterRecord>, fromEpisode: Int, totalEpisodes: Int, prevItem: LocalDateTime): Pair<List<ScatterRecord>, List<LocalDateTime>> {
            val unusedRecord = ArrayList<ScatterRecord>(scatterRecord.size)
            val scatterMap = HashMap<Int, LinkedList<LocalDateTime>>().apply {
                for (record in scatterRecord) {
                    val (episode, datetime) = record
                    if(episode >= fromEpisode) {
                        this.computeIfAbsent(episode) { LinkedList() }.add(datetime.parseDateTime())
                    }else{
                        unusedRecord.add(record)
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

            val leftRecord = scatterMap.flatMap { (episode, list) ->
                list.map { ScatterRecord(episode, it.toDateTimeString()) }
            }

            return Pair(unusedRecord + leftRecord, groupedList)
        }

        /**
         * 从链表中取得与目标项最接近的项。这会更改链表。
         */
        private fun getClosestItem(list: LinkedList<LocalDateTime>, prevItem: LocalDateTime): LocalDateTime {
            return list.apply { sortBy { Duration.between(it, prevItem).toMillis() } }.removeFirst()
        }
    }
}
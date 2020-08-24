package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.*
import com.heerkirov.animation.model.result.*
import com.heerkirov.animation.service.RecordGetterService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.*
import com.heerkirov.animation.util.ktorm.dsl.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.support.postgresql.ilike
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class RecordGetterServiceImpl(@Autowired private val database: Database,
                              @Autowired private val recordProcessor: RecordProcessor) : RecordGetterService {
    private val findOrderTranslator = OrderTranslator {
        "create_time" to Animations.createTime
        "update_time" to Animations.updateTime
        "publish_time" to Animations.publishTime
    }

    private val detailFields = arrayOf(
            Animations.title, Animations.cover, Records.seenOriginal, Records.inDiary, Records.scatterRecord,
            Animations.totalEpisodes, Animations.publishedEpisodes, Animations.publishPlan, Records.progressCount,
            Records.createTime, Records.updateTime, RecordProgresses.watchedEpisodes
    )

    override fun diary(filter: DiaryFilter, user: User): DiaryResult {
        val nightTimeTable = user.setting.nightTimeTable
        val zone = ZoneId.of(user.setting.timezone)
        val (direction, order) = filter.order.firstOrNull() ?: throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'order' is required.")

        val items = database.from(Records)
                .innerJoin(Animations, Animations.id eq Records.animationId)
                .leftJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id) and (RecordProgresses.ordinal eq Records.progressCount))
                .select(Animations.id, Animations.title, Animations.cover,
                        Animations.totalEpisodes, Animations.publishedEpisodes, Animations.publishPlan,
                        Records.progressCount, Records.createTime, RecordProgresses.watchedEpisodes, RecordProgresses.startTime)
                .whereWithConditions {
                    it += Records.ownerId eq user.id
                    it += Records.inDiary eq true
                    when(filter.filter) {
                        "active" -> it += (RecordProgresses.watchedEpisodes.isNotNull() and (RecordProgresses.watchedEpisodes less Animations.publishedEpisodes)) or (Animations.publishPlan notEq emptyList())
                        "watchable" -> it += RecordProgresses.watchedEpisodes.isNotNull() and (RecordProgresses.watchedEpisodes less Animations.publishedEpisodes)
                        "updating" -> it += Animations.publishPlan notEq emptyList()
                        "completed" -> it += Animations.publishedEpisodes eq Animations.totalEpisodes
                        "shelve" -> it += (Animations.publishedEpisodes less Animations.totalEpisodes) and (RecordProgresses.watchedEpisodes eq Animations.publishedEpisodes) and (Animations.publishPlan eq emptyList())
                        null -> {}  //null时什么也不做
                        else -> throw UnsupportedOperationException()
                    }
                }
                .asSequence()
                .map { DiaryItem(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Animations.totalEpisodes]!!,
                        it[Animations.publishedEpisodes]!!,
                        it[RecordProgresses.watchedEpisodes],
                        it[Animations.publishPlan]!!.firstOrNull()?.toDateTimeString(),
                        recordProcessor.getStatus(it[Records.progressCount]!!, it[Animations.totalEpisodes]!!, it[RecordProgresses.watchedEpisodes]),
                        (it[RecordProgresses.startTime] ?: it[Records.createTime]!!).toDateTimeString()
                ) }
                .let { recordProcessor.sortDiary(it, direction, order, nightTimeTable, zone) }
                .toList()

        return DiaryResult(items, nightTimeTable)
    }

    override fun timetable(user: User): TimetableResult {
        val nightTimeTable = user.setting.nightTimeTable
        val zone = ZoneId.of(user.setting.timezone)

        val items = database.from(Animations)
                .innerJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id))
                .select()
                .where { (Animations.totalEpisodes greater Animations.publishedEpisodes) and (Animations.publishPlan notEq emptyList()) }
                .asSequence()
                .map { TimetableItem(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Animations.publishPlan]!!.first().toDateTimeString(),
                        it[Animations.publishedEpisodes]!! + 1
                ) }
                .map { Pair(it.nextPublishTime.parseDateTime().toLocalTime(), it) }
                .sortedBy { it.first }
                .map { it.second }
                .groupBy {
                    it.nextPublishTime
                            .parseDateTime()
                            .asZonedTime(zone)
                            .runIf(nightTimeTable) { t -> t.minusHours(recordProcessor.nightTimeTableHourOffset) }
                            .dayOfWeek.value
                }

        return TimetableResult(items, nightTimeTable)
    }

    override fun activity(filter: ActivityFilter, user: User): ListResult<ActivityRes> {
        return database.from(Records)
                .innerJoin(Animations, Animations.id eq Records.animationId)
                .leftJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id) and (RecordProgresses.ordinal eq Records.progressCount))
                .select(Animations.id, Animations.title, Animations.cover, Animations.totalEpisodes,
                        Records.lastActiveTime, Records.lastActiveEvent, Records.progressCount,
                        RecordProgresses.watchedEpisodes)
                .whereWithConditions {
                    it += (Records.ownerId eq user.id) and (Records.lastActiveTime.isNotNull())
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                }
                .orderBy(Records.lastActiveTime.desc())
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { ActivityRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Records.lastActiveTime]!!.toDateTimeString(),
                        it[Records.lastActiveEvent]!!,
                        recordProcessor.calculateProgress(it[Records.progressCount]!!, it[Animations.totalEpisodes]!!, it[RecordProgresses.watchedEpisodes])
                ) }
    }

    override fun history(filter: HistoryFilter, user: User): ListResult<HistoryRes> {
        return database.from(RecordProgresses)
                .innerJoin(Records, Records.id eq RecordProgresses.recordId)
                .innerJoin(Animations, Animations.id eq Records.animationId)
                .select(Animations.id, Animations.title, Animations.cover, RecordProgresses.startTime,
                        RecordProgresses.finishTime, RecordProgresses.ordinal)
                .whereWithConditions {
                    it += (Records.ownerId eq user.id) and (RecordProgresses.finishTime.isNotNull())
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                    when (filter.ordinal) {
                        "first" -> it += RecordProgresses.ordinal eq 1
                        "last" -> it += RecordProgresses.ordinal eq Records.progressCount
                        "rewatched" -> it += RecordProgresses.ordinal greater 1
                    }
                }
                .orderBy(RecordProgresses.finishTime.desc())
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { HistoryRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[RecordProgresses.startTime]?.toDateTimeString(),
                        it[RecordProgresses.finishTime]!!.toDateTimeString(),
                        it[RecordProgresses.ordinal]!!
                ) }
    }

    override fun scale(filter: ScaleFilter, user: User): List<ScaleRes> {
        val lower = filter.lower ?: throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'lower' is required.")
        val upper = filter.upper ?: throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'upper' is required.")

        data class Row(val id: Int, val title: String, val cover: String?, val ordinal: Int, val start: LocalDateTime, val end: LocalDateTime, val finished: Boolean)

        return database.from(RecordProgresses)
                .innerJoin(Records, (Records.id eq RecordProgresses.recordId) and (Records.ownerId eq user.id))
                .innerJoin(Animations, Animations.id eq Records.animationId)
                .select(Animations.id, Animations.title, Animations.cover, RecordProgresses.ordinal, RecordProgresses.startTime, RecordProgresses.finishTime, RecordProgresses.watchedRecord)
                .whereWithConditions {
                    //由于startTime/finishTime并不能用作最终范围，因此在where条件中仅做一次粗筛
                    it += RecordProgresses.finishTime.isNull() or (RecordProgresses.finishTime greaterEq lower)
                    it += RecordProgresses.startTime.isNull() or (RecordProgresses.startTime lessEq upper)
                }.asSequence()
                .map { row ->
                    val watchedRecord = row[RecordProgresses.watchedRecord]!!
                    val startTime = row[RecordProgresses.startTime]
                    val finishTime = row[RecordProgresses.finishTime]

                    val start = startTime ?: watchedRecord.firstOrNull { it != null } ?: finishTime
                    val end = finishTime ?: watchedRecord.lastOrNull { it != null } ?: startTime
                    if (start == null || end == null) null else {
                        Row(row[Animations.id]!!, row[Animations.title]!!, row[Animations.cover], row[RecordProgresses.ordinal]!!, start, end, finishTime != null)
                    }
                }.filterNotNull()
                .filter { it.start <= upper && it.end >= lower }
                .sortedBy { it.start }
                .map { ScaleRes(it.id, it.title, it.cover, it.ordinal, it.start.toDateTimeString(), it.end.toDateTimeString(), it.finished) }
                .toList()
    }

    override fun find(filter: FindFilter, user: User): ListResult<FindRes> {
        return when(filter.filter) {
            "not_seen" -> findNotSeen(filter, user)
            "recorded" -> findRecorded(filter, user)
            "incomplete" -> findInComplete(filter, user)
            else -> throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'filter' is required.")
        }
    }

    override fun get(animationId: Int, user: User): RecordDetailRes {
        val rowSet = database.from(Records)
                .innerJoin(Animations, Records.animationId eq Animations.id)
                .leftJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id) and (RecordProgresses.ordinal eq Records.progressCount))
                .select(*detailFields)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull()
                ?: throw NotFoundException("Record not found.")

        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
        val progressCount = rowSet[Records.progressCount]!!
        val watchedEpisodes = rowSet[RecordProgresses.watchedEpisodes]

        return RecordDetailRes(
                animationId = animationId,
                title = rowSet[Animations.title]!!,
                cover = rowSet[Animations.cover],
                seenOriginal = rowSet[Records.seenOriginal]!!,
                status = recordProcessor.getStatus(progressCount, totalEpisodes, watchedEpisodes),
                inDiary = rowSet[Records.inDiary]!!,
                totalEpisodes = totalEpisodes,
                publishedEpisodes = publishedEpisodes,
                watchedEpisodes = watchedEpisodes ?: 0,
                publishPlan = rowSet[Animations.publishPlan]!!.map { it.toDateTimeString() },
                progressCount = progressCount,
                createTime = rowSet[Records.createTime]!!.toDateTimeString(),
                updateTime = rowSet[Records.updateTime]!!.toDateTimeString()
        )
    }

    private fun findNotSeen(filter: FindFilter, user: User): ListResult<FindRes> {
        return database.from(Animations)
                .leftJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id))
                .select(Animations.id, Animations.title, Animations.cover, Animations.totalEpisodes, Animations.publishedEpisodes)
                .whereWithConditions {
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                    it += (Records.ownerId.isNull())
                }
                .orderBy(filter.order, findOrderTranslator)
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { FindRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Animations.totalEpisodes]!!,
                        it[Animations.publishedEpisodes]!!,
                        null,
                        null
                ) }
    }

    private fun findRecorded(filter: FindFilter, user: User): ListResult<FindRes> {
        return database.from(Animations)
                .innerJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id))
                .select(Animations.id, Animations.title, Animations.cover, Animations.totalEpisodes, Animations.publishedEpisodes)
                .whereWithConditions {
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                    it += (Records.progressCount eq 0) and (Records.inDiary eq false)
                }
                .orderBy(filter.order, findOrderTranslator)
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { FindRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Animations.totalEpisodes]!!,
                        it[Animations.publishedEpisodes]!!,
                        null,
                        null
                ) }
    }

    private fun findInComplete(filter: FindFilter, user: User): ListResult<FindRes> {
        return database.from(Animations)
                .innerJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id))
                .innerJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id) and (RecordProgresses.ordinal eq Records.progressCount))
                .select(Animations.id, Animations.title, Animations.cover, Animations.totalEpisodes, Animations.publishedEpisodes,
                        RecordProgresses.watchedEpisodes, Records.progressCount)
                .whereWithConditions {
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                    it += (RecordProgresses.finishTime.isNull()) and (Records.inDiary eq false)
                }
                .orderBy(filter.order, findOrderTranslator)
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { FindRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Animations.totalEpisodes]!!,
                        it[Animations.publishedEpisodes]!!,
                        it[RecordProgresses.watchedEpisodes]!!,
                        recordProcessor.calculateProgress(it[Records.progressCount]!!, it[Animations.totalEpisodes]!!, it[RecordProgresses.watchedEpisodes]!!)
                ) }
    }
}
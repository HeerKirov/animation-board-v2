package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.ActivityFilter
import com.heerkirov.animation.model.filter.DiaryFilter
import com.heerkirov.animation.model.filter.FindFilter
import com.heerkirov.animation.model.filter.HistoryFilter
import com.heerkirov.animation.model.result.*
import com.heerkirov.animation.service.RecordGetterService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.Tuple4
import me.liuwj.ktorm.support.postgresql.ilike
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZoneId
import java.time.temporal.ChronoField

@Service
class RecordGetterServiceImpl(@Autowired private val database: Database,
                              @Autowired private val recordProcessor: RecordProcessor) : RecordGetterService {
    private val nightTimeTableHourOffset = 2L   //夜晚时间表将0点之后2个小时内的时间视作今天
    private val weekDurationAvailable = 2       //距今天差2周以内的被放入周历表排序

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
                .sortDiary(direction, order, nightTimeTable, zone)
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
                            .runIf(nightTimeTable) { t -> t.minusHours(nightTimeTableHourOffset) }
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

    private fun Sequence<DiaryItem>.sortDiary(direction: Int, order: String, nightTimeTable: Boolean, timezone: ZoneId): Sequence<DiaryItem> {
        val now = DateTimeUtil.now()
        return when(order) {
            "weekly_calendar" -> {
                val zonedNow = now.asZonedTime(timezone)
                this.map {
                    //将next publish plan拆解成几项参数: 完整时间, 周数差，周内时间
                    if(it.nextPublishPlan != null) {
                        val datetime = it.nextPublishPlan
                                .parseDateTime()
                                .asZonedTime(timezone)
                                .runIf(nightTimeTable) { t -> t.minusHours(nightTimeTableHourOffset) }
                        val weekDuration = weekDuration(zonedNow, datetime)
                        val weekday = datetime.dayOfWeek.value
                        val minute = datetime.getLong(ChronoField.MINUTE_OF_DAY)
                        val timeInWeek = weekday * 60 * 24 + minute
                        Tuple4(it, datetime, weekDuration, timeInWeek)
                    }else{
                        Tuple4(it, null, 0, 0L)
                    }
                }.sortedWith(Comparator { (a, aTime, aWeekDuration, aMinute), (b, bTime, bWeekDuration, bMinute) ->
                    if(aTime != null && bTime != null) { //都有更新计划
                        if(aWeekDuration <= weekDurationAvailable && bWeekDuration <= weekDurationAvailable) {  //都在最近2周以内
                            if(aMinute != bMinute) {    //按周内时间排序
                                aMinute.compareTo(bMinute) * direction
                            }else{  //最后按id排序
                                a.animationId.compareTo(b.animationId) * direction
                            }
                        }else if(aWeekDuration > weekDurationAvailable && aWeekDuration > weekDurationAvailable) {  //都在最近两周以外
                            aTime.compareTo(bTime) * direction   //按完整时间排序
                        }else if(aWeekDuration <= weekDurationAvailable) { -direction }else{ direction } //a在以内，那么a优先，且降序时反转
                    }else if(aTime == null && bTime == null) { //都没有更新计划
                        val aWatched = a.watchedEpisodes ?: 0
                        val bWatched = b.watchedEpisodes ?: 0
                        if(aWatched < a.publishedEpisodes && bWatched < b.publishedEpisodes) {    //都有存货
                            a.subscriptionTime.compareTo(b.subscriptionTime) //按订阅时间排序。UTC时间戳可以直接这么比
                        }else if(a.watchedEpisodes ?: 0 >= a.publishedEpisodes && b.watchedEpisodes ?: 0 >= b.publishedEpisodes) {    //都没有存货
                            if(!((a.status == RecordStatus.COMPLETED) xor (b.status == RecordStatus.COMPLETED))) {  //都已完结或都未完结
                                a.animationId.compareTo(b.animationId)  //按id排序
                            }else if(a.status == RecordStatus.COMPLETED) { -1 }else{ 1 }    //a已完结，那么不论direction总是优先，否则就是b优先
                        }else if(a.watchedEpisodes ?: 0 < a.publishedEpisodes) { -1 }else{ 1 }   //a有存货，那么不论direction总是优先，否则就是b优先
                    }else if(aTime != null) { -1 }else{ 1 } //a有更新计划，那么不论direction总是优先，否则就是b优先
                }).map { it.element1 }
            }
            "update_soon" -> {
                this.map {
                    //将下次更新计划时间转换为它对now的时间差，越早的差越小
                    Pair(it, if(it.nextPublishPlan != null) Duration.between(it.nextPublishPlan.parseDateTime(), now).toMinutes() else null)
                }.sortedWith(Comparator { (a, aDuration), (b, bDuration) ->
                    if(aDuration != null && bDuration != null) {    //a和b都有时间差
                        aDuration.compareTo(bDuration) * direction
                    }else if(aDuration == null && bDuration == null) {  //a和b都没有时间差
                        val aWatched = a.watchedEpisodes ?: 0
                        val bWatched = b.watchedEpisodes ?: 0
                        if(aWatched < a.publishedEpisodes && bWatched < b.publishedEpisodes) {    //都有存货
                            a.subscriptionTime.compareTo(b.subscriptionTime) //按订阅时间排序。UTC时间戳可以直接这么比
                        }else if(a.watchedEpisodes ?: 0 >= a.publishedEpisodes && b.watchedEpisodes ?: 0 >= b.publishedEpisodes) {    //都没有存货
                            if(!((a.status == RecordStatus.COMPLETED) xor (b.status == RecordStatus.COMPLETED))) {  //都已完结或都未完结
                                a.animationId.compareTo(b.animationId)  //按id排序
                            }else if(a.status == RecordStatus.COMPLETED) { -1 }else{ 1 }    //a已完结，那么不论direction总是优先，否则就是b优先
                        }else if(a.watchedEpisodes ?: 0 < a.publishedEpisodes) { -1 }else{ 1 }   //a有存货，那么不论direction总是优先，否则就是b优先
                    }else if(aDuration != null) { -1 }else{ 1 } //a有时间差，那么不论direction总是a优先
                }).map { it.first }
            }
            "subscription_time" -> {
                this.map { Pair(it, it.subscriptionTime.parseDateTime()) }.sortedBy { it.second }.map { it.first }
            }
            else -> throw UnsupportedOperationException()
        }
    }
}
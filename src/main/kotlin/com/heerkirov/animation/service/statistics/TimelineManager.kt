package com.heerkirov.animation.service.statistics

import com.heerkirov.animation.dao.*
import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.enums.PublishType
import com.heerkirov.animation.enums.StatisticType
import com.heerkirov.animation.model.data.TimelineModal
import com.heerkirov.animation.model.data.TimelineOverviewModal
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.TimelineOverviewRes
import com.heerkirov.animation.model.result.TimelineRes
import com.heerkirov.animation.util.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

@Component
class TimelineManager(@Autowired private val database: Database) {
    fun getOverview(user: User): TimelineOverviewRes {
        return database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.TIMELINE_OVERVIEW) }
                .firstOrNull()
                ?.let {
                    val modal = it[Statistics.content]!!.parseJSONObject<TimelineOverviewModal>()
                    TimelineOverviewRes(modal.beginYear, modal.beginMonth, modal.endYear, modal.endMonth, it[Statistics.updateTime]!!.toDateTimeString())
                }
                ?: TimelineOverviewRes(null, null, null, null, null)
    }

    fun get(user: User, lower: LocalDate, upper: LocalDate, aggregateTimeUnit: AggregateTimeUnit): TimelineRes {
        val data = database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.TIMELINE) and (Statistics.key greaterEq lower.toDateMonthString()) and (Statistics.key lessEq upper.toDateMonthString()) }
                .orderBy(Statistics.key.asc())
                .map { Statistics.createEntity(it) }

        val updateTime = data.asSequence().map { it.updateTime }.max()?.toDateTimeString()

        val items = data
                .groupBy {
                    when(aggregateTimeUnit) {
                        AggregateTimeUnit.MONTH -> it.key!!
                        AggregateTimeUnit.SEASON -> it.key!!.parseDateMonth()!!.run { "$year-${(monthValue - 1) / 3 + 1}" }
                        AggregateTimeUnit.YEAR -> it.key!!.parseDateMonth()!!.year.toString()
                    }
                }.map { (time, statistics) ->
                    val modals = statistics.map { it.content.parseJSONObject<TimelineModal>() }

                    TimelineRes.Item(time,
                            modals.sumBy { it.chaseAnimations ?: 0 },
                            modals.sumBy { it.chaseEpisodes ?: 0 },
                            modals.sumBy { it.chaseDuration ?: 0 },
                            modals.sumBy { it.supplementAnimations ?: 0 },
                            modals.sumBy { it.supplementEpisodes ?: 0 },
                            modals.sumBy { it.supplementDuration ?: 0 },
                            modals.sumBy { it.rewatchedAnimations },
                            modals.sumBy { it.rewatchedEpisodes },
                            modals.sumBy { it.rewatchedDuration },
                            modals.sumBy { it.scatterEpisodes },
                            modals.sumBy { it.scatterDuration },
                            modals.asSequence().map { it.maxScore }.filterNotNull().max(),
                            modals.asSequence().map { it.minScore }.filterNotNull().min(),
                            modals.sumBy { it.scoredAnimations }.let { scoredAnimations ->
                                if(scoredAnimations == 0) null else {
                                    modals.sumBy { it.sumScore } * 1.0 / scoredAnimations
                                }
                            },
                            modals.flatMap { it.scoreCounts?.toList() ?: emptyList() }
                                    .groupBy({ it.first }) { it.second }
                                    .mapValues { (_, v) -> v.sum() }
                    )
                }

        return TimelineRes(items, updateTime)
    }

    fun update(user: User) {
        val modal = generate(user)
        val now = DateTimeUtil.now()

        val min = modal.keys.min()?.parseDateMonth()
        val max = modal.keys.max()?.parseDateMonth()
        val overview = TimelineOverviewModal(min?.year, min?.monthValue, max?.year, max?.monthValue)
        val overviewId = database.from(Statistics)
                .select(Statistics.id)
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.TIMELINE_OVERVIEW) }
                .firstOrNull()?.get(Statistics.id)
        if(overviewId == null) {
            database.insert(Statistics) {
                it.ownerId to user.id
                it.type to StatisticType.TIMELINE_OVERVIEW
                it.key to null
                it.content to overview.toJSONString()
                it.updateTime to now
            }
        }else{
            database.update(Statistics) {
                where { it.id eq overviewId }
                it.content to overview.toJSONString()
                it.updateTime to now
            }
        }

        val currentItems = database.from(Statistics)
                .select(Statistics.key)
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.TIMELINE) }
                .map { it[Statistics.key]!! }
                .toSet()

        val minus = currentItems - modal.keys
        if(minus.isNotEmpty()) {
            database.delete(Statistics) {
                (it.ownerId eq user.id) and (it.type eq StatisticType.TIMELINE) and (it.key inList minus)
            }
        }
        val appends = modal.keys - currentItems
        if(appends.isNotEmpty()) {
            database.batchInsert(Statistics) {
                for (append in appends) {
                    item {
                        it.ownerId to user.id
                        it.type to StatisticType.TIMELINE
                        it.key to append
                        it.content to modal.getValue(append).toJSONString()
                        it.updateTime to now
                    }
                }
            }
        }
        val updates = modal.keys.intersect(currentItems)
        if(updates.isNotEmpty()) {
            database.batchUpdate(Statistics) {
                for (update in updates) {
                    item {
                        where { (it.ownerId eq user.id) and (it.type eq StatisticType.TIMELINE) and (it.key eq update) }
                        it.content to modal.getValue(update).toJSONString()
                        it.updateTime to now
                    }
                }
            }
        }
    }

    fun generate(user: User): Map<String, TimelineModal> {
        data class ProgressRow(val episodeDuration: Int?, val score: Int?, val ordinal: Int, val timePoint: List<LocalDate>, val completed: Boolean, val chase: Boolean)
        data class ScatterRow(val episodeDuration: Int?, val scatterRecord: List<LocalDate>)
        data class ScoredRow(val scoredAnimations: Int, val sumScore: Int, val maxScore: Int?, val minScore: Int?, val scoreCounts: Map<Int, Int>)

        val zone = ZoneId.of(user.setting.timezone)

        //基于进度取得进度数据
        val progressRows = database.from(Animations)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .innerJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id))
                .leftJoin(Comments, (Comments.animationId eq Animations.id) and (Comments.ownerId eq user.id) and (Comments.score.isNotNull()))
                .select(Animations.episodeDuration, Animations.publishTime, Animations.publishType,
                        Comments.score, RecordProgresses.ordinal, RecordProgresses.watchedRecord,
                        RecordProgresses.watchedEpisodes, RecordProgresses.startTime, RecordProgresses.finishTime)
                .map {
                    val ordinal = it[RecordProgresses.ordinal]!!
                    val watchedEpisodes = it[RecordProgresses.watchedEpisodes]!!
                    val startTime = it[RecordProgresses.startTime]?.asZonedTime(zone)?.toLocalDate()
                    val finishTime = it[RecordProgresses.finishTime]?.asZonedTime(zone)?.toLocalDate()
                    val watchedRecord = it[RecordProgresses.watchedRecord]!!.map { t -> t?.asZonedTime(zone)?.toLocalDate() }
                    val publishType = it[Animations.publishType]
                    //由于startTime已经在时区转换后做了脱区处理，比较的只是Date部分，不需要再处理了
                    val maxChaseTime = it[Animations.publishTime]?.let { d -> LocalDate.of(d.year, (d.monthValue - 1) / 3 * 3 + 1, 1).plusMonths(3) }

                    ProgressRow(it[Animations.episodeDuration], it[Comments.score], ordinal,
                            getTimePointOfProgress(watchedRecord, watchedEpisodes, startTime, finishTime), finishTime != null,
                            //区分追番进度的条件为"进度序号为1"、"放送类型为TV&WEB"、"用户订阅时间在当季(取发布时间所在季度的3个月)三个月内或更早"。
                            ordinal == 1 && publishType == PublishType.TV_AND_WEB && startTime != null && maxChaseTime != null && startTime < maxChaseTime)
                }

        //基于记录取得离散数据
        val scatterRows = database.from(Animations)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .select(Animations.episodeDuration, Records.scatterRecord)
                .map { ScatterRow(it[Animations.episodeDuration], it[Records.scatterRecord]!!.map { r -> r.watchedTime.parseDateTime().asZonedTime(zone).toLocalDate() }) }

        val scatterEpisodesAndDurations = scatterRows.asSequence()
                .flatMap { it.scatterRecord.asSequence().map { r -> Pair(r.toDateMonthString(), it.episodeDuration) } }
                .groupBy({ it.first }) { it.second }
                .mapValues { Pair(it.value.size, it.value.filterNotNull().sum()) }

        val secondaryProgressEpisodesAndDurations = progressRows.asSequence()
                .filter { it.ordinal > 1 }
                .flatMap { it.timePoint.asSequence().map { r -> Pair(r.toDateMonthString(), it.episodeDuration) } }
                .groupBy({ it.first }) { it.second }
                .mapValues { Pair(it.value.size, it.value.filterNotNull().sum()) }
        val secondaryProgressAnimations = progressRows.asSequence()
                .filter { it.completed && it.ordinal > 1 && it.timePoint.isNotEmpty() }
                .groupBy { it.timePoint.last().toDateMonthString() }
                .mapValues { it.value.count() }

        val firstProgressEpisodesAndDurations = progressRows.asSequence()
                .filter { !it.chase && it.ordinal == 1 }
                .flatMap { it.timePoint.asSequence().map { r -> Pair(r.toDateMonthString(), it.episodeDuration) } }
                .groupBy({ it.first }) { it.second }
                .mapValues { Pair(it.value.size, it.value.filterNotNull().sum()) }
        val firstProgressAnimations = progressRows.asSequence()
                .filter { !it.chase && it.completed && it.ordinal == 1 && it.timePoint.isNotEmpty() }
                .groupBy { it.timePoint.last().toDateMonthString() }
                .mapValues { it.value.count() }

        val chaseProgressEpisodesAndDurations = progressRows.asSequence()
                .filter { it.chase && it.ordinal == 1 }
                .flatMap { it.timePoint.asSequence().map { r -> Pair(r.toDateMonthString(), it.episodeDuration) } }
                .groupBy({ it.first }) { it.second }
                .mapValues { Pair(it.value.size, it.value.filterNotNull().sum()) }
        val chaseProgressAnimations = progressRows.asSequence()
                .filter { it.chase && it.completed && it.ordinal == 1 && it.timePoint.isNotEmpty() }
                .groupBy { it.timePoint.last().toDateMonthString() }
                .mapValues { it.value.count() }

        val firstProgressScores = progressRows.asSequence()
                .filter { it.completed && it.ordinal == 1 && it.timePoint.isNotEmpty() && it.score != null }
                .groupBy { it.timePoint.last().toDateMonthString() }
                .mapValues { (_, list) ->
                    val scoredAnimations = list.count()
                    val maxScore = list.maxBy { it.score!! }?.score
                    val minScore = list.minBy { it.score!! }?.score
                    val sumScore = list.sumBy { it.score!! }
                    val scoreCounts = list.groupingBy { it.score!! }.eachCount()
                    ScoredRow(scoredAnimations, sumScore, maxScore, minScore, scoreCounts)
                }

        //四个条件都取到了各自的最大覆盖范围，因此只需这四个就能得到全部key
        val keys = scatterEpisodesAndDurations.keys + secondaryProgressEpisodesAndDurations.keys + firstProgressEpisodesAndDurations.keys + chaseProgressEpisodesAndDurations.keys
        if(keys.isEmpty()) {
            return emptyMap()
        }

        val keyValues = keys.associateWith {
            val (chaseEpisodes, chaseDuration) = chaseProgressEpisodesAndDurations[it] ?: Pair(0, 0)
            val (watchedEpisodes, watchedDuration) = firstProgressEpisodesAndDurations[it] ?: Pair(0, 0)
            val (rewatchedEpisodes, rewatchedDuration) = secondaryProgressEpisodesAndDurations[it] ?: Pair(0, 0)
            val (scatterEpisodes, scatterDuration) = scatterEpisodesAndDurations[it] ?: Pair(0, 0)
            val scored = firstProgressScores[it]
            TimelineModal(
                    chaseProgressAnimations[it] ?: 0, chaseEpisodes, chaseDuration,
                    firstProgressAnimations[it] ?: 0, watchedEpisodes, watchedDuration,
                    secondaryProgressAnimations[it] ?: 0, rewatchedEpisodes, rewatchedDuration,
                    scatterEpisodes, scatterDuration,
                    scored?.scoredAnimations ?: 0, scored?.maxScore, scored?.minScore, scored?.sumScore ?: 0, scored?.scoreCounts ?: emptyMap()
            )
        }

        //迭代从最小值到最大值的全部时间点，防止无数据的时间点被遗漏
        return stepFor(keys.min()!!.parseDateMonth()!!, keys.max()!!.parseDateMonth()!!) { it.plusMonths(1) }
                .asSequence()
                .map { it.toDateMonthString() }
                .map { Pair(it, keyValues[it] ?: TimelineModal(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null, 0, emptyMap())) }
                .toMap()
    }
}

/**
 * 将观看时间点整理为确定的时间点分布。
 * 由于实际记录中的观看时间可能部分或全部缺失，因此需要一个计算方法将缺失部分补全或假设计算出来。
 * 这个方法的预定返回值项数为watchedEpisodes的数量。然而在某些情况下，部分时间点无法推断，因此实际返回数量可能少于此数，甚至返回0项。
 */
fun getTimePointOfProgress(watchedRecord: List<LocalDate?>, watchedEpisodes: Int, startTime: LocalDate?, finishTime: LocalDate?): List<LocalDate> {
    if(watchedEpisodes == 0 || ((watchedRecord.isEmpty() || watchedRecord.all { it == null }) && startTime == null && finishTime == null)) {
        return emptyList()
    }

    val record = Array(watchedEpisodes) { watchedRecord.getOrNull(it) }
    if(record.firstOrNull() == null && startTime != null) { record[0] = startTime }
    if(record.lastOrNull() == null && finishTime != null) { record[record.size - 1] = finishTime }

    var prevIndex: Int? = null
    var prev: LocalDate? = null
    var nextIndex: Int? = null
    var next: LocalDate? = null

    fun findNext() {
        for(i in (nextIndex ?: -1) + 1 until record.size) {
            if(record[i] != null) {
                nextIndex = i
                next = record[i]
                return
            }
        }
        nextIndex = null
        next = null
    }

    findNext()

    return IntRange(0, watchedEpisodes - 1).map { i ->
        if(nextIndex == i) {
            val value = next!!
            prevIndex = nextIndex
            prev = next
            findNext()
            value
        }else if(prev != null && next != null) {
            val delta = (next!!.toEpochDay() - prev!!.toEpochDay()) * (i - prevIndex!!) * 1.0 / (nextIndex!! - prevIndex!!)
            LocalDate.ofEpochDay((prev!!.toEpochDay() + delta).roundToLong())!!
        }else if(next != null) {
            next!!
        }else if(prev != null) {
            prev!!
        }else{
            throw RuntimeException("Unexpected error: prev and next are both null.")
        }
    }
}
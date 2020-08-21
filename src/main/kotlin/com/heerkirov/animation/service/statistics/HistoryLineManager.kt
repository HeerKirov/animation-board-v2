package com.heerkirov.animation.service.statistics

import com.heerkirov.animation.dao.*
import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.enums.PublishType
import com.heerkirov.animation.enums.StatisticType
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.HistoryLineModal
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.HistoryLineRes
import com.heerkirov.animation.model.result.SeasonOverviewRes
import com.heerkirov.animation.util.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.ZoneId

@Component
class HistoryLineManager(@Autowired private val database: Database) {
    fun getOverview(user: User): SeasonOverviewRes {
        return database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.HISTORY) }
                .firstOrNull()
                ?.let {
                    val modal = it[Statistics.content]!!.parseJSONObject<HistoryLineModal>()
                    SeasonOverviewRes(modal.beginYear, modal.beginSeason, modal.endYear, modal.endSeason, it[Statistics.updateTime]!!.toDateTimeString())
                }
                ?: SeasonOverviewRes(null, null, null, null, null)
    }

    fun get(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int, aggregateTimeUnit: AggregateTimeUnit): HistoryLineRes {
        if(aggregateTimeUnit == AggregateTimeUnit.MONTH) throw RuntimeException("History line statistics cannot be grouped in month time range.")

        val rowSet = database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.HISTORY) }
                .firstOrNull()
                ?: throw NotFoundException("Statistic not found.")
        val updateTime = rowSet[Statistics.updateTime]!!
        val content = rowSet[Statistics.content]!!.parseJSONObject<HistoryLineModal>()
        val lower = compValue(lowerYear, lowerSeason)
        val upper = compValue(upperYear, upperSeason)

        val items = content.items.asSequence()
                .map { Pair(compValue(it.year, it.season), it) }
                .filter { it.first in lower..upper }
                .groupBy({ if(aggregateTimeUnit == AggregateTimeUnit.SEASON) { "${it.second.year}-${it.second.season}" }else{ it.second.year.toString() } }) { it.second }
                .map { (time, items) ->
                    val chaseAnimations = items.sumBy { it.chaseAnimations ?: 0 }
                    val supplementAnimations = items.sumBy { it.supplementAnimations ?: 0 }
                    val scoredAnimations = items.sumBy { it.scoredAnimations }
                    val maxScore = items.asSequence().map { it.maxScore }.filterNotNull().max()
                    val minScore = items.asSequence().map { it.minScore }.filterNotNull().min()
                    val sumScore = items.asSequence().map { it.sumScore }.sum()
                    val avgScore = if(scoredAnimations == 0) null else { sumScore * 1.0 / scoredAnimations }
                    val scoreCounts = items.flatMap { it.scoreCounts?.toList() ?: emptyList() }
                            .groupBy({ it.first }) { it.second }
                            .mapValues { (_, v) -> v.sum() }
                    HistoryLineRes.Item(time, chaseAnimations, supplementAnimations, maxScore, minScore, avgScore, scoreCounts)
                }
                .sortedBy { it.time }
                .toList()

        return HistoryLineRes(items, updateTime.toDateTimeString())
    }

    fun update(user: User) {
        val modal = generate(user)

        val id = database.from(Statistics).select(Statistics.id)
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.HISTORY) }
                .firstOrNull()?.get(Statistics.id)
        if(id == null) {
            database.insert(Statistics) {
                it.ownerId to user.id
                it.type to StatisticType.HISTORY
                it.key to null
                it.content to modal.toJSONString()
                it.updateTime to DateTimeUtil.now()
            }
        }else{
            database.update(Statistics) {
                where { it.id eq id }
                it.content to modal.toJSONString()
                it.updateTime to DateTimeUtil.now()
            }
        }
    }

    fun generate(user: User): HistoryLineModal {
        data class Row(val publishTime: LocalDate, val score: Int?, val chase: Boolean)

        val zone = ZoneId.of(user.setting.timezone)

        val rowSets = database.from(Animations)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .leftJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id) and (RecordProgresses.ordinal eq 1) and (RecordProgresses.startTime.isNotNull()))
                .leftJoin(Comments, (Comments.animationId eq Animations.id) and (Comments.ownerId eq user.id) and (Comments.score.isNotNull()))
                .select(Animations.publishTime, Animations.publishType, Comments.score, RecordProgresses.startTime)
                .where { Animations.publishTime.isNotNull() }
                .asSequence()
                .map {
                    val score = it[Comments.score]
                    val startTime = it[RecordProgresses.startTime]?.asZonedTime(zone)?.toLocalDate()
                    val publishType = it[Animations.publishType]
                    val publishTime = it[Animations.publishTime]!!
                    val maxChaseTime = publishTime.let { d -> LocalDate.of(d.year, (d.monthValue - 1) / 3 * 3 + 1, 1).plusMonths(3) }

                    Row(publishTime, score, publishType == PublishType.TV_AND_WEB && startTime != null && startTime < maxChaseTime)
                }
                .groupBy { Pair(it.publishTime.year, (it.publishTime.monthValue - 1) / 3 + 1) }
                .mapValues { (pair, items) ->
                    val scoredItems = items.filter { it.score != null }.map { it.score!! }
                    val maxScore = scoredItems.max()
                    val minScore = scoredItems.min()
                    val sumScore = scoredItems.sum()
                    val scoreCounts = scoredItems.groupingBy { it }.eachCount()
                    HistoryLineModal.Item(pair.first, pair.second,
                            items.filter { it.chase }.count(), items.filter { !it.chase }.count(),
                            scoredItems.size, maxScore, minScore, sumScore, scoreCounts)
                }

        val (beginYear, beginSeason) = rowSets.keys.minBy { compValue(it.first, it.second) } ?: Pair(null, null)
        val (endYear, endSeason) = rowSets.keys.maxBy { compValue(it.first, it.second) } ?: Pair(null, null)
        if(beginYear != null && beginSeason != null && endYear != null && endSeason != null) {
            val items = stepFor(compValue(beginYear, beginSeason), compValue(endYear, endSeason)) { it + 1 }.asSequence()
                    .map { Pair(it / 4, it % 4 + 1) }
                    .map { rowSets[it] ?: HistoryLineModal.Item(it.first, it.second, 0, 0, 0, null, null, 0, emptyMap()) }
                    .toList()

            return HistoryLineModal(beginYear, beginSeason, endYear, endSeason, items)
        }
        return HistoryLineModal(null, null, null, null, emptyList())
    }
}

/**
 * 将年和季度转换为更容易比较的值。
 */
private fun compValue(year: Int, season: Int): Int {
    return season + year * 4 - 1
}
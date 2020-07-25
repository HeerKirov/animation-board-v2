package com.heerkirov.animation.service.statistics

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Comments
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.dao.Statistics
import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.enums.StatisticType
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.HistoryLineModal
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.HistoryLineRes
import com.heerkirov.animation.model.result.SeasonOverviewRes
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.parseJSONObject
import com.heerkirov.animation.util.toDateTimeString
import com.heerkirov.animation.util.toJSONString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.RuntimeException
import java.time.LocalDate

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
                .map { Pair(if(aggregateTimeUnit == AggregateTimeUnit.SEASON) { "${it.second.year}-${it.second.season}" }else{ it.second.year.toString() }, it.second) }
                .groupBy({ it.first }) { it.second }
                .map { (time, items) ->
                    val totalAnimations = items.sumBy { it.totalAnimations }
                    val scoredAnimations = items.sumBy { it.scoredAnimations }
                    val maxScore = items.asSequence().map { it.maxScore }.filterNotNull().max()
                    val minScore = items.asSequence().map { it.minScore }.filterNotNull().min()
                    val sumScore = items.asSequence().map { it.sumScore }.sum()
                    val avgScore = if(scoredAnimations == 0) null else { sumScore * 1.0 / scoredAnimations }
                    HistoryLineRes.Item(time, totalAnimations, maxScore, minScore, avgScore)
                }
                .sortedBy { it.time }
                .toList()

        return HistoryLineRes(items, updateTime.toDateTimeString())
    }

    fun update(user: User) {
        val modal = generate(user)

        val id = database.from(Statistics).select()
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
        data class Row(val publishTime: LocalDate, val score: Int?)

        val rowSets = database.from(Animations)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .leftJoin(Comments, (Comments.animationId eq Animations.id) and (Comments.score.isNotNull()))
                .select(Animations.publishTime, Comments.score)
                .where { (Animations.publishTime.isNotNull()) }
                .asSequence()
                .map { Row(it[Animations.publishTime]!!, it[Comments.score]) }
                .groupBy { Pair(it.publishTime.year, (it.publishTime.monthValue - 1) / 3 + 1) }
                .mapValues { (pair, items) ->
                    val scoredItems = items.filter { it.score != null }.map { it.score!! }
                    val maxScore = scoredItems.max()
                    val minScore = scoredItems.min()
                    val sumScore = scoredItems.sum()
                    HistoryLineModal.Item(pair.first, pair.second, items.size, scoredItems.size, maxScore, minScore, sumScore)
                }
                .toSortedMap(Comparator { o1, o2 -> (compValue(o1.first, o1.second)).compareTo(compValue(o2.first, o2.second)) })
                .values
                .toList()

        return HistoryLineModal(rowSets.firstOrNull()?.year, rowSets.firstOrNull()?.season, rowSets.lastOrNull()?.year, rowSets.lastOrNull()?.season, rowSets)
    }

    /**
     * 将年和季度转换为更容易比较的值。
     */
    private fun compValue(year: Int, season: Int): Int {
        return season + year * 4
    }
}
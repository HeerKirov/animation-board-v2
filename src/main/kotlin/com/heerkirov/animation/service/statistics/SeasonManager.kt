package com.heerkirov.animation.service.statistics

import com.heerkirov.animation.dao.*
import com.heerkirov.animation.enums.PublishType
import com.heerkirov.animation.enums.SexLimitLevel
import com.heerkirov.animation.enums.StatisticType
import com.heerkirov.animation.enums.ViolenceLimitLevel
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.SeasonModal
import com.heerkirov.animation.model.data.SeasonOverviewModal
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.SeasonLineRes
import com.heerkirov.animation.model.result.SeasonOverviewRes
import com.heerkirov.animation.model.result.SeasonRes
import com.heerkirov.animation.model.result.toResWith
import com.heerkirov.animation.util.*
import com.heerkirov.animation.util.ktorm.dsl.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.*

@Component
class SeasonManager(@Autowired private val database: Database) {
    fun getOverview(user: User): SeasonOverviewRes {
        return database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.SEASON_OVERVIEW) }
                .firstOrNull()
                ?.let { it[Statistics.content]!!.parseJSONObject<SeasonOverviewModal>().toResWith(it[Statistics.updateTime]!!) }
                ?: SeasonOverviewRes(null, null, null, null, null)
    }

    fun getSeasonLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int): SeasonLineRes {
        val items = database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.SEASON) and (Statistics.key greaterEq "$lowerYear-$lowerSeason") and (Statistics.key lessEq "$upperYear-$upperSeason") }
                .orderBy(Statistics.key.asc())
                .map {
                    val split = it[Statistics.key]!!.split('-')
                    val year = split[0].toInt()
                    val season = split[1].toInt()
                    val content = it[Statistics.content]!!.parseJSONObject<SeasonModal>().toResWith(it[Statistics.updateTime]!!)
                    val updateTime = it[Statistics.updateTime]!!

                    val item = SeasonLineRes.Item(year, season, content.totalAnimations, content.maxScore, content.minScore, content.avgScore, content.avgPositivity)
                    Pair(item, updateTime)
                }
        val updateTime = items.asSequence().map { it.second }.max()?.toDateTimeString()

        return SeasonLineRes(items.map { it.first }, updateTime)
    }

    fun get(user: User, year: Int, season: Int): SeasonRes {
        return database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.SEASON) and (Statistics.key eq "$year-$season") }
                .firstOrNull()
                ?.let { it[Statistics.content]!!.parseJSONObject<SeasonModal>().toResWith(it[Statistics.updateTime]!!) }
                ?: throw NotFoundException("Statistic not found.")
    }

    fun update(user: User, year: Int, season: Int, forceGenerate: Boolean = false): Boolean {
        val modal = generate(user, year, season, forceGenerate) ?: return false
        val key = "$year-$season"
        val id = database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.SEASON) and (Statistics.key eq key) }
                .firstOrNull()?.get(Statistics.id)
        if(id == null) {
            database.insert(Statistics) {
                it.ownerId to user.id
                it.type to StatisticType.SEASON
                it.key to key
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
        return true
    }

    fun updateOverview(user: User) {
        val overview = database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.SEASON_OVERVIEW) }
                .firstOrNull()
                ?.let { Pair(it[Statistics.id]!!, it[Statistics.content]!!.parseJSONObject<SeasonOverviewModal>()) }

        val bound = database.from(Statistics).select(Statistics.key)
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.SEASON) }
                .map {
                    val split = it[Statistics.key]!!.split('-')
                    Pair(split[0].toInt(), split[1].toInt())
                }.let { data ->
                    if(data.isNotEmpty()) {
                        val min = data.minBy { compValue(it.first, it.second) }!!
                        val max = data.maxBy { compValue(it.first, it.second) }!!
                        SeasonOverviewModal(min.first, min.second, max.first, max.second)
                    }else{
                        SeasonOverviewModal(null, null, null, null)
                    }
                }

        if (overview == null) database.insert(Statistics) {
            it.ownerId to user.id
            it.type to StatisticType.SEASON_OVERVIEW
            it.key to null
            it.content to bound.toJSONString()
            it.updateTime to DateTimeUtil.now()
        } else database.update(Statistics) {
            where { it.id eq overview.first }
            it.content to SeasonOverviewModal(bound.beginYear, bound.beginSeason, bound.endYear, bound.endSeason).toJSONString()
            it.updateTime to DateTimeUtil.now()
        }
    }

    fun updateOverview(user: User, year: Int, season: Int) {
        val overview = database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.SEASON_OVERVIEW) }
                .firstOrNull()
                ?.let { Pair(it[Statistics.id]!!, it[Statistics.content]!!.parseJSONObject<SeasonOverviewModal>()) }

        when {
            overview == null -> database.insert(Statistics) {
                it.ownerId to user.id
                it.type to StatisticType.SEASON_OVERVIEW
                it.key to null
                it.content to SeasonOverviewModal(year, season, year, season).toJSONString()
                it.updateTime to DateTimeUtil.now()
            }
            overview.second.beginYear == null || overview.second.beginSeason == null ||
                    compValue(year, season) < compValue(overview.second.beginYear!!, overview.second.beginSeason!!) -> database.update(Statistics) {
                where { it.id eq overview.first }
                it.content to SeasonOverviewModal(year, season, overview.second.endYear, overview.second.endSeason).toJSONString()
                it.updateTime to DateTimeUtil.now()
            }
            overview.second.endYear == null || overview.second.endSeason == null ||
                    compValue(year, season) > compValue(overview.second.endYear!!, overview.second.endSeason!!) -> database.update(Statistics) {
                where { it.id eq overview.first }
                it.content to SeasonOverviewModal(overview.second.beginYear, overview.second.beginSeason, year, season).toJSONString()
                it.updateTime to DateTimeUtil.now()
            }
        }
    }

    private fun generate(user: User, year: Int, season: Int, forceGenerate: Boolean = false): SeasonModal? {
        data class Row(val id: Int, val title: String, val cover: String?,
                       val sexLimitLevel: SexLimitLevel?, val violenceLimitLevel: ViolenceLimitLevel?, val score: Int?,
                       val startTime: LocalDateTime, val finishTime: LocalDateTime?,
                       val publishedRecord: List<LocalDateTime?>, val watchedRecord: List<LocalDateTime?>)

        val zone = ZoneId.of(user.setting.timezone)

        val beginDate = LocalDate.of(year, (season - 1) * 3 + 1, 1)
        val endDate = beginDate.plusMonths(3)
        //由于数据库中的时间存的是UTC时间，因此需要把当前时区的end时间点转换为UTC时区的，得到的才是基于用户时区的整点数据
        val endUTCTime = ZonedDateTime.of(endDate, LocalTime.MIN, zone).asUTCTime()
        //查出符合"用户有record的"、"至少存在1进度的"、"放送类型为TV&WEB的"、"发布时间在本季3个月内的"、"用户订阅时间在本季三个月或之前的"的全部动画
        val rowSets = database.from(Animations)
                .innerJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id) and (Records.progressCount greater 0))
                .innerJoin(RecordProgresses, (RecordProgresses.ordinal eq 1) and (RecordProgresses.recordId eq Records.id) and (RecordProgresses.startTime.isNotNull()) and (RecordProgresses.startTime less endUTCTime))
                .leftJoin(Comments, (Comments.animationId eq Animations.id) and (Comments.ownerId eq user.id) and (Comments.score.isNotNull()))
                .select(Animations.id, Animations.title, Animations.cover, Animations.sexLimitLevel, Animations.violenceLimitLevel, Animations.publishedRecord,
                        Comments.score, RecordProgresses.startTime, RecordProgresses.finishTime, RecordProgresses.watchedRecord)
                .where { (Animations.publishType eq PublishType.TV_AND_WEB) and (Animations.publishTime greaterEq beginDate) and (Animations.publishTime less endDate) }
                .map { Row(it[Animations.id]!!, it[Animations.title]!!, it[Animations.cover], it[Animations.sexLimitLevel], it[Animations.violenceLimitLevel], it[Comments.score],
                        it[RecordProgresses.startTime]!!, it[RecordProgresses.finishTime], it[Animations.publishedRecord]!!, it[RecordProgresses.watchedRecord]!!) }

        if(rowSets.isEmpty() && !forceGenerate) return null

        //导出两项维度上的动画数量
        val sexLimitLevelCounts = rowSets.asSequence()
                .filter { it.sexLimitLevel != null }
                .groupBy { it.sexLimitLevel!! }
                .mapValues { it.value.size }
                .toSortedMap()
        val violenceLimitLevelCounts = rowSets.asSequence()
                .filter { it.violenceLimitLevel != null }
                .groupBy { it.violenceLimitLevel!! }
                .mapValues { it.value.size }
                .toSortedMap()
        //根据同样的条件，在标签数量上做聚合
        val tagCounts = database.from(Tags)
                .innerJoin(AnimationTagRelations, AnimationTagRelations.tagId eq Tags.id)
                .innerJoin(Animations, Animations.id eq AnimationTagRelations.animationId)
                .innerJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id) and (Records.progressCount greater 0))
                .innerJoin(RecordProgresses, (RecordProgresses.ordinal eq 1) and (RecordProgresses.recordId eq Records.id) and (RecordProgresses.startTime.isNotNull()) and (RecordProgresses.startTime less endUTCTime))
                .select(Tags.name, count(Animations.id))
                .where { (Animations.publishType eq PublishType.TV_AND_WEB) and (Animations.publishTime greaterEq beginDate) and (Animations.publishTime less endDate) }
                .groupBy(Tags.name)
                .asSequence()
                .map { Pair(it[Tags.name]!!, it.getInt(2)) }
                .toMap()

        //导出animation详情列表
        val animations = rowSets.map {
            SeasonModal.Animation(it.id, it.title, it.cover,
                    it.sexLimitLevel, it.violenceLimitLevel,
                    it.startTime.toDateTimeString(), it.finishTime?.toDateTimeString(), it.score,
                    calculatePositivity(it.publishedRecord, it.watchedRecord, it.startTime))
        }

        //计算全部动画的最高分、最低分、平均分
        val maxScore = rowSets.asSequence().map { it.score }.filterNotNull().max()
        val minScore = rowSets.asSequence().map { it.score }.filterNotNull().min()
        val avgScore = rowSets.asSequence().map { it.score }.filterNotNull().average().let { if(it.isNaN()) null else it }
        //计算全部动画的平均及时度
        val avgPositivity = animations.asSequence().map { it.positivity }.filterNotNull().average().let { if(it.isNaN()) null else it }

        return SeasonModal(rowSets.size, maxScore, minScore, avgScore, avgPositivity, sexLimitLevelCounts, violenceLimitLevelCounts, tagCounts, animations)
    }
}

/**
 * 计算一部动画的及时度。
 * 一部动画的及时度，为其全部集数，排除不可计算集数(如没有发布时间或观看时间记录)，的及时度，去掉最高最低，的平均分。
 * 如果可计算集数不超过3，那么直接视作不可计算。
 * 集数的发布时间从publishedRecord取得。但是，当存在一些集数的发布时间早于用户的订阅时间subscriptionTime时，这些集数从第1集开始，替换为订阅时间，并且后续每集顺延1天。
 */
private fun calculatePositivity(publishedRecord: List<LocalDateTime?>, watchedRecord: List<LocalDateTime?>, subscriptionTime: LocalDateTime): Double? {
    val size = minOf(publishedRecord.size, watchedRecord.size)
    val positivity = ArrayList<Double>(size)
    for(i in 0 until size) {
        val publishedTime = publishedRecord[i]
        val watchedTime = watchedRecord[i]
        if(publishedTime != null && watchedTime != null) {
            val realPublishedTime = if(publishedTime >= subscriptionTime) publishedTime else subscriptionTime.plusDays(i.toLong())
            positivity.add(calculatePositivityOfEpisode(realPublishedTime, watchedTime))
        }
    }
    if(positivity.size < 3) return null

    return positivity.sorted().subList(1, positivity.size - 1).average()
}

/**
 * 计算某单集的及时度。
 * 这一项计算更为简单，根据时间差导出预定分数。
 */
private fun calculatePositivityOfEpisode(publishedTime: LocalDateTime, watchedTime: LocalDateTime): Double {
    val t = Duration.between(publishedTime, watchedTime).toHours()
    return when {
        t <  2      -> 5.0
        t <  8      -> 4.5
        t < 24      -> 4.0
        t < 36      -> 3.5
        t < 24 *  2 -> 3.0
        t < 24 *  3 -> 2.5
        t < 24 *  7 -> 2.0
        t < 24 * 14 -> 1.5
        t < 24 * 30 -> 1.0
        t < 24 * 60 -> 0.5
        else        -> 0.0
    }
}

/**
 * 将年和季度转换为更容易比较的值。
 */
private fun compValue(year: Int, season: Int): Int {
    return season + year * 4
}
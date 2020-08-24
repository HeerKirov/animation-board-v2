package com.heerkirov.animation.service.statistics

import com.heerkirov.animation.dao.*
import com.heerkirov.animation.enums.*
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.OverviewModal
import com.heerkirov.animation.model.data.ScatterRecord
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.OverviewRes
import com.heerkirov.animation.model.result.toResWith
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.parseJSONObject
import com.heerkirov.animation.util.toJSONString
import com.heerkirov.animation.util.ktorm.dsl.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OverviewManager(@Autowired private val database: Database) {
    fun get(user: User): OverviewRes {
        return database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.OVERVIEW) }
                .firstOrNull()
                ?.let { it[Statistics.content]!!.parseJSONObject<OverviewModal>().toResWith(it[Statistics.updateTime]!!) }
                ?: throw NotFoundException("Statistic not found.")
    }

    fun update(user: User) {
        val modal = generate(user)
        val id = database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.OVERVIEW) }
                .firstOrNull()?.get(Statistics.id)
        if(id == null) {
            database.insert(Statistics) {
                it.ownerId to user.id
                it.type to StatisticType.OVERVIEW
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

    private fun generate(user: User): OverviewModal {
        data class Row(val episodeDuration: Int?, val totalEpisodes: Int,
                       val originalWorkType: OriginalWorkType?, val publishType: PublishType?, val sexLimitLevel: SexLimitLevel?, val violenceLimitLevel: ViolenceLimitLevel?,
                       val scatterRecord: List<ScatterRecord>, val progressCount: Int,
                       val watchedEpisodes: Int?, val score: Int?)

        //查找出用户有record的全部animation
        val rowSets = database.from(Animations)
                .innerJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id))
                .leftJoin(RecordProgresses, (RecordProgresses.ordinal eq Records.progressCount) and (RecordProgresses.recordId eq Records.id))
                .leftJoin(Comments, (Comments.animationId eq Animations.id) and (Comments.ownerId eq user.id))
                .select(Animations.episodeDuration, Animations.totalEpisodes,
                        Animations.originalWorkType, Animations.publishType, Animations.sexLimitLevel, Animations.violenceLimitLevel,
                        Records.scatterRecord, Records.progressCount,
                        RecordProgresses.watchedEpisodes, Comments.score)
                .map { Row(it[Animations.episodeDuration], it[Animations.totalEpisodes]!!,
                        it[Animations.originalWorkType], it[Animations.publishType], it[Animations.sexLimitLevel], it[Animations.violenceLimitLevel],
                        it[Records.scatterRecord]!!, it[Records.progressCount]!!,
                        it[RecordProgresses.watchedEpisodes], it[Comments.score]) }

        //计算至少有1离散记录或存在至少1观看数的动画总数
        val totalAnimations = rowSets.asSequence()
                .filter { (it.watchedEpisodes != null && it.watchedEpisodes > 0) || it.scatterRecord.isNotEmpty() }
                .count()
        //计算动画的平均时长，以及动画的总共观看集数
        val totalEpisodeMap = rowSets.map {
            val progressEpisodes = if(it.watchedEpisodes == null) { 0 }else{
                (it.progressCount - 1) * it.totalEpisodes + it.watchedEpisodes
            }
            Pair(it.episodeDuration, it.scatterRecord.size + progressEpisodes)
        }
        //对总集数求和
        val totalEpisodes = totalEpisodeMap.asSequence().map { it.second }.sum()
        //对总集数和平均时长的乘积求和
        val totalDuration = totalEpisodeMap.asSequence().map { (it.first ?: 0) * it.second }.sum()
        //求总平均分
        val avgScore = rowSets.asSequence().map { it.score }.filterNotNull().average().let { if(it.isNaN()) null else it }

        //从列表导出各种维度的动画数量
        val scoreCounts = rowSets.asSequence()
                .filter { it.score != null }
                .groupBy { it.score!! }
                .mapValues { it.value.size }
                .toSortedMap()
        val originalWorkTypeCounts = rowSets.asSequence()
                .filter { it.originalWorkType != null }
                .groupBy { it.originalWorkType!! }
                .mapValues { it.value.size }
                .toSortedMap()
        val publishTypeCounts = rowSets.asSequence()
                .filter { it.publishType != null }
                .groupBy { it.publishType!! }
                .mapValues { it.value.size }
                .toSortedMap()
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
        val tagCounts = database.from(Tags)
                .innerJoin(AnimationTagRelations, AnimationTagRelations.tagId eq Tags.id)
                .innerJoin(Animations, Animations.id eq AnimationTagRelations.animationId)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .select(Tags.name, count(Animations.id))
                .groupBy(Tags.id)
                .orderBy(count(Animations.id).desc())
                .asSequence()
                .map { Pair(it[Tags.name]!!, it.getInt(2)) }
                .toMap()
        //从列表导出各种维度的动画平均分
        val sexLimitLevelAvgScores = rowSets.asSequence()
                .filter { it.sexLimitLevel != null && it.score != null }
                .groupBy { it.sexLimitLevel!! }
                .mapValues { it.value.sumByDouble { row -> row.score!!.toDouble() } / it.value.size }
                .toSortedMap()
        val violenceLimitLevelAvgScores = rowSets.asSequence()
                .filter { it.violenceLimitLevel != null && it.score != null }
                .groupBy { it.violenceLimitLevel!! }
                .mapValues { it.value.sumByDouble { row -> row.score!!.toDouble() } / it.value.size }
                .toSortedMap()
        //重新查找数据库并计算标签维度的动画平均分
        val tagAvgScores = database.from(Tags)
                .innerJoin(AnimationTagRelations, AnimationTagRelations.tagId eq Tags.id)
                .innerJoin(Animations, Animations.id eq AnimationTagRelations.animationId)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .innerJoin(Comments, (Comments.animationId eq Animations.id) and (Comments.score.isNotNull()))
                .select(Tags.name, avg(Comments.score))
                .groupBy(Tags.id)
                .orderBy(avg(Comments.score).desc())
                .asSequence()
                .map { Pair(it[Tags.name]!!, it.getDouble(2)) }
                .toMap()

        return OverviewModal(totalAnimations, totalEpisodes, totalDuration, avgScore,
                scoreCounts, originalWorkTypeCounts, publishTypeCounts,
                sexLimitLevelCounts, violenceLimitLevelCounts, tagCounts,
                sexLimitLevelAvgScores, violenceLimitLevelAvgScores, tagAvgScores)
    }
}
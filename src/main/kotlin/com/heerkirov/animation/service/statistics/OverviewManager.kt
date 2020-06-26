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
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

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

        val rowSets = database.from(Animations)
                .innerJoin(Records, (Animations.id eq Records.animationId) and (Records.ownerId eq user.id))
                .leftJoin(RecordProgresses, (RecordProgresses.ordinal eq Records.progressCount) and (RecordProgresses.recordId eq Records.id))
                .leftJoin(Comments, Comments.animationId eq Animations.id)
                .select(Animations.episodeDuration, Animations.totalEpisodes,
                        Animations.originalWorkType, Animations.publishType, Animations.sexLimitLevel, Animations.violenceLimitLevel,
                        Records.scatterRecord, Records.progressCount,
                        RecordProgresses.watchedEpisodes, Comments.score)
                .map { Row(it[Animations.episodeDuration], it[Animations.totalEpisodes]!!,
                        it[Animations.originalWorkType], it[Animations.publishType], it[Animations.sexLimitLevel], it[Animations.violenceLimitLevel],
                        it[Records.scatterRecord]!!, it[Records.progressCount]!!,
                        it[RecordProgresses.watchedEpisodes], it[Comments.score]) }

        val totalAnimations = rowSets.asSequence()
                .filter { (it.watchedEpisodes != null && it.watchedEpisodes > 0) || it.scatterRecord.isNotEmpty() }
                .count()
        val totalEpisodeMap = rowSets.map {
            val progressEpisodes = if(it.watchedEpisodes == null) { 0 }else{
                (it.progressCount - 1) * it.totalEpisodes + it.watchedEpisodes
            }
            Pair(it.episodeDuration, it.scatterRecord.size + progressEpisodes)
        }
        val totalEpisodes = totalEpisodeMap.asSequence().map { it.second }.sum()
        val totalDuration = totalEpisodeMap.asSequence().map { (it.first ?: 0) * it.second }.sum()

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

        return OverviewModal(totalAnimations, totalEpisodes, totalDuration, scoreCounts, originalWorkTypeCounts, publishTypeCounts, sexLimitLevelCounts, violenceLimitLevelCounts, tagCounts)
    }
}
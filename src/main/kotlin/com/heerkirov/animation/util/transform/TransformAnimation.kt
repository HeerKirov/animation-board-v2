package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.enums.*
import com.heerkirov.animation.service.manager.AnimationRelationProcessor
import com.heerkirov.animation.util.logger
import com.heerkirov.animation.util.map
import com.heerkirov.animation.util.parseJsonNode
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.batchUpdate
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.insertAndGenerateKey
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.sortedBy
import me.liuwj.ktorm.entity.toList

class TransformAnimation(private val userLoader: UserLoader,
                         private val v1Database: Database,
                         private val database: Database) {
    private val log = logger<TransformAnimation>()

    fun transform(): Map<Long, Int> {
        val idMap = HashMap<Long, Int>()

        val v1Animations = v1Database.sequenceOf(V1Animations).sortedBy { it.id }.toList()
        for (v1Animation in v1Animations) {
            val id = database.insertAndGenerateKey(Animations) {
                it.title to v1Animation.title
                it.originTitle to v1Animation.originTitle
                it.otherTitle to v1Animation.otherTitle
                it.cover to v1Animation.cover

                it.introduction to v1Animation.introduction
                it.keyword to v1Animation.keyword
                it.originalWorkType to v1Animation.originalWorkType?.convertOriginalWorkType()
                it.sexLimitLevel to v1Animation.limitLevel?.convertSexLimitLevel()
                it.violenceLimitLevel to v1Animation.limitLevel?.convertViolenceLimitLevel()
                it.publishType to v1Animation.publishType?.convertPublishType()
                it.publishTime to v1Animation.publishTime
                it.episodeDuration to v1Animation.duration
                it.totalEpisodes to (v1Animation.sumQuantity ?: 1)
                it.publishedEpisodes to (v1Animation.publishedQuantity ?: 0)
                it.publishedRecord to v1Animation.publishedRecord.map { t -> t?.toV2Time() }
                it.publishPlan to v1Animation.publishPlan.filterNotNull().map { t -> t.toV2Time() }

                it.relations to emptyMap()
                it.relationsTopology to emptyMap()

                it.createTime to v1Animation.createTime.toV2Time()
                it.updateTime to (v1Animation.updateTime?.toV2Time() ?: v1Animation.createTime.toV2Time())
                it.creator to userLoader[v1Animation.creator].id
                it.updater to userLoader[v1Animation.updater ?: v1Animation.creator].id
            } as Int
            idMap[v1Animation.id] = id
        }
        log.info("Transform ${idMap.size} animations from v1.")

        database.batchUpdate(Animations) {
            for (v1Animation in v1Animations) {
                item {
                    where { it.id eq idMap[v1Animation.id]!! }
                    it.relations to v1Animation.relations.convertRelations(idMap)
                }
            }
        }
        val num = AnimationRelationProcessor(database).updateAllRelationTopology()
        log.info("Update $num animations' topology.")

        return idMap
    }

    private fun String.convertOriginalWorkType(): OriginalWorkType {
        return when(this) {
            "NOVEL" -> OriginalWorkType.NOVEL
            "MANGA" -> OriginalWorkType.MANGA
            "GAME" -> OriginalWorkType.GAME
            "ORI" -> OriginalWorkType.ORIGINAL
            "OTHER" -> OriginalWorkType.OTHER
            else -> throw NoSuchElementException("No such original work type '$this'.")
        }
    }

    private fun String.convertPublishType(): PublishType {
        return when(this) {
            "GENERAL" -> PublishType.TV_AND_WEB
            "MOVIE" -> PublishType.MOVIE
            "OVA" -> PublishType.OVA_AND_OAD
            else -> throw NoSuchElementException("No such publish type '$this'.")
        }
    }

    private fun String.convertSexLimitLevel(): SexLimitLevel {
        return when(this) {
            "ALL" -> SexLimitLevel.ALL
            "R12" -> SexLimitLevel.R12
            "R15" -> SexLimitLevel.R15
            "R17" -> SexLimitLevel.R17
            "R18" -> SexLimitLevel.R18
            "R18G" -> SexLimitLevel.R17
            else -> throw NoSuchElementException("No such limit level '$this'.")
        }
    }

    private fun String.convertViolenceLimitLevel(): ViolenceLimitLevel {
        return when(this) {
            "ALL" -> ViolenceLimitLevel.NO
            "R12" -> ViolenceLimitLevel.NORMAL
            "R15" -> ViolenceLimitLevel.MILD
            "R17" -> ViolenceLimitLevel.SEVERE
            "R18" -> ViolenceLimitLevel.SEVERE
            "R18G" -> ViolenceLimitLevel.RESTRICTED
            else -> throw NoSuchElementException("No such limit level '$this'.")
        }
    }

    private fun String.convertRelations(animationIdMap: Map<Long, Int>): Map<RelationType, List<Int>> {
        return this.parseJsonNode().fields().map { (r, list) ->
            Pair(when(r) {
                "PREV" -> RelationType.PREV
                "NEXT" -> RelationType.NEXT
                "UNOFFICIAL" -> RelationType.FANWAI
                "OFFICIAL" -> RelationType.MAIN_ARTICLE
                "EXTERNAL" -> RelationType.RUMOR
                "TRUE" -> RelationType.TRUE_PASS
                "SERIES" -> RelationType.SERIES
                else -> throw NoSuchElementException("No such relation type '$r'.")
            }, list.map { animationIdMap[it["id"].asLong()] ?: throw NoSuchElementException() })
        }.toMap()
    }
}

package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.enums.*
import com.heerkirov.animation.service.manager.AnimationRelationProcessor
import com.heerkirov.animation.util.logger
import com.heerkirov.animation.util.map
import com.heerkirov.animation.util.parseJsonNode
import org.ktorm.database.Database
import org.ktorm.dsl.batchUpdate
import org.ktorm.dsl.eq
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList

class TransformAnimation(private val userLoader: UserLoader,
                         private val v1Database: Database,
                         private val database: Database) {
    private val log = logger<TransformAnimation>()

    fun transform(): Map<Long, Int> {
        val idMap = HashMap<Long, Int>()

        val v1Animations = v1Database.sequenceOf(V1Animations).sortedBy { it.id }.toList()
        for (v1Animation in v1Animations) {
            val id = database.insertAndGenerateKey(Animations) {
                set(it.title, v1Animation.title)
                set(it.originTitle, v1Animation.originTitle)
                set(it.otherTitle, v1Animation.otherTitle)
                set(it.cover, v1Animation.cover)

                set(it.introduction, v1Animation.introduction)
                set(it.keyword, v1Animation.keyword)
                set(it.originalWorkType, v1Animation.originalWorkType?.convertOriginalWorkType())
                set(it.sexLimitLevel, v1Animation.limitLevel?.convertSexLimitLevel())
                set(it.violenceLimitLevel, v1Animation.limitLevel?.convertViolenceLimitLevel())
                set(it.publishType, v1Animation.publishType?.convertPublishType())
                set(it.publishTime, v1Animation.publishTime)
                set(it.episodeDuration, v1Animation.duration)
                set(it.totalEpisodes, v1Animation.sumQuantity ?: 1)
                set(it.publishedEpisodes, v1Animation.publishedQuantity ?: 0)
                set(it.publishedRecord, v1Animation.publishedRecord.map { t -> t?.toV2Time() })
                set(it.publishPlan, v1Animation.publishPlan.filterNotNull().map { t -> t.toV2Time() })

                set(it.relations, emptyMap())
                set(it.relationsTopology, emptyMap())

                set(it.createTime, v1Animation.createTime.toV2Time())
                set(it.updateTime, v1Animation.updateTime?.toV2Time() ?: v1Animation.createTime.toV2Time())
                set(it.creator, userLoader[v1Animation.creator].id)
                set(it.updater, userLoader[v1Animation.updater ?: v1Animation.creator].id)
            } as Int
            idMap[v1Animation.id] = id
        }
        log.info("Transform ${idMap.size} animations from v1.")

        database.batchUpdate(Animations) {
            for (v1Animation in v1Animations) {
                item {
                    where { it.id eq idMap[v1Animation.id]!! }
                    set(it.relations, v1Animation.relations.convertRelations(idMap))
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
            "R12" -> ViolenceLimitLevel.A
            "R15" -> ViolenceLimitLevel.B
            "R17" -> ViolenceLimitLevel.C
            "R18" -> ViolenceLimitLevel.C
            "R18G" -> ViolenceLimitLevel.D
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

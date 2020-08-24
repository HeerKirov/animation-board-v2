package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.OriginalWorkType
import com.heerkirov.animation.enums.PublishType
import com.heerkirov.animation.enums.SexLimitLevel
import com.heerkirov.animation.enums.ViolenceLimitLevel
import com.heerkirov.animation.model.data.Animation
import com.heerkirov.animation.util.ktorm.*
import com.heerkirov.animation.util.ktorm.enum
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Animations : BaseTable<Animation>("animation") {
    val id = int("id").primaryKey()
    val title = varchar("title")
    val originTitle = varchar("origin_title")
    val otherTitle = varchar("other_title")
    val cover = varchar("cover")

    val publishType = enum("publish_type", typeRef<PublishType>())
    val publishTime = date("publish_time")
    val episodeDuration = int("episode_duration")
    val totalEpisodes = int("total_episodes")
    val publishedEpisodes = int("published_episodes")
    val publishedRecord = json("published_record", NullableListConverter(DateTimeConverter()))
    val publishPlan = json("publish_plan", ListConverter(DateTimeConverter()))

    val introduction = text("introduction")
    val keyword = varchar("keyword")
    val sexLimitLevel = enum("sex_limit_level", typeRef<SexLimitLevel>())
    val violenceLimitLevel = enum("violence_limit_level", typeRef<ViolenceLimitLevel>())
    val originalWorkType = enum("original_work_type", typeRef<OriginalWorkType>())

    val relations = json("relations", RelationConverter())
    val relationsTopology = json("relations_topology", RelationConverter())

    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")
    val creator = int("creator")
    val updater = int("updater")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Animation(
            id = row[id]!!,
            title = row[title]!!,
            originTitle = row[originTitle],
            otherTitle = row[otherTitle],
            cover = row[cover],
            publishType = row[publishType],
            publishTime = row[publishTime],
            episodeDuration = row[episodeDuration],
            totalEpisodes = row[totalEpisodes]!!,
            publishedEpisodes = row[publishedEpisodes]!!,
            publishedRecord = row[publishedRecord]!!,
            publishPlan = row[publishPlan]!!,
            introduction = row[introduction],
            keyword = row[keyword],
            sexLimitLevel = row[sexLimitLevel],
            violenceLimitLevel = row[violenceLimitLevel],
            originalWorkType = row[originalWorkType],
            relations = row[relations]!!,
            relationsTopology = row[relationsTopology]!!,
            createTime = row[createTime]!!,
            updateTime = row[updateTime]!!,
            creator = row[creator]!!,
            updater = row[updater]!!
    )
}
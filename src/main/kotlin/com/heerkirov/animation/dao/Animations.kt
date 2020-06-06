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
    val id by int("id").primaryKey()
    val title by varchar("title")
    val originTitle by varchar("origin_title")
    val otherTitle by varchar("other_title")
    val cover by varchar("cover")

    val publishType by enum("publish_type", typeRef<PublishType>())
    val publishTime by date("publish_time")
    val episodeDuration by int("episode_duration")
    val totalEpisodes by int("total_episodes")
    val publishedEpisodes by int("published_episodes")
    val publishedRecord by json("published_record", NullableListConverter(DateTimeConverter()))
    val publishPlan by json("publish_plan", ListConverter(DateTimeConverter()))

    val introduction by text("introduction")
    val keyword by varchar("keyword")
    val sexLimitLevel by enum("sex_limit_level", typeRef<SexLimitLevel>())
    val violenceLimitLevel by enum("violence_limit_level", typeRef<ViolenceLimitLevel>())
    val originalWorkType by enum("original_work_type", typeRef<OriginalWorkType>())

    val relations by json("relations", RelationConverter())
    val relationsTopology by json("relations_topology", RelationConverter())

    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")
    val creator by int("creator")
    val updater by int("updater")

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
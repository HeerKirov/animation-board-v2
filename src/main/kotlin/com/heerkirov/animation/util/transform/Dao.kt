package com.heerkirov.animation.util.transform

import com.heerkirov.animation.util.ktorm.array
import com.heerkirov.animation.util.ktorm.jsonString
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*


object V1Animations : BaseTable<V1Animation>("api_animation") {
    val id by long("id").primaryKey()
    val title by varchar("title")
    val originTitle by varchar("origin_title")
    val otherTitle by varchar("other_title")
    val cover by varchar("cover")
    val originalWorkType by varchar("original_work_type")
    val publishType by varchar("publish_type")
    val publishTime by date("publish_time")
    val sumQuantity by int("sum_quantity")
    val publishedQuantity by int("published_quantity")
    val duration by int("duration")
    val publishPlan by array("publish_plan", V1TimestampStrConverter())
    val publishedRecord by array("published_record", V1TimestampStrConverter())
    val introduction by varchar("introduction")
    val keyword by varchar("keyword")
    val limitLevel by varchar("limit_level")
    val relations by jsonString("original_relations")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")
    val creator by varchar("creator")
    val updater by varchar("updater")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1Animation(
            id = row[id]!!,
            title = row[title]!!,
            originTitle = row[originTitle],
            otherTitle = row[otherTitle],
            cover = row[cover],
            originalWorkType = row[originalWorkType],
            publishType = row[publishType],
            publishTime = row[publishTime],
            sumQuantity = row[sumQuantity],
            publishedQuantity = row[publishedQuantity],
            duration = row[duration],
            publishPlan = row[publishPlan]!!,
            publishedRecord = row[publishedRecord]!!,
            introduction = row[introduction],
            keyword = row[keyword],
            limitLevel = row[limitLevel],
            relations = row[relations]!!,
            createTime = row[createTime]!!,
            updateTime = row[updateTime],
            creator = row[creator]!!,
            updater = row[updater]
    )
}

object V1Staffs : BaseTable<V1Staff>("api_staff") {
    val id by long("id").primaryKey()
    val name by varchar("name")
    val originName by varchar("origin_name")
    val remark by varchar("remark")
    val isOrganization by boolean("is_organization")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")
    val creator by varchar("creator")
    val updater by varchar("updater")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1Staff(
            id = row[id]!!,
            name = row[name]!!,
            originName = row[originName],
            remark = row[remark],
            isOrganization = row[isOrganization]!!,
            createTime = row[createTime]!!,
            updateTime = row[updateTime],
            creator = row[creator]!!,
            updater = row[updater]
    )
}

object V1Tags : BaseTable<V1Tag>("api_tag") {
    val id by long("id").primaryKey()
    val name by varchar("name")
    val introduction by varchar("introduction")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")
    val creator by varchar("creator")
    val updater by varchar("updater")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1Tag(
            id = row[id]!!,
            name = row[name]!!,
            introduction = row[introduction],
            createTime = row[createTime]!!,
            updateTime = row[updateTime],
            creator = row[creator]!!,
            updater = row[updater]
    )
}

object V1Comments : BaseTable<V1Comment>("api_comment") {
    val id by long("id").primaryKey()
    val ownerId by int("owner_id")
    val animationId by long("animation_id")
    val score by int("score")
    val shortComment by varchar("short_comment")
    val article by varchar("article")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1Comment(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            animationId = row[animationId]!!,
            score = row[score],
            shortComment = row[shortComment],
            article = row[article],
            createTime = row[createTime]!!,
            updateTime = row[updateTime]
    )
}

object V1Diaries : BaseTable<V1Diary>("api_diary") {
    val id by long("id").primaryKey()
    val ownerId by int("owner_id")
    val animationId by long("animation_id")
    val status by varchar("status")
    val watchedQuantity by int("watched_quantity")
    val watchedRecord by array("watched_record", V1TimestampStrConverter())
    val watchManyTimes by boolean("watch_many_times")
    val watchOriginalWork by boolean("watch_original_work")
    val subscriptionTime by datetime("subscription_time")
    val finishTime by datetime("finish_time")
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1Diary(
            id = row[id]!!,
            ownerId = row[ownerId]!!,
            animationId = row[animationId]!!,
            status = row[status]!!,
            watchedQuantity = row[watchedQuantity]!!,
            watchedRecord = row[watchedRecord]!!,
            watchManyTimes = row[watchManyTimes]!!,
            watchOriginalWork = row[watchOriginalWork]!!,
            subscriptionTime = row[subscriptionTime],
            finishTime = row[finishTime],
            createTime = row[createTime]!!,
            updateTime = row[updateTime]
    )
}

object V1AnimationStaffByAuthors : BaseTable<V1AnimationStaffByAuthor>("api_animation_original_work_authors") {
    val id by long("id").primaryKey()
    val animationId by long("animation_id")
    val staffId by long("staff_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationStaffByAuthor(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!
    )
}

object V1AnimationStaffByCompanies : BaseTable<V1AnimationStaffByCompany>("api_animation_staff_companies") {
    val id by long("id").primaryKey()
    val animationId by long("animation_id")
    val staffId by long("staff_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationStaffByCompany(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!
    )
}

object V1AnimationStaffBySupervisors : BaseTable<V1AnimationStaffBySupervisor>("api_animation_staff_supervisors") {
    val id by long("id").primaryKey()
    val animationId by long("animation_id")
    val staffId by long("staff_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationStaffBySupervisor(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!
    )
}

object V1AnimationTags : BaseTable<V1AnimationTag>("api_animation_tags") {
    val id by long("id").primaryKey()
    val animationId by long("animation_id")
    val tagId by long("tag_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationTag(
            id = row[id]!!,
            animationId = row[animationId]!!,
            tagId = row[tagId]!!
    )
}

object V1Users : BaseTable<V1User>("auth_user") {
    val id by int("id").primaryKey()
    val isStaff by boolean("is_staff")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1User(
            id = row[id]!!,
            isStaff = row[isStaff]!!
    )
}

object V1Profiles : BaseTable<V1Profile>("api_profile") {
    val id by int("id").primaryKey()
    val username by varchar("username")
    val userId by int("user_id")
    val animationUpdateNotice by boolean("animation_update_notice")
    val nightUpdateMode by boolean("night_update_mode")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1Profile(
            id = row[id]!!,
            username = row[username]!!,
            userId = row[userId]!!,
            animationUpdateNotice = row[animationUpdateNotice]!!,
            nightUpdateMode = row[nightUpdateMode]!!
    )
}
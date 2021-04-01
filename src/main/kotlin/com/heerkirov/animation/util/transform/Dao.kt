package com.heerkirov.animation.util.transform

import com.heerkirov.animation.util.ktorm.array
import com.heerkirov.animation.util.ktorm.jsonString
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.*


object V1Animations : BaseTable<V1Animation>("api_animation") {
    val id = long("id").primaryKey()
    val title = varchar("title")
    val originTitle = varchar("origin_title")
    val otherTitle = varchar("other_title")
    val cover = varchar("cover")
    val originalWorkType = varchar("original_work_type")
    val publishType = varchar("publish_type")
    val publishTime = date("publish_time")
    val sumQuantity = int("sum_quantity")
    val publishedQuantity = int("published_quantity")
    val duration = int("duration")
    val publishPlan = array("publish_plan", V1TimestampStrConverter())
    val publishedRecord = array("published_record", V1TimestampStrConverter())
    val introduction = varchar("introduction")
    val keyword = varchar("keyword")
    val limitLevel = varchar("limit_level")
    val relations = jsonString("original_relations")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")
    val creator = varchar("creator")
    val updater = varchar("updater")

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
    val id = long("id").primaryKey()
    val name = varchar("name")
    val originName = varchar("origin_name")
    val remark = varchar("remark")
    val isOrganization = boolean("is_organization")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")
    val creator = varchar("creator")
    val updater = varchar("updater")

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
    val id = long("id").primaryKey()
    val name = varchar("name")
    val introduction = varchar("introduction")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")
    val creator = varchar("creator")
    val updater = varchar("updater")

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
    val id = long("id").primaryKey()
    val ownerId = int("owner_id")
    val animationId = long("animation_id")
    val score = int("score")
    val shortComment = varchar("short_comment")
    val article = varchar("article")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")

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
    val id = long("id").primaryKey()
    val ownerId = int("owner_id")
    val animationId = long("animation_id")
    val status = varchar("status")
    val watchedQuantity = int("watched_quantity")
    val watchedRecord = array("watched_record", V1TimestampStrConverter())
    val watchManyTimes = boolean("watch_many_times")
    val watchOriginalWork = boolean("watch_original_work")
    val subscriptionTime = datetime("subscription_time")
    val finishTime = datetime("finish_time")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")

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
    val id = long("id").primaryKey()
    val animationId = long("animation_id")
    val staffId = long("staff_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationStaffByAuthor(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!
    )
}

object V1AnimationStaffByCompanies : BaseTable<V1AnimationStaffByCompany>("api_animation_staff_companies") {
    val id = long("id").primaryKey()
    val animationId = long("animation_id")
    val staffId = long("staff_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationStaffByCompany(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!
    )
}

object V1AnimationStaffBySupervisors : BaseTable<V1AnimationStaffBySupervisor>("api_animation_staff_supervisors") {
    val id = long("id").primaryKey()
    val animationId = long("animation_id")
    val staffId = long("staff_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationStaffBySupervisor(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!
    )
}

object V1AnimationTags : BaseTable<V1AnimationTag>("api_animation_tags") {
    val id = long("id").primaryKey()
    val animationId = long("animation_id")
    val tagId = long("tag_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1AnimationTag(
            id = row[id]!!,
            animationId = row[animationId]!!,
            tagId = row[tagId]!!
    )
}

object V1Users : BaseTable<V1User>("auth_user") {
    val id = int("id").primaryKey()
    val isStaff = boolean("is_staff")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1User(
            id = row[id]!!,
            isStaff = row[isStaff]!!
    )
}

object V1Profiles : BaseTable<V1Profile>("api_profile") {
    val id = int("id").primaryKey()
    val username = varchar("username")
    val userId = int("user_id")
    val animationUpdateNotice = boolean("animation_update_notice")
    val nightUpdateMode = boolean("night_update_mode")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = V1Profile(
            id = row[id]!!,
            username = row[username]!!,
            userId = row[userId]!!,
            animationUpdateNotice = row[animationUpdateNotice]!!,
            nightUpdateMode = row[nightUpdateMode]!!
    )
}
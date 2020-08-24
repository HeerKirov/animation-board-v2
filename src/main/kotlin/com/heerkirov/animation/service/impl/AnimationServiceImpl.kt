package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.*
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.filter.AnimationFilter
import com.heerkirov.animation.model.form.AnimationForm
import com.heerkirov.animation.model.form.AnimationPartialForm
import com.heerkirov.animation.model.data.Animation
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.*
import com.heerkirov.animation.service.AnimationService
import com.heerkirov.animation.service.manager.*
import com.heerkirov.animation.util.*
import com.heerkirov.animation.util.ktorm.dsl.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.support.postgresql.ilike
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AnimationServiceImpl(@Autowired private val database: Database,
                           @Autowired private val animationProcessor: AnimationProcessor,
                           @Autowired private val relationProcessor: AnimationRelationProcessor,
                           @Autowired private val tagProcessor: AnimationTagProcessor,
                           @Autowired private val staffProcessor: AnimationStaffProcessor,
                           @Autowired private val recordProcessor: AnimationRecordProcessor) : AnimationService {
    private val orderTranslator = OrderTranslator {
        "publish_time" to Animations.publishTime nulls last
        "create_time" to Animations.createTime
        "update_time" to Animations.updateTime
        "sex_limit_level" to Animations.sexLimitLevel nulls last
        "violence_limit_level" to Animations.violenceLimitLevel nulls last
    }

    private val animationFields = arrayOf(
            Animations.id, Animations.title, Animations.originTitle, Animations.otherTitle, Animations.cover,
            Animations.publishType, Animations.publishTime, Animations.episodeDuration, Animations.totalEpisodes, Animations.publishedEpisodes, Animations.publishedRecord, Animations.publishPlan,
            Animations.introduction, Animations.keyword, Animations.sexLimitLevel, Animations.violenceLimitLevel, Animations.originalWorkType,
            Animations.relations, Animations.relationsTopology, Animations.createTime, Animations.updateTime, Animations.creator, Animations.updater
    )

    override fun list(filter: AnimationFilter, currentUser: User?): ListResult<Animation> {
        return database.from(Animations)
                .let {
                    //当tag条件存在时，在查询中连接tag。
                    if(filter.tag != null) {
                        val filterTagId = filter.tag.toIntOrNull()
                        it.innerJoin(AnimationTagRelations, (AnimationTagRelations.animationId eq Animations.id).let { exp ->
                            if(filterTagId != null) { exp and (AnimationTagRelations.tagId eq filterTagId) }else{ exp }
                        }).let { exp ->
                            if(filterTagId == null) {
                                exp.innerJoin(Tags, (Tags.id eq AnimationTagRelations.tagId) and (Tags.name eq filter.tag))
                            }else{ exp }
                        }
                    }else{ it }
                }
                .let {
                    //当staff条件存在时，在查询中连接staff。
                    if(filter.staff != null || filter.staffType != null) {
                        val filterStaffId = filter.staff?.toIntOrNull()
                        it.innerJoin(AnimationStaffRelations, (AnimationStaffRelations.animationId eq Animations.id).let { exp ->
                            //staff和staffType条件必须同时存在，才会在连接中过滤staffType。
                            if(filter.staffType != null && filter.staff != null) { exp and (AnimationStaffRelations.staffType eq filter.staffType) }else{ exp }
                        }.let { exp ->
                            if(filterStaffId != null) { exp and (AnimationStaffRelations.staffId eq filterStaffId) }else{ exp }
                        }).let { exp ->
                            if(filterStaffId == null && filter.staff != null) {
                                exp.innerJoin(Staffs, (Staffs.id eq AnimationStaffRelations.staffId) and (Staffs.name eq filter.staff))
                            }else{ exp }
                        }
                    }else{ it }
                }
                .select(*animationFields)
                .whereWithConditions {
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        //search参数在title, originTitle, otherTitle, keyword中搜索
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s) or (Animations.keyword ilike s)
                    }
                    if(filter.originalWorkType != null) { it += Animations.originalWorkType eq filter.originalWorkType }
                    if(filter.publishType != null) { it += Animations.publishType eq filter.publishType }
                    if(filter.sexLimitLevel != null) { it += Animations.sexLimitLevel eq filter.sexLimitLevel }
                    if(filter.violenceLimitLevel != null) { it += Animations.violenceLimitLevel eq filter.violenceLimitLevel }
                    if(filter.publishTime != null) {
                        val (year, month) = filter.publishTime.parseYearAndMonth() ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'publish_time' cast error: Param must be 'yyyy' or 'yyyy-MM'.")
                        if(year <= 0) throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'publish_time': year must be greater than 0.")
                        it += if(month != null) {
                            if(month < 1 || month > 12) throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'publish_time': month must between 1 and 12.")
                            val publishTime = LocalDate.of(year, month, 1)
                            Animations.publishTime eq publishTime
                        }else{
                            val begin = LocalDate.of(year, 1, 1)
                            val end = LocalDate.of(year + 1, 1, 1)
                            (Animations.publishTime greaterEq begin) and (Animations.publishTime less end)
                        }
                    }
                }
                .let {
                    //当至少一个join存在时，在结果前需要进行group by以使结果按照animation正确分组。
                    if(filter.tag != null || filter.staff != null || filter.staffType != null) { it.groupBy(Animations.id) }else{ it }
                }
                .orderBy(filter.order, orderTranslator)
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { Animations.createEntity(it) }
    }

    override fun get(id: Int): AnimationResult {
        val animation = database.sequenceOf(Animations).find { it.id eq id } ?: throw NotFoundException("Animation not found.")

        val tags = database.from(AnimationTagRelations)
                .innerJoin(Tags, AnimationTagRelations.tagId eq Tags.id)
                .select(Tags.id, Tags.name)
                .where { AnimationTagRelations.animationId eq id }
                .map { TagRes(it[Tags.id]!!, it[Tags.name]!!) }

        val staffs = database.from(AnimationStaffRelations)
                .innerJoin(Staffs, AnimationStaffRelations.staffId eq Staffs.id)
                .select(Staffs.id, Staffs.name, Staffs.cover, Staffs.isOrganization, Staffs.occupation, AnimationStaffRelations.staffType)
                .where { AnimationStaffRelations.animationId eq id }
                .map { StaffRelationRes(it[Staffs.id]!!, it[Staffs.name]!!, it[Staffs.cover], it[Staffs.isOrganization]!!, it[Staffs.occupation], it[AnimationStaffRelations.staffType]!!) }

        val relationIds = animation.relationsTopology.entries
                .sortedBy { -it.key.level }
                .flatMap { entry -> entry.value.map { Pair(entry.key, it) } }
        val relationMaps = if(relationIds.isNotEmpty()) {
            database.from(Animations)
                    .select(Animations.id, Animations.title, Animations.cover)
                    .where { Animations.id inList relationIds.map { (_, id) -> id } }
                    .map {
                        val animationId = it[Animations.id]!!
                        val res = Pair(it[Animations.title]!!, it[Animations.cover])
                        Pair(animationId, res)
                    }.toMap()
        }else{
            emptyMap()
        }
        val relations = relationIds.map { (r, id) ->
            val (title, cover) = relationMaps[id] ?: error("Cannot find relation of animation $id.")
            AnimationRelationRes(id, title, cover, r)
        }

        return AnimationResult(animation, tags, staffs, relations)
    }

    @Transactional
    override fun create(form: AnimationForm, creator: User): Int {
        val now = DateTimeUtil.now()

        val (publishedEpisodes, publishPlan, publishedRecord) = animationProcessor.processQuantityAndPlan(
                form.totalEpisodes,
                form.publishedEpisodes,
                form.publishPlan,
                emptyList(), now)

        val publishTime = if(form.publishTime == null) { null }else{
            form.publishTime.parseDateMonth() ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'publish_time' must be 'yyyy-MM'.")
        }

        val id = database.insertAndGenerateKey(Animations) {
            it.title to form.title
            it.originTitle to if(form.originTitle?.isNotBlank() == true) form.originTitle else null
            it.otherTitle to if(form.otherTitle?.isNotBlank() == true) form.otherTitle else null
            it.publishType to form.publishType
            it.publishTime to publishTime
            it.episodeDuration to form.episodeDuration
            it.totalEpisodes to form.totalEpisodes
            it.publishedEpisodes to publishedEpisodes
            it.publishedRecord to publishedRecord
            it.publishPlan to publishPlan
            it.introduction to if(form.introduction?.isNotBlank() == true) form.introduction else null
            it.keyword to if(form.keyword?.isNotBlank() == true) form.keyword else null
            it.sexLimitLevel to form.sexLimitLevel
            it.violenceLimitLevel to form.violenceLimitLevel
            it.originalWorkType to form.originalWorkType
            it.relations to emptyMap()
            it.relationsTopology to emptyMap()
            it.createTime to now
            it.updateTime to now
            it.creator to creator.id
            it.updater to creator.id
        } as Int

        if(form.tags.isNotEmpty()) {
            tagProcessor.updateTags(id, form.tags, creator, creating = true)
        }
        if(form.staffs.isNotEmpty()) {
            staffProcessor.updateStaffs(id, form.staffs, creating = true)
        }
        if(form.relations.isNotEmpty()) {
            try {
                relationProcessor.updateRelationTopology(id, form.relations)
            }catch (e: NoSuchElementException) {
                throw BadRequestException(ErrCode.NOT_EXISTS, e.message)
            }
        }

        return id
    }

    @Transactional
    override fun partialUpdate(id: Int, form: AnimationPartialForm, updater: User) {
        val now = DateTimeUtil.now()

        val row = database.update(Animations) {
            where { it.id eq id }
            it.updateTime to now
            it.updater to updater.id
            if(form.title != null) it.title to form.title
            if(form.originTitle != null) it.originTitle to if(form.originTitle.isNotBlank()) form.originTitle else null
            if(form.otherTitle != null) it.otherTitle to if(form.otherTitle.isNotBlank()) form.otherTitle else null
            if(form.introduction != null) it.introduction to if(form.introduction.isNotBlank()) form.introduction else null
            if(form.keyword != null) it.keyword to if(form.keyword.isNotBlank()) form.keyword else null
            if(form.sexLimitLevel != null) it.sexLimitLevel to form.sexLimitLevel
            if(form.violenceLimitLevel != null) it.violenceLimitLevel to form.violenceLimitLevel
            if(form.originalWorkType != null) it.originalWorkType to form.originalWorkType
            if(form.publishType != null) it.publishType to form.publishType
            if(form.episodeDuration != null) it.episodeDuration to form.episodeDuration

            if(form.publishTime != null) {
                it.publishTime to (form.publishTime.parseDateMonth() ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'publish_time' must be 'yyyy-MM'."))
            }

            if(form.totalEpisodes != null || form.publishedEpisodes != null || form.publishPlan != null) {
                val rowSet = database.from(Animations)
                        .select(Animations.totalEpisodes, Animations.publishedEpisodes, Animations.publishPlan, Animations.publishedRecord)
                        .where { Animations.id eq id }
                        .firstOrNull() ?: throw NotFoundException("Animation not found.")
                val oldTotalEpisodes = rowSet[Animations.totalEpisodes]!!
                val oldPublishedEpisodes = rowSet[Animations.publishedEpisodes]!!
                val oldPublishPlan = rowSet[Animations.publishPlan]!!
                val oldPublishedRecord = rowSet[Animations.publishedRecord]!!
                val totalEpisodes = form.totalEpisodes ?: oldTotalEpisodes
                val (publishedEpisodes, publishPlan, publishedRecord) = animationProcessor.processQuantityAndPlan(
                        totalEpisodes, form.publishedEpisodes ?: oldPublishedEpisodes,
                        form.publishPlan ?: oldPublishPlan, oldPublishedRecord, now)
                it.totalEpisodes to totalEpisodes
                it.publishedEpisodes to publishedEpisodes
                it.publishPlan to publishPlan
                it.publishedRecord to publishedRecord

                if(totalEpisodes != oldTotalEpisodes || publishedEpisodes != oldPublishedEpisodes) {
                    recordProcessor.updateRecord(id, totalEpisodes, publishedEpisodes, oldTotalEpisodes, oldPublishedEpisodes)
                }
            }
        }
        if(row == 0) throw NotFoundException("Animation not found.")

        if(form.tags?.isNotEmpty() == true) {
            tagProcessor.updateTags(id, form.tags, updater)
        }
        if(form.staffs?.isNotEmpty() == true) {
            staffProcessor.updateStaffs(id, form.staffs)
        }
        if(form.relations?.isNotEmpty() == true) {
            try {
                relationProcessor.updateRelationTopology(id, form.relations)
            }catch (e: NoSuchElementException) {
                throw BadRequestException(ErrCode.NOT_EXISTS, e.message)
            }
        }
    }

    override fun delete(id: Int) {
        val animation = database.sequenceOf(Animations).find { it.id eq id } ?: throw NotFoundException("Animation not found.")

        recordProcessor.deleteRecords(id)
        if(animation.relationsTopology.isNotEmpty()) relationProcessor.removeAnimationInTopology(id, animation.relationsTopology)
        database.delete(Comments) { it.animationId eq id }
        database.delete(AnimationTagRelations) { it.animationId eq id }
        database.delete(AnimationStaffRelations) { it.animationId eq id }
        database.delete(Animations) { it.id eq id }

    }
}
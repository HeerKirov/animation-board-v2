package com.heerkirov.animation.service.impl

import com.heerkirov.animation.component.RelationProcessor
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
import com.heerkirov.animation.util.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AnimationServiceImpl(@Autowired private val database: Database,
                           @Autowired private val relationProcessor: RelationProcessor) : AnimationService {
    private val orderTranslator = OrderTranslator {
        "publish_time" to Animations.publishTime nulls last
        "create_time" to Animations.createTime
        "update_time" to Animations.updateTime
        "sex_limit_level" to Animations.sexLimitLevel nulls last
        "violence_limit_level" to Animations.violenceLimitLevel nulls last
    }

    private val animationFields = arrayOf(
            Animations.id, Animations.title, Animations.originTitle, Animations.otherTitle, Animations.cover,
            Animations.publishType, Animations.publishTime, Animations.duration, Animations.sumQuantity, Animations.publishedQuantity, Animations.publishedRecord, Animations.publishPlan,
            Animations.introduction, Animations.keyword, Animations.sexLimitLevel, Animations.violenceLimitLevel, Animations.originalWorkType,
            Animations.relations, Animations.relationsTopology, Animations.createTime, Animations.updateTime, Animations.creator, Animations.updater
    )

    override fun list(filter: AnimationFilter, currentUser: User?): ListResult<Animation> {
        //TODO 在record功能完备后，添加seen filter 选项
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
                        it += (Animations.title like s) or (Animations.originTitle like s) or (Animations.otherTitle like s) or (Animations.keyword like s)
                    }
                    if(filter.originalWorkType != null) { it += Animations.originalWorkType eq filter.originalWorkType }
                    if(filter.publishType != null) { it += Animations.publishType eq filter.publishType }
                    if(filter.sexLimitLevel != null) { it += Animations.sexLimitLevel eq filter.sexLimitLevel }
                    if(filter.violenceLimitLevel != null) { it += Animations.violenceLimitLevel eq filter.violenceLimitLevel }
                    if(filter.publishTime != null) {
                        val (year, month) = filter.publishTime.toYearAndMonth() ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'publish_time' cast error: Param must be 'yyyy' or 'yyyy-MM'.")
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

    @Transactional
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

        val relationMaps = animation.relationsTopology.entries
                .sortedBy { -it.key.level }
                .flatMap { entry -> entry.value.map { Pair(it, entry.key) } }.toMap()
        val relations = if(relationMaps.isNotEmpty()) {
            database.from(Animations)
                    .select(Animations.id, Animations.title, Animations.cover)
                    .where { Animations.id inList relationMaps.keys }
                    .map {
                        val animationId = it[Animations.id]!!
                        AnimationRelationRes(animationId, it[Animations.title]!!, it[Animations.cover], relationMaps[animationId] ?: error("Cannot find relation of animation $animationId."))
                    }
        }else{
            emptyList()
        }

        return AnimationResult(animation, tags, staffs, relations)
    }

    @Transactional
    override fun create(animationForm: AnimationForm, creator: User): Int {
        val publishTime = if(animationForm.publishTime == null) { null }else{
            animationForm.publishTime.toDateMonth() ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Param 'publish_time' must be 'yyyy-MM'.")
        }
        val sumQuantity = animationForm.sumQuantity
        val publishedQuantity = when {
            sumQuantity == null -> null
            animationForm.publishedQuantity != null && animationForm.publishedQuantity > sumQuantity -> sumQuantity
            else -> animationForm.publishedQuantity
        }

        val now = DateTimeUtil.now()

        val id = database.insertAndGenerateKey(Animations) {
            it.title to animationForm.title
            it.originTitle to animationForm.originTitle
            it.otherTitle to animationForm.otherTitle
            it.publishType to animationForm.publishType
            it.publishTime to publishTime
            it.duration to animationForm.duration
            it.sumQuantity to sumQuantity
            it.publishedQuantity to publishedQuantity
            it.publishedRecord to emptyList()
            //TODO 在创建/更新时就可以处理plan，使生效内容加入record，而不是等到schedule执行时
            //      控制plan的计划数量不得超过sumQuantity
            it.publishPlan to animationForm.publishPlan
            it.introduction to animationForm.introduction
            it.keyword to animationForm.keyword
            it.sexLimitLevel to animationForm.sexLimitLevel
            it.violenceLimitLevel to animationForm.violenceLimitLevel
            it.originalWorkType to animationForm.originalWorkType
            it.relations to emptyMap()
            it.relationsTopology to emptyMap()
            it.createTime to now
            it.updateTime to now
            it.creator to creator.id
            it.updater to creator.id
        }

        return id as Int
    }

    @Transactional
    override fun update(id: Int, animationForm: AnimationForm, updater: User) {
        TODO("Not yet implemented")
    }

    @Transactional
    override fun partialUpdate(id: Int, animationPartialForm: AnimationPartialForm, updater: User) {
        TODO("Not yet implemented")
    }

    override fun delete(id: Int) {
        TODO("Not yet implemented")
    }
}
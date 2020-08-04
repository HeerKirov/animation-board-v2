package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.AnimationTagRelations
import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.filter.TagFilter
import com.heerkirov.animation.model.form.*
import com.heerkirov.animation.model.data.Tag
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.result.toListResult
import com.heerkirov.animation.service.TagService
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.OrderTranslator
import com.heerkirov.animation.util.orderBy
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.support.postgresql.ilike
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagServiceImpl(@Autowired private val database: Database) : TagService {
    private val orderTranslator = OrderTranslator {
        "name" to Tags.name
        "group" to Tags.group nulls last
        "ordinal" to Tags.ordinal
        "animation_count" to Tags.animationCount
        "create_time" to Tags.createTime
        "update_time" to Tags.updateTime
    }

    override fun list(filter: TagFilter): ListResult<Tag> {
        return database.from(Tags).select()
                .whereWithConditions {
                    if(filter.search != null) {
                        it += Tags.name ilike "%${filter.search}%"
                    }
                }
                .orderBy(filter.order, orderTranslator)
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { Tags.createEntity(it) }
    }

    override fun get(id: Int): Tag {
        return database.sequenceOf(Tags)
                .find { it.id eq id }
                ?: throw NotFoundException("Tag not found.")
    }

    @Transactional
    override fun create(tagForm: TagCreateForm, creator: User): Int {
        database.sequenceOf(Tags).find { it.name eq tagForm.name }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Tag with name '${tagForm.name}' is already exists.")
        }

        val now = DateTimeUtil.now()
        return database.insertAndGenerateKey(Tags) {
            it.name to tagForm.name
            it.introduction to tagForm.introduction
            it.group to if(tagForm.group?.isNotBlank() == true) tagForm.group else null
            it.ordinal to getTagCount()
            it.createTime to now
            it.updateTime to now
            it.creator to creator.id
            it.updater to creator.id
        } as Int
    }

    @Transactional
    override fun update(id: Int, tagForm: TagUpdateForm, updater: User) {
        database.sequenceOf(Tags).find { (it.name eq tagForm.name) and (it.id notEq id) }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Tag with name '${tagForm.name}' is already exists.")
        }

        if(database.update(Tags) {
            it.name to tagForm.name
            it.introduction to tagForm.introduction
            it.updateTime to DateTimeUtil.now()
            it.updater to updater.id
            where { it.id eq id }
        } == 0) throw NotFoundException("Tag not found.")
    }

    @Transactional
    override fun partialUpdate(id: Int, tagForm: TagPartialForm, updater: User) {
        if(tagForm.name?.isNotBlank() == true) {
            database.sequenceOf(Tags).find { (it.name eq tagForm.name) and (it.id notEq id) }?.run {
                throw BadRequestException(ErrCode.ALREADY_EXISTS, "Tag with name '${tagForm.name}' is already exists.")
            }
        }

        val ordinal = if(tagForm.ordinal == null) null else {
            val tag = database.sequenceOf(Tags).find { it.id eq id } ?: throw NotFoundException("Tag not found.")
            if(tagForm.ordinal > tag.ordinal) {
                database.update(Tags) {
                    where { (it.ordinal greater tag.ordinal) and (it.ordinal lessEq tagForm.ordinal) }
                    it.ordinal to (it.ordinal - 1)
                }
                val tagCount = getTagCount()
                if(tagForm.ordinal > tagCount) tagCount else tagForm.ordinal
            }else if(tagForm.ordinal < tag.ordinal) {
                database.update(Tags) {
                    where { (it.ordinal greaterEq tagForm.ordinal) and (it.ordinal less tag.ordinal) }
                    it.ordinal to (it.ordinal + 1)
                }
                val tagCount = getTagCount()
                if(tagForm.ordinal > tagCount) tagCount else tagForm.ordinal
            }else null
        }

        if(database.update(Tags) {
            if(tagForm.name != null) it.name to tagForm.name
            if(tagForm.introduction != null) it.introduction to tagForm.introduction
            if(tagForm.group != null) it.group to if(tagForm.group.isNotBlank()) tagForm.group else null
            if(ordinal != null) it.ordinal to ordinal
            it.updateTime to DateTimeUtil.now()
            it.updater to updater.id
            where { it.id eq id }
        } == 0) throw NotFoundException("Tag not found.")
    }

    @Transactional
    override fun delete(id: Int) {
        if(database.delete(Tags) { it.id eq id } == 0) throw NotFoundException("Tag not found.")
        database.delete(AnimationTagRelations) { it.tagId eq id }
    }

    private fun getTagCount(): Int {
        return database.sequenceOf(Tags).count()
    }
}
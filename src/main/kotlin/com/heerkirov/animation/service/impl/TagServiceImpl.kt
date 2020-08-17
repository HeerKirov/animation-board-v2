package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.AnimationTagRelations
import com.heerkirov.animation.dao.TagGroups
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Service
class TagServiceImpl(@Autowired private val database: Database) : TagService {
    private val orderTranslator = OrderTranslator {
        "name" to Tags.name
        "group" to TagGroups.ordinal nulls last
        "ordinal" to Tags.ordinal
        "animation_count" to Tags.animationCount
        "create_time" to Tags.createTime
        "update_time" to Tags.updateTime
    }

    override fun list(filter: TagFilter): ListResult<Tag> {
        return database.from(Tags)
                .leftJoin(TagGroups, Tags.group eq TagGroups.group)
                .select()
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

    override fun groupList(): List<List<Tag>> {
        val tags = database.from(Tags)
                .leftJoin(TagGroups, Tags.group eq TagGroups.group)
                .select()
                .orderBy(TagGroups.ordinal.asc(), Tags.ordinal.asc())
                .map { Tags.createEntity(it) }

        val groups = HashMap<String?, MutableList<Tag>>()
        val lists = LinkedList<List<Tag>>()

        for (tag in tags) {
            groups.computeIfAbsent(tag.group) { ArrayList<Tag>().apply { if(it != null) { lists.add(this) } } }.add(tag)
        }
        groups[null]?.apply { lists.add(this) }

        return lists
    }

    @Transactional
    override fun create(tagForm: TagCreateForm, creator: User): Int {
        database.sequenceOf(Tags).find { it.name eq tagForm.name }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Tag with name '${tagForm.name}' is already exists.")
        }

        val now = DateTimeUtil.now()
        val group = if(tagForm.group?.isNotBlank() == true) tagForm.group else null
        if(group != null && database.sequenceOf(TagGroups).find { it.group eq group } == null) {
            database.insert(TagGroups) {
                it.group to group
                it.ordinal to (getGroupCount() + 1)
            }
        }

        return database.insertAndGenerateKey(Tags) {
            it.name to tagForm.name
            it.introduction to tagForm.introduction
            it.group to group
            it.ordinal to (getTagCount(group) + 1)
            it.createTime to now
            it.updateTime to now
            it.creator to creator.id
            it.updater to creator.id
        } as Int
    }

    @Transactional
    override fun partialUpdate(id: Int, tagForm: TagPartialForm, updater: User) {
        if(tagForm.name?.isNotBlank() == true) {
            database.sequenceOf(Tags).find { (it.name eq tagForm.name) and (it.id notEq id) }?.run {
                throw BadRequestException(ErrCode.ALREADY_EXISTS, "Tag with name '${tagForm.name}' is already exists.")
            }
        }

        //TODO 重构group的更新相关代码。group一旦更新，ordinal必须变更
        val newGroup = if(tagForm.group.isNullOrBlank()) null else {

            tagForm.group
        }

        val newOrdinal = if(tagForm.ordinal == null) null else {
            val tag = database.sequenceOf(Tags).find { it.id eq id } ?: throw NotFoundException("Tag not found.")
            if(tagForm.ordinal > tag.ordinal) {
                database.update(Tags) {
                    where { if(tag.group != null) { it.group eq tag.group }else{ it.group.isNull() } and (it.ordinal greater tag.ordinal) and (it.ordinal lessEq tagForm.ordinal) }
                    it.ordinal to (it.ordinal - 1)
                }
                val tagCount = getTagCount(tag.group)
                if(tagForm.ordinal > tagCount) tagCount else tagForm.ordinal
            }else if(tagForm.ordinal < tag.ordinal) {
                database.update(Tags) {
                    where { if(tag.group != null) { it.group eq tag.group }else{ it.group.isNull() } and (it.ordinal greaterEq tagForm.ordinal) and (it.ordinal less tag.ordinal) }
                    it.ordinal to (it.ordinal + 1)
                }
                val tagCount = getTagCount(tag.group)
                if(tagForm.ordinal > tagCount) tagCount else tagForm.ordinal
            }else null
        }

        if(database.update(Tags) {
            if(tagForm.name != null) it.name to tagForm.name
            if(tagForm.introduction != null) it.introduction to tagForm.introduction
            if(newGroup != null) it.group to newGroup
            if(newOrdinal != null) it.ordinal to newOrdinal
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

    @Transactional
    override fun groupPartialUpdate(group: String, form: GroupPartialForm) {
        val tagGroup = database.sequenceOf(TagGroups).find { it.group eq group } ?: throw NotFoundException("TagGroup not found.")

        val newGroup = if(form.group.isNullOrBlank() || form.group == group) null else {
            database.update(Tags) {
                where { it.group eq group }
                it.group to form.group
            }
            form.group
        }
        val newOrdinal = if(form.ordinal == null || form.ordinal == tagGroup.ordinal) null else {
            if(form.ordinal > tagGroup.ordinal) {
                database.update(TagGroups) {
                    where { (it.ordinal greater tagGroup.ordinal) and (it.ordinal lessEq form.ordinal) }
                    it.ordinal to (it.ordinal - 1)
                }
            }else{
                database.update(TagGroups) {
                    where { (it.ordinal greaterEq form.ordinal) and (it.ordinal less tagGroup.ordinal) }
                    it.ordinal to (it.ordinal + 1)
                }
            }
            val groupCount = getGroupCount()
            if(form.ordinal > groupCount) groupCount else form.ordinal
        }

        database.update(TagGroups) {
            where { it.group eq group }
            if(newGroup != null) it.group to newGroup
            if(newOrdinal != null) it.ordinal to newOrdinal
        }
    }

    private fun getTagCount(group: String?): Int {
        return database.sequenceOf(Tags).filter { if(group != null) { it.group eq group }else{ it.group.isNull() } }.count()
    }

    private fun getGroupCount(): Int {
        return database.sequenceOf(TagGroups).count()
    }
}
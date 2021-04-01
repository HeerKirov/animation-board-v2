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
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.support.postgresql.ilike
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
                set(it.group, group)
                set(it.ordinal, getGroupCount() + 1)
            }
        }

        return database.insertAndGenerateKey(Tags) {
            set(it.name, tagForm.name)
            set(it.introduction, tagForm.introduction)
            set(it.group, group)
            set(it.ordinal, getTagCount(group) + 1)
            set(it.createTime, now)
            set(it.updateTime, now)
            set(it.creator, creator.id)
            set(it.updater, creator.id)
        } as Int
    }

    @Transactional
    override fun partialUpdate(id: Int, tagForm: TagPartialForm, updater: User) {
        if(tagForm.name?.isNotBlank() == true) {
            database.sequenceOf(Tags).find { (it.name eq tagForm.name) and (it.id notEq id) }?.run {
                throw BadRequestException(ErrCode.ALREADY_EXISTS, "Tag with name '${tagForm.name}' is already exists.")
            }
        }
        val tag = database.sequenceOf(Tags).find { it.id eq id } ?: throw NotFoundException("Tag not found.")

        val (newGroup, newOrdinal) = if(tagForm.group.isNullOrBlank() || tagForm.group == tag.group) {
            //group没有变化，那么只在当前group的范围内做ordinal的变动
            Pair(null, if(tagForm.ordinal == null || tagForm.ordinal == tag.ordinal) null else {
                //ordinal发生了变动
                val max = getTagCount(tag.group)
                val newOrdinal = if(tagForm.ordinal > max) max else tagForm.ordinal
                if(newOrdinal > tag.ordinal) {
                    database.update(Tags) {
                        where { if(tag.group != null) { it.group eq tag.group }else{ it.group.isNull() } and (it.ordinal greater tag.ordinal) and (it.ordinal lessEq newOrdinal) }
                        set(it.ordinal, it.ordinal - 1)
                    }
                }else{
                    database.update(Tags) {
                        where { if(tag.group != null) { it.group eq tag.group }else{ it.group.isNull() } and (it.ordinal greaterEq newOrdinal) and (it.ordinal less tag.ordinal) }
                        set(it.ordinal, it.ordinal + 1)
                    }
                }
                newOrdinal
            })
        }else{
            //group发生变化
            //如果group不存在则创建
            if(database.sequenceOf(TagGroups).find { it.group eq tagForm.group } == null) {
                database.insert(TagGroups) {
                    set(it.group, tagForm.group)
                    set(it.ordinal, getGroupCount() + 1)
                }
            }
            //重排旧的group的ordinal
            database.update(Tags) {
                where { if (tag.group != null) { it.group eq tag.group }else{ it.group.isNull() } and (it.ordinal greater tag.ordinal) }
                set(it.ordinal, it.ordinal - 1)
            }
            //将tag插入新的group中
            Pair(tagForm.group, if(tagForm.ordinal == null) {
                //没有指定ordinal，那么追加到新group的末尾
                getTagCount(tagForm.group) + 1
            }else{
                //指定了ordinal，那么将其插入
                val max = getTagCount(tagForm.group) + 1
                val newOrdinal = if(tagForm.ordinal > max) max else tagForm.ordinal
                database.update(Tags) {
                    where { (it.group eq tagForm.group) and (it.ordinal greaterEq newOrdinal) }
                    set(it.ordinal, it.ordinal + 1)
                }
                newOrdinal
            })
        }

        if(database.update(Tags) {
            if(tagForm.name != null) set(it.name, tagForm.name)
            if(tagForm.introduction != null) set(it.introduction, tagForm.introduction)
            if(newGroup != null) set(it.group, newGroup)
            if(newOrdinal != null) set(it.ordinal, newOrdinal)
            set(it.updateTime, DateTimeUtil.now())
            set(it.updater, updater.id)
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
            database.sequenceOf(TagGroups).find { (it.group eq form.group) }?.run {
                throw BadRequestException(ErrCode.ALREADY_EXISTS, "TagGroup with name '${form.group}' is already exists.")
            }
            database.update(Tags) {
                where { it.group eq group }
                set(it.group, form.group)
            }
            form.group
        }
        val newOrdinal = if(form.ordinal == null || form.ordinal == tagGroup.ordinal) null else {
            if(form.ordinal > tagGroup.ordinal) {
                database.update(TagGroups) {
                    where { (it.ordinal greater tagGroup.ordinal) and (it.ordinal lessEq form.ordinal) }
                    set(it.ordinal, it.ordinal - 1)
                }
            }else{
                database.update(TagGroups) {
                    where { (it.ordinal greaterEq form.ordinal) and (it.ordinal less tagGroup.ordinal) }
                    set(it.ordinal, it.ordinal + 1)
                }
            }
            val groupCount = getGroupCount()
            if(form.ordinal > groupCount) groupCount else form.ordinal
        }

        if(newGroup != null || newOrdinal != null) {
            database.update(TagGroups) {
                where { it.group eq group }
                if(newGroup != null) set(it.group, newGroup)
                if(newOrdinal != null) set(it.ordinal, newOrdinal)
            }
        }
    }

    private fun getTagCount(group: String?): Int {
        return database.sequenceOf(Tags).filter { if(group != null) { it.group eq group }else{ it.group.isNull() } }.count()
    }

    private fun getGroupCount(): Int {
        return database.sequenceOf(TagGroups).count()
    }
}
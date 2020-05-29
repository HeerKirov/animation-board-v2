package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.AnimationTagRelations
import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.form.TagDetailRes
import com.heerkirov.animation.form.TagForm
import com.heerkirov.animation.form.TagRes
import com.heerkirov.animation.model.Tag
import com.heerkirov.animation.model.User
import com.heerkirov.animation.service.TagService
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.map
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.toList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagServiceImpl(@Autowired private val database: Database) : TagService {
    override fun list(): List<Tag> {
        return database.sequenceOf(Tags).toList()
    }

    override fun get(id: Int): Tag {
        return database.sequenceOf(Tags)
                .find { it.id eq id }
                ?: throw NotFoundException("Tag not found.")
    }

    @Transactional
    override fun create(tagForm: TagForm, creator: User): Int {
        database.sequenceOf(Tags).find { it.name eq tagForm.name }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Tag with name '${tagForm.name}' is already exists.")
        }

        val now = DateTimeUtil.now()
        return database.insertAndGenerateKey(Tags) {
            it.name to tagForm.name
            it.introduction to tagForm.introduction
            it.createTime to now
            it.updateTime to now
            it.creator to creator.id
            it.updater to creator.id
        } as Int
    }

    @Transactional
    override fun update(id: Int, tagForm: TagForm, updater: User) {
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
    override fun delete(id: Int) {
        if(database.delete(Tags) { it.id eq id } == 0) throw NotFoundException("Tag not found.")
        database.delete(AnimationTagRelations) { it.tagId eq id }
    }
}
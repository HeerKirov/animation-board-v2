package com.heerkirov.animation.service.manager

import com.heerkirov.animation.dao.AnimationTagRelations
import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.util.DateTimeUtil
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TagProcessor(@Autowired private val database: Database) {
    /**
     * 为animation更新tags。智能处理tag列表和关系映射表。
     */
    fun updateTags(animationId: Int, tags: List<Any>, user: User, creating: Boolean = false) {
        //查出新的tag的id列表
        val tagIds = findAll(tags, user)
        //查出旧的animation关联的关系列表
        val oldTagIds = if(creating) { emptySet() }else{
            database.from(AnimationTagRelations).select(AnimationTagRelations.tagId)
                    .where { AnimationTagRelations.animationId eq animationId }
                    .asSequence()
                    .map { it[AnimationTagRelations.tagId]!! }
                    .toSet()
        }
        //查找关联列表有，但新列表没有的项，也就是需要删除的关联
        val deleteIds = oldTagIds - tagIds
        for (deleteId in deleteIds) {
            database.delete(AnimationTagRelations) { (it.animationId eq animationId) and (it.tagId eq deleteId) }
        }
        //查找关联列表没有，但新列表有的项，也就是需要添加的关联
        val addIds = tagIds - oldTagIds
        database.batchInsert(AnimationTagRelations) {
            for (addId in addIds) {
                item {
                    it.animationId to animationId
                    it.tagId to addId
                }
            }
        }
    }

    /**
     * 解析乱七八糟的tag列表，创建不存在的tag，最终返回一个id列表。
     */
    fun findAll(tags: List<Any>, user: User): List<Int> {
        val names = arrayListOf<String>()
        val ids = arrayListOf<Int>()
        for (tag in tags) {
            when (tag) {
                is Int -> ids += tag
                is String -> names += tag
                else -> throw BadRequestException(ErrCode.PARAM_ERROR, "Tag must be id(Int) or name(String).")
            }
        }
        val ret = arrayListOf(tags.size)
        //处理值是id的tags
        if(ids.isNotEmpty()) {
            val idRowSet = database.from(Tags).select(Tags.id).where { Tags.id inList ids }
            if(idRowSet.totalRecords < ids.size) {
                val minus = ids.toSet() - idRowSet.map { it[Tags.id]!! }.toSet()
                throw BadRequestException(ErrCode.NOT_EXISTS, "Tag ${minus.joinToString(", ")} not exists.")
            }
            ret.addAll(ids)
        }
        //处理值是name的tags
        if(names.isNotEmpty()) {
            val nameRowSet = database.from(Tags).select(Tags.id, Tags.name)
                    .where { Tags.name inList names }
                    .asSequence()
                    .map { Pair(it[Tags.name]!!, it[Tags.id]!!) }
                    .toMap()
            if(nameRowSet.size < names.size) {
                val now = DateTimeUtil.now()
                val creatingList = ArrayList<String>(names.size - nameRowSet.size)
                database.batchInsert(Tags) {
                    for (name in names) {
                        if(!nameRowSet.containsKey(name)) {
                            creatingList += name
                            item {
                                it.name to name
                                it.introduction to ""
                                it.createTime to now
                                it.updateTime to now
                                it.creator to user.id
                                it.updater to user.id
                            }
                        }
                    }
                }
                ret.addAll(database.from(Tags).select(Tags.id).where { Tags.name inList creatingList }.map { it[Tags.id]!! })
            }
            ret.addAll(nameRowSet.values)
        }

        return ret
    }
}
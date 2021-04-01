package com.heerkirov.animation.service.manager

import com.heerkirov.animation.dao.AnimationTagRelations
import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimationTagProcessor(@Autowired private val database: Database) {
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
                    set(it.animationId, animationId)
                    set(it.tagId, addId)
                }
            }
        }
        //更新数量变化
        database.batchUpdate(Tags) {
            for (deleteId in deleteIds) {
                item {
                    where { it.id eq deleteId }
                    set(it.animationCount, it.animationCount minus 1)
                }
            }
        }
        database.batchUpdate(Tags) {
            for (addId in addIds) {
                item {
                    where { it.id eq addId }
                    set(it.animationCount, it.animationCount plus 1)
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
        val ret = ArrayList<Int>(tags.size)
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
                                set(it.name, name)
                                set(it.introduction, "")
                                set(it.createTime, now)
                                set(it.updateTime, now)
                                set(it.creator, user.id)
                                set(it.updater, user.id)
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

    /**
     * 更新全部tag的count。
     */
    fun updateAllCount() {
        val rowSets = database.from(Tags)
                .leftJoin(AnimationTagRelations, Tags.id eq AnimationTagRelations.tagId)
                .select(Tags.id, count(AnimationTagRelations.animationId))
                .groupBy(Tags.id)
                .having { Tags.animationCount notEq count(AnimationTagRelations.animationId) }
                .map { Pair(it[Tags.id]!!, it.getInt(2)) }

        database.batchUpdate(Tags) {
            for (row in rowSets) {
                item {
                    where { it.id eq row.first }
                    set(it.animationCount, row.second)
                }
            }
        }
    }
}
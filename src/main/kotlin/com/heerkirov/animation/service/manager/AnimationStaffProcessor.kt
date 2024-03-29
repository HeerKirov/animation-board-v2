package com.heerkirov.animation.service.manager

import com.heerkirov.animation.dao.AnimationStaffRelations
import com.heerkirov.animation.dao.Staffs
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.enums.StaffTypeInAnimation
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class AnimationStaffProcessor(@Autowired private val database: Database) {
    /**
     * 为animation更新staffs。智能处理关系映射表。
     */
    fun updateStaffs(animationId: Int, staffs: Map<StaffTypeInAnimation, List<Int>>, creating: Boolean = false) {
        //检查更新的staff表中的staff id都存在
        checkExists(staffs.flatMap { it.value })
        //查出旧的animation关联的关系列表
        val oldStaffs = if(creating) { emptySet() }else{
            database.from(AnimationStaffRelations).select(AnimationStaffRelations.staffId, AnimationStaffRelations.staffType)
                    .where { AnimationStaffRelations.animationId eq animationId }
                    .asSequence()
                    .map { Pair(it[AnimationStaffRelations.staffId]!!, it[AnimationStaffRelations.staffType]!!) }
                    .toSet()
        }
        //将staff表映射为关系键值对
        val newStaffs = staffs.flatMap { (r, list) -> list.map { Pair(it, r) } }
        //构建数量变化集合
        val counts = HashMap<Int, Int>()
        //查找不变项
        val keepItems = (oldStaffs intersect newStaffs).asSequence().map { it.first }.toSet()
        //查找需要删除的关联项
        val deleteItems = oldStaffs - newStaffs
        for ((staffId, staffType) in deleteItems) {
            database.delete(AnimationStaffRelations) { (it.animationId eq animationId) and (it.staffId eq staffId) and (it.staffType eq staffType) }
            if(!keepItems.contains(staffId)) {
                counts[staffId] = counts.computeIfAbsent(staffId) { 0 } - 1
            }
        }
        //查找需要增加的关联项
        val addItems = newStaffs - oldStaffs
        database.batchInsert(AnimationStaffRelations) {
            for ((staffId, staffType) in addItems) {
                item {
                    set(it.animationId, animationId)
                    set(it.staffId, staffId)
                    set(it.staffType, staffType)
                }
                if(!keepItems.contains(staffId)) {
                    counts[staffId] = counts.computeIfAbsent(staffId) { 0 } + 1
                }
            }
        }
        //更新数量变化
        database.batchUpdate(Staffs) {
            counts.forEach { (staffId, delta) ->
                item {
                    where { it.id eq staffId }
                    set(it.animationCount, it.animationCount plus delta)
                }
            }
        }
    }

    /**
     * 检查staff id是否都存在。
     */
    fun checkExists(staffIds: List<Int>) {
        if(staffIds.isNotEmpty()) {
            val set = staffIds.toSet()
            val rowSet = database.from(Staffs).select(Staffs.id).where { Staffs.id inList set }
            if(rowSet.totalRecords < set.size) {
                val minus = set.toSet() - rowSet.map { it[Staffs.id]!! }.toSet()
                throw BadRequestException(ErrCode.PARAM_ERROR, "Staff ${minus.joinToString(", ")} not exists.")
            }
        }
    }

    /**
     * 更新全部staff的count。
     */
    fun updateAllCount() {
        val rowSets = database.from(Staffs)
                .leftJoin(AnimationStaffRelations, Staffs.id eq AnimationStaffRelations.staffId)
                .select(Staffs.id, count(AnimationStaffRelations.animationId))
                .groupBy(Staffs.id)
                .having { Staffs.animationCount notEq count(AnimationStaffRelations.animationId) }
                .map { Pair(it[Staffs.id]!!, it.getInt(2)) }

        database.batchUpdate(Staffs) {
            for (row in rowSets) {
                item {
                    where { it.id eq row.first }
                    set(it.animationCount, row.second)
                }
            }
        }
    }
}
package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.AnimationStaffRelations
import com.heerkirov.animation.dao.Staffs
import com.heerkirov.animation.enums.StaffTypeInAnimation
import com.heerkirov.animation.util.logger
import org.ktorm.database.Database
import org.ktorm.dsl.batchInsert
import org.ktorm.dsl.insertAndGenerateKey
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy

class TransformStaff(private val userLoader: UserLoader,
                     private val v1Database: Database,
                     private val database: Database) {
    private val log = logger<TransformStaff>()

    fun transform(animationIdMap: Map<Long, Int>) {
        val staffIdMap = HashMap<Long, Int>()

        for (v1Staff in v1Database.sequenceOf(V1Staffs).sortedBy { it.id }) {
            val id = database.insertAndGenerateKey(Staffs) {
                set(it.name, v1Staff.name)
                set(it.originName, v1Staff.originName)
                set(it.remark, v1Staff.remark)
                set(it.cover, null)
                set(it.isOrganization, v1Staff.isOrganization)
                set(it.occupation, null)
                set(it.createTime, v1Staff.createTime.toV2Time())
                set(it.updateTime, v1Staff.updateTime?.toV2Time() ?: v1Staff.createTime.toV2Time())
                set(it.creator, userLoader[v1Staff.creator].id)
                set(it.updater, userLoader[v1Staff.updater ?: v1Staff.creator].id)
            } as Int
            staffIdMap[v1Staff.id] = id
        }
        log.info("Transform ${staffIdMap.size} Staffs from v1.")

        var num = 0
        database.batchInsert(AnimationStaffRelations) {
            for (v1Relation in v1Database.sequenceOf(V1AnimationStaffByAuthors)) {
                item {
                    set(it.staffId, staffIdMap[v1Relation.staffId])
                    set(it.animationId, animationIdMap[v1Relation.animationId])
                    set(it.staffType, StaffTypeInAnimation.AUTHOR)
                }
                num += 1
            }
            for (v1Relation in v1Database.sequenceOf(V1AnimationStaffByCompanies)) {
                item {
                    set(it.staffId, staffIdMap[v1Relation.staffId])
                    set(it.animationId, animationIdMap[v1Relation.animationId])
                    set(it.staffType, StaffTypeInAnimation.COMPANY)
                }
                num += 1
            }
            for (v1Relation in v1Database.sequenceOf(V1AnimationStaffBySupervisors)) {
                item {
                    set(it.staffId, staffIdMap[v1Relation.staffId])
                    set(it.animationId, animationIdMap[v1Relation.animationId])
                    set(it.staffType, StaffTypeInAnimation.STAFF)
                }
                num += 1
            }
        }
        log.info("Transform $num animation-staff's relation from v1.")
    }
}
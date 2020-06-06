package com.heerkirov.animation.util.transform

import com.heerkirov.animation.dao.AnimationStaffRelations
import com.heerkirov.animation.dao.Staffs
import com.heerkirov.animation.enums.StaffTypeInAnimation
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.batchInsert
import me.liuwj.ktorm.dsl.insertAndGenerateKey
import me.liuwj.ktorm.entity.sequenceOf

class TransformStaff(private val userLoader: UserLoader,
                     private val v1Database: Database,
                     private val database: Database) {
    private val log = logger<TransformStaff>()

    fun transform(animationIdMap: Map<Long, Int>) {
        val staffIdMap = HashMap<Long, Int>()

        for (v1Staff in v1Database.sequenceOf(V1Staffs)) {
            val id = database.insertAndGenerateKey(Staffs) {
                it.name to v1Staff.name
                it.originName to v1Staff.originName
                it.remark to v1Staff.remark
                it.cover to null
                it.isOrganization to v1Staff.isOrganization
                it.occupation to null
                it.createTime to v1Staff.createTime.toV2Time()
                it.updateTime to (v1Staff.updateTime?.toV2Time() ?: v1Staff.createTime.toV2Time())
                it.creator to userLoader[v1Staff.creator].id
                it.updater to userLoader[v1Staff.updater ?: v1Staff.creator].id
            } as Int
            staffIdMap[v1Staff.id] = id
        }
        log.info("Transform ${staffIdMap.size} Staffs from v1.")

        var num = 0
        database.batchInsert(AnimationStaffRelations) {
            for (v1Relation in v1Database.sequenceOf(V1AnimationStaffByAuthors)) {
                item {
                    it.staffId to staffIdMap[v1Relation.staffId]
                    it.animationId to animationIdMap[v1Relation.animationId]
                    it.staffType to StaffTypeInAnimation.AUTHOR
                }
                num += 1
            }
            for (v1Relation in v1Database.sequenceOf(V1AnimationStaffByCompanies)) {
                item {
                    it.staffId to staffIdMap[v1Relation.staffId]
                    it.animationId to animationIdMap[v1Relation.animationId]
                    it.staffType to StaffTypeInAnimation.COMPANY
                }
                num += 1
            }
            for (v1Relation in v1Database.sequenceOf(V1AnimationStaffBySupervisors)) {
                item {
                    it.staffId to staffIdMap[v1Relation.staffId]
                    it.animationId to animationIdMap[v1Relation.animationId]
                    it.staffType to StaffTypeInAnimation.STAFF
                }
                num += 1
            }
        }
        log.info("Transform $num animation-staff's relation from v1.")
    }
}
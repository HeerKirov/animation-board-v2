package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.AnimationStaffRelations
import com.heerkirov.animation.dao.Staffs
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.filter.StaffFilter
import com.heerkirov.animation.model.result.ListResult
import com.heerkirov.animation.model.form.StaffForm
import com.heerkirov.animation.model.result.toListResult
import com.heerkirov.animation.model.data.Staff
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.service.StaffService
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.OrderTranslator
import com.heerkirov.animation.util.orderBy
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.support.postgresql.ilike
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StaffServiceImpl(@Autowired private val database: Database) : StaffService {
    private val orderTranslator = OrderTranslator {
        "name" to Staffs.name
        "animation_count" to Staffs.animationCount
        "create_time" to Staffs.createTime
        "update_time" to Staffs.updateTime
    }

    override fun list(filter: StaffFilter): ListResult<Staff> {
        return database.from(Staffs).select()
                .whereWithConditions {
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Staffs.name ilike s) or (Staffs.originName ilike s) or (Staffs.remark ilike s)
                    }
                    if(filter.isOrganization != null) {
                        it += Staffs.isOrganization eq filter.isOrganization
                    }
                    if(filter.occupation != null) {
                        it += Staffs.occupation eq filter.occupation
                    }
                }
                .orderBy(filter.order, orderTranslator)
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { Staffs.createEntity(it) }
    }

    override fun get(id: Int): Staff {
        return database.sequenceOf(Staffs)
                .find { it.id eq id }
                ?: throw NotFoundException("Staff not found.")
    }

    override fun create(staffForm: StaffForm, creator: User): Int {
        database.sequenceOf(Staffs).find { it.name eq staffForm.name }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Staff with name '${staffForm.name}' is already exists.")
        }

        val now = DateTimeUtil.now()
        return database.insertAndGenerateKey(Staffs) {
            set(it.name, staffForm.name)
            set(it.originName, if(staffForm.originName?.isNotBlank() == true) staffForm.originName else null)
            set(it.remark, if(staffForm.remark?.isNotBlank() == true) staffForm.remark else null)
            set(it.isOrganization, staffForm.isOrganization)
            set(it.occupation, staffForm.occupation)
            set(it.createTime, now)
            set(it.updateTime, now)
            set(it.creator, creator.id)
            set(it.updater, creator.id)
        } as Int
    }

    override fun update(id: Int, staffForm: StaffForm, updater: User) {
        database.sequenceOf(Staffs).find { (it.name eq staffForm.name) and (it.id notEq id) }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Staff with name '${staffForm.name}' is already exists.")
        }
        if(database.update(Staffs) {
            set(it.name, staffForm.name)
            set(it.originName, if(staffForm.originName?.isNotBlank() == true) staffForm.originName else null)
            set(it.remark, if(staffForm.remark?.isNotBlank() == true) staffForm.remark else null)
            set(it.isOrganization, staffForm.isOrganization)
            set(it.occupation, staffForm.occupation)
            set(it.updateTime, DateTimeUtil.now())
            set(it.updater, updater.id)
            where { it.id eq id }
        } == 0) throw NotFoundException("Staff not found.")
    }

    override fun delete(id: Int) {
        if(database.delete(Staffs) { it.id eq id } == 0) throw NotFoundException("Staff not found.")
        database.delete(AnimationStaffRelations) { it.staffId eq id }
    }
}
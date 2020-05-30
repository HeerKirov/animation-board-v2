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
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StaffServiceImpl(@Autowired private val database: Database) : StaffService {
    private val orderTranslator = OrderTranslator {
        "name" to Staffs.name
        "create_time" to Staffs.createTime
        "update_time" to Staffs.updateTime
    }

    override fun list(filter: StaffFilter): ListResult<Staff> {
        return database.from(Staffs).select()
                .whereWithConditions {
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Staffs.name like s) or (Staffs.originName like s) or (Staffs.remark like s)
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
            it.name to staffForm.name
            it.originName to staffForm.originName
            it.remark to staffForm.remark
            it.isOrganization to staffForm.isOrganization
            it.occupation to staffForm.occupation
            it.createTime to now
            it.updateTime to now
            it.creator to creator.id
            it.updater to creator.id
        } as Int
    }

    override fun update(id: Int, staffForm: StaffForm, updater: User) {
        database.sequenceOf(Staffs).find { (it.name eq staffForm.name) and (it.id notEq id) }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Staff with name '${staffForm.name}' is already exists.")
        }
        if(database.update(Staffs) {
            it.name to staffForm.name
            it.originName to staffForm.originName
            it.remark to staffForm.remark
            it.isOrganization to staffForm.isOrganization
            it.occupation to staffForm.occupation
            it.updateTime to DateTimeUtil.now()
            it.updater to updater.id
            where { it.id eq id }
        } == 0) throw NotFoundException("Staff not found.")
    }

    override fun delete(id: Int) {
        if(database.delete(Staffs) { it.id eq id } == 0) throw NotFoundException("Staff not found.")
        database.delete(AnimationStaffRelations) { it.staffId eq id }
    }
}
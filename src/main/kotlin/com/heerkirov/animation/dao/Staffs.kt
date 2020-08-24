package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.StaffOccupation
import com.heerkirov.animation.model.data.Staff
import com.heerkirov.animation.util.ktorm.enum
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Staffs : BaseTable<Staff>("staff") {
    val id = int("id").primaryKey()
    val name = varchar("name")
    val originName = varchar("origin_name")
    val remark = varchar("remark")
    val cover = varchar("cover")
    val isOrganization = boolean("is_organization")
    val occupation = enum("occupation", typeRef<StaffOccupation>())
    val animationCount = int("animation_count")
    val createTime = datetime("create_time")
    val updateTime = datetime("update_time")
    val creator = int("creator")
    val updater = int("updater")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Staff(
            id = row[id]!!,
            name = row[name]!!,
            originName = row[originName],
            remark = row[remark],
            cover = row[cover],
            isOrganization = row[isOrganization]!!,
            occupation = row[occupation],
            animationCount = row[animationCount]!!,
            createTime = row[createTime]!!,
            updateTime = row[updateTime]!!,
            creator = row[creator]!!,
            updater = row[updater]!!
    )
}
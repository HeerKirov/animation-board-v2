package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.StaffOccupation
import com.heerkirov.animation.model.data.Staff
import com.heerkirov.animation.util.ktorm.enum
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.*

object Staffs : BaseTable<Staff>("staff") {
    val id by int("id").primaryKey()
    val name by varchar("name")
    val originName by varchar("origin_name")
    val remark by varchar("remark")
    val cover by varchar("cover")
    val isOrganization by boolean("is_organization")
    val occupation by enum("occupation", typeRef<StaffOccupation>())
    val createTime by datetime("create_time")
    val updateTime by datetime("update_time")
    val creator by int("creator")
    val updater by int("updater")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Staff(
            id = row[id]!!,
            name = row[name]!!,
            originName = row[originName],
            remark = row[remark],
            cover = row[cover],
            isOrganization = row[isOrganization]!!,
            occupation = row[occupation],
            createTime = row[createTime]!!,
            updateTime = row[updateTime]!!,
            creator = row[creator]!!,
            updater = row[updater]!!
    )
}
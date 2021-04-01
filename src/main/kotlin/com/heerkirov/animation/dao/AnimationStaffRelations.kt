package com.heerkirov.animation.dao

import com.heerkirov.animation.enums.StaffTypeInAnimation
import com.heerkirov.animation.model.data.AnimationStaffRelation
import com.heerkirov.animation.util.ktorm.enum
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.BaseTable
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.typeRef

object AnimationStaffRelations : BaseTable<AnimationStaffRelation>("animation_staff_relation") {
    val id = long("id").primaryKey()
    val animationId = int("animation_id")
    val staffId = int("staff_id")
    val staffType = enum("staff_type", typeRef<StaffTypeInAnimation>())

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = AnimationStaffRelation(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!,
            staffType = row[staffType]!!
    )
}
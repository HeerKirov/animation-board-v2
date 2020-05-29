package com.heerkirov.animation.dao

import com.heerkirov.animation.model.AnimationStaffRelation
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.int
import me.liuwj.ktorm.schema.long

object AnimationStaffRelations : BaseTable<AnimationStaffRelation>("animation_staff_relation") {
    val id by long("id").primaryKey()
    val animationId by int("animation_id")
    val staffId by int("staff_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = AnimationStaffRelation(
            id = row[id]!!,
            animationId = row[animationId]!!,
            staffId = row[staffId]!!
    )
}
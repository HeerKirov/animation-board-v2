package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.AnimationTagRelation
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.BaseTable
import org.ktorm.schema.int
import org.ktorm.schema.long

object AnimationTagRelations : BaseTable<AnimationTagRelation>("animation_tag_relation") {
    val id = long("id").primaryKey()
    val animationId = int("animation_id")
    val tagId = int("tag_id")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = AnimationTagRelation(
            id = row[id]!!,
            animationId = row[animationId]!!,
            tagId = row[tagId]!!
    )
}
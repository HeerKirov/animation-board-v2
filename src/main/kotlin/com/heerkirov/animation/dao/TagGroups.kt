package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.TagGroup
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.int
import me.liuwj.ktorm.schema.varchar

object TagGroups : BaseTable<TagGroup>("tag_group") {
    val group = varchar("group").primaryKey()
    val ordinal = int("ordinal")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = TagGroup(
            group = row[group]!!,
            ordinal = row[ordinal]!!
    )
}
package com.heerkirov.animation.dao

import com.heerkirov.animation.model.data.TagGroup
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.BaseTable
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object TagGroups : BaseTable<TagGroup>("tag_group") {
    val group = varchar("group").primaryKey()
    val ordinal = int("ordinal")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = TagGroup(
            group = row[group]!!,
            ordinal = row[ordinal]!!
    )
}
package com.heerkirov.animation.command

import com.heerkirov.animation.dao.TagGroups
import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.model.data.Tag
import com.heerkirov.animation.util.loadProperties
import org.ktorm.database.Database
import org.ktorm.dsl.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * @since 0.4.0
 * [version upgrade]
 * 更新tag和group的ordinal。按照tag的默认ordinal做一次排序，然后排给group-tag结构。
 */
fun main() {
    val properties = loadProperties("application.properties")

    val v2Database = Database.connect(
            url = properties.getProperty("spring.datasource.url"),
            driver = properties.getProperty("spring.datasource.driver-class-name"),
            user = properties.getProperty("spring.datasource.username"),
            password = properties.getProperty("spring.datasource.password")
    )

    v2Database.useTransaction {
        val tags = v2Database.from(Tags)
                .leftJoin(TagGroups, TagGroups.group eq Tags.group)
                .select()
                .orderBy(TagGroups.ordinal.asc(), Tags.ordinal.asc())
                .map { Tags.createEntity(it) }

        val groups = HashMap<String?, MutableList<Tag>>()
        val lists = LinkedList<List<Tag>>()

        for (tag in tags) {
            groups.computeIfAbsent(tag.group) { ArrayList<Tag>().apply { if(it != null) { lists.add(this) } } }.add(tag)
        }
        groups[null]?.apply { lists.add(this) }

        v2Database.deleteAll(TagGroups)
        v2Database.batchInsert(TagGroups) {
            for (i in lists.indices) {
                val group = lists[i].first().group
                if(group != null) {
                    item {
                        set(it.ordinal, i + 1)
                        set(it.group, group)
                    }
                }
            }
        }

        v2Database.batchUpdate(Tags) {
            for (list in lists) {
                for(i in list.indices) {
                    val tag = list[i]
                    item {
                        where { it.id eq tag.id }
                        set(it.ordinal, i + 1)
                    }
                }
            }
        }
    }
}
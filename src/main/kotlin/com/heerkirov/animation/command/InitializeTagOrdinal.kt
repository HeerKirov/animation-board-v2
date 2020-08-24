package com.heerkirov.animation.command

import com.heerkirov.animation.dao.Tags
import com.heerkirov.animation.util.loadProperties
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.batchUpdate
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.update
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.sortedBy
import me.liuwj.ktorm.entity.toList

/**
 * @since 0.3.0
 * [version upgrade]
 * 初始化tag的ordinal。
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
        val tags = v2Database.sequenceOf(Tags).sortedBy { it.id }.toList()
        v2Database.batchUpdate(Tags) {
            var ordinal = 0
            for (tag in tags) {
                item {
                    where { it.id eq tag.id }
                    it.ordinal to ++ordinal
                }
            }
        }
    }
}
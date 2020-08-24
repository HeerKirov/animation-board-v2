package com.heerkirov.animation.command

import com.heerkirov.animation.service.manager.AnimationRelationProcessor
import com.heerkirov.animation.util.loadProperties
import me.liuwj.ktorm.database.Database

/**
 * @since 0.1.0
 * [database util]
 * 刷新全数据库的relation关系。
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
        val num = AnimationRelationProcessor(v2Database).updateAllRelationTopology()
        println("update $num records.")
    }
}
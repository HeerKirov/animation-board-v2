package com.heerkirov.animation.command

import com.heerkirov.animation.util.loadProperties
import com.heerkirov.animation.util.transform.transformV1ToV2
import me.liuwj.ktorm.database.Database

/**
 * 从animation board v1的数据库迁移数据。
 */
fun main(vararg args: String) {
    val properties = loadProperties("application.properties")

    val v1Database = Database.connect(
            url = properties.getProperty("command.transform.v1.url"),
            driver = properties.getProperty("spring.datasource.driver-class-name"),
            user = properties.getProperty("command.transform.v1.username"),
            password = properties.getProperty("command.transform.v1.password")
    )

    val v2Database = Database.connect(
            url = properties.getProperty("spring.datasource.url"),
            driver = properties.getProperty("spring.datasource.driver-class-name"),
            user = properties.getProperty("spring.datasource.username"),
            password = properties.getProperty("spring.datasource.password")
    )


    transformV1ToV2(v1Database, v2Database)
}
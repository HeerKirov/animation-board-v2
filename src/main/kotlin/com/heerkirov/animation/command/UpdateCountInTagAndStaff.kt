package com.heerkirov.animation.command

import com.heerkirov.animation.service.manager.AnimationStaffProcessor
import com.heerkirov.animation.service.manager.AnimationTagProcessor
import com.heerkirov.animation.util.loadProperties
import me.liuwj.ktorm.database.Database

/**
 * @since 0.3.0
 * [version upgrade]
 * 刷新全数据库的tag和staff的count。
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
        AnimationTagProcessor(v2Database).updateAllCount()
        AnimationStaffProcessor(v2Database).updateAllCount()
    }
}
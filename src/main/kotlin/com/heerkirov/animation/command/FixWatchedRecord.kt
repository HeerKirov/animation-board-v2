package com.heerkirov.animation.command

import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.util.loadProperties
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*

/**
 * @since 0.2.2
 * [bug fixes]
 * 修正一项watchedRecord的数据错误。
 * 这个错误是从v1数据库迁移而来的。有一部分数据的watchedRecord填写成了当时条目的创建时间，远大于完成时间，这部分数据在统计时发生了统计偏差，因此要予以修正。
 */
fun main() {
    val properties = loadProperties("application.properties")

    val database = Database.connect(
            url = properties.getProperty("spring.datasource.url"),
            driver = properties.getProperty("spring.datasource.driver-class-name"),
            user = properties.getProperty("spring.datasource.username"),
            password = properties.getProperty("spring.datasource.password")
    )

    database.useTransaction {
        val progresses = database.from(RecordProgresses).select()
                .where { (RecordProgresses.finishTime.isNotNull()) and (RecordProgresses.ordinal eq 1) }
                .orderBy(RecordProgresses.finishTime.asc())
                .map { RecordProgresses.createEntity(it) }

        var cnt = 0
        database.batchUpdate(RecordProgresses) {
            for (progress in progresses) {
                val finishTime = progress.finishTime
                if(progress.watchedRecord.any { it != null && it > finishTime }) {
                    cnt += 1
                    item {
                        where { it.id eq progress.id }
                        it.watchedRecord to progress.watchedRecord.map { t -> if(t != null && t > finishTime) null else t }
                    }
                }
            }
        }
        println("Fix $cnt records.")
    }
}
package com.heerkirov.animation.command

import com.aliyun.oss.OSSClientBuilder
import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.util.loadProperties
import org.ktorm.database.Database
import org.ktorm.dsl.*

/**
 * @since 0.1.0
 * [initialize oss]
 * 从animation board v1的OSS存储迁移数据。
 */
fun main() {
    val properties = loadProperties("application.properties")

    val v1OSS = OSSClientBuilder().build(
            properties.getProperty("command.transform.v1.endpoint"),
            properties.getProperty("command.transform.v1.access-key.id"),
            properties.getProperty("command.transform.v1.access-key.secret")
    )
    val v1Bucket = properties.getProperty("command.transform.v1.bucket-name")

    val v2OSS = OSSClientBuilder().build(
            properties.getProperty("oss.endpoint"),
            properties.getProperty("oss.access-key.id"),
            properties.getProperty("oss.access-key.secret")
    )
    val v2Bucket = properties.getProperty("oss.bucket-name")

    val v2Database = Database.connect(
            url = properties.getProperty("spring.datasource.url"),
            driver = properties.getProperty("spring.datasource.driver-class-name"),
            user = properties.getProperty("spring.datasource.username"),
            password = properties.getProperty("spring.datasource.password")
    )

    val covers = v2Database.from(Animations)
            .select(Animations.cover)
            .where { Animations.cover.isNotNull() }
            .map { it[Animations.cover]!! }

    println("Find ${covers.size} covers need to be transformed.")

    var count = 0
    for (cover in covers) {
        v1OSS.getObject(v1Bucket, cover).objectContent.use {
            v2OSS.putObject(v2Bucket, "cover/animation/$cover", it)
        }
        println("Transformed [$cover].")
        count += 1
    }
    println("Transform complete. $count item was transformed.")
}
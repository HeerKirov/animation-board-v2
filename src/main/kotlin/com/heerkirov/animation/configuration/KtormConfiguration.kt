package com.heerkirov.animation.configuration

import org.ktorm.database.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class KtormConfiguration(@Autowired private val dataSource: DataSource) {

    @Bean
    fun database(): Database {
        return Database.connectWithSpringSupport(dataSource)
    }
}
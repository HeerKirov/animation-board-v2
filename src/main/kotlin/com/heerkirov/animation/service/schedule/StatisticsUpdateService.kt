package com.heerkirov.animation.service.schedule

import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.service.statistics.OverviewManager
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.from
import me.liuwj.ktorm.dsl.select
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StatisticsUpdateService(@Autowired private val database: Database,
                              @Autowired private val overviewManager: OverviewManager) {
    private val log = logger<StatisticsUpdateService>()

    @Scheduled(cron = "\${service.schedule.statistics.overview.cron}")
    @Transactional
    fun updateOverview() {
        log.info("Compute statistics overview.")
        for (user in getUsers()) {
            overviewManager.update(user)
        }
    }

    private fun getUsers(): List<User> {
        return database.from(Users).select().map { Users.createEntity(it) }.filter { it.setting.autoUpdateStatistics }
    }
}
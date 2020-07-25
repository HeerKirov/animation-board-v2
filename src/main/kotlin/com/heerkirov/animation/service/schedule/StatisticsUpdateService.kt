package com.heerkirov.animation.service.schedule

import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.service.statistics.HistoryLineManager
import com.heerkirov.animation.service.statistics.OverviewManager
import com.heerkirov.animation.service.statistics.SeasonManager
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.from
import me.liuwj.ktorm.dsl.select
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate

@Service
class StatisticsUpdateService(@Autowired private val database: Database,
                              @Autowired private val overviewManager: OverviewManager,
                              @Autowired private val seasonManager: SeasonManager,
                              @Autowired private val historyLineManager: HistoryLineManager) {
    private val log = logger<StatisticsUpdateService>()

    @Scheduled(cron = "\${service.schedule.statistics.overview}")
    @Transactional
    fun updateOverview() {
        log.info("Compute statistics overview.")
        for (user in getUsers()) {
            overviewManager.update(user)
        }
    }

    @Scheduled(cron = "\${service.schedule.statistics.season}")
    @Transactional
    fun updateSeason() {
        val now = LocalDate.now()
        when {
            now.dayOfMonth == 1 -> {
                //每月1日，对所有的数据做一次更新
                log.info("Compute statistics full season.")
                for(user in getUsers()) {
                    for(year in 1995..now.year) {
                        for(season in 1..4) {
                            seasonManager.update(user, year, season)
                        }
                    }
                    seasonManager.updateOverview(user)
                }
            }
            now.dayOfWeek == DayOfWeek.MONDAY -> {
                //每周周一时，对本季度、上季度、上上季度做一次更新
                log.info("Compute statistics recent season.")
                for (user in getUsers()) {
                    for(i in 0..2) {
                        val date = now.minusMonths(i.toLong())
                        val year = date.year
                        val season = (date.monthValue - 1) / 3 + 1

                        if(seasonManager.update(user, year, season)) {
                            seasonManager.updateOverview(user, year, season)
                        }
                    }
                }
            }
            else -> {
                //其余时候，只更新本季度
                log.info("Compute statistics current season.")
                for (user in getUsers()) {
                    val year = now.year
                    val season = (now.monthValue - 1) / 3 + 1

                    if(seasonManager.update(user, year, season)) {
                        seasonManager.updateOverview(user, year, season)
                    }
                }
            }
        }
    }

    @Scheduled(cron = "\${service.schedule.statistics.historyline}")
    @Transactional
    fun updateHistoryLine() {
        log.info("Compute statistics overview.")
        for (user in getUsers()) {
            historyLineManager.update(user)
        }
    }

    private fun getUsers(): List<User> {
        return database.from(Users).select().map { Users.createEntity(it) }.filter { it.setting.autoUpdateStatistics }
    }
}
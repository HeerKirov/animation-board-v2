package com.heerkirov.animation.service.impl

import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.HistoryLineRes
import com.heerkirov.animation.model.result.SeasonLineRes
import com.heerkirov.animation.model.result.SeasonOverviewRes
import com.heerkirov.animation.service.StatisticsService
import com.heerkirov.animation.service.statistics.HistoryLineManager
import com.heerkirov.animation.service.statistics.OverviewManager
import com.heerkirov.animation.service.statistics.SeasonManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StatisticsServiceImpl(@Autowired private val overviewManager: OverviewManager,
                            @Autowired private val seasonManager: SeasonManager,
                            @Autowired private val historyLineManager: HistoryLineManager) : StatisticsService {

    override fun getOverview(user: User) = overviewManager.get(user)

    override fun getSeasonOverview(user: User) = seasonManager.getOverview(user)

    override fun getSeasonLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int) = seasonManager.getSeasonLine(user, lowerYear, lowerSeason, upperYear, upperSeason)

    override fun getSeason(user: User, year: Int, season: Int) = seasonManager.get(user, year, season)

    override fun getHistoryLineOverview(user: User) = historyLineManager.getOverview(user)

    override fun getHistoryLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int, aggregateTimeUnit: AggregateTimeUnit) = historyLineManager.get(user, lowerYear, lowerSeason, upperYear, upperSeason, aggregateTimeUnit)

    @Transactional
    override fun updateOverview(user: User) {
        overviewManager.update(user)
    }

    @Transactional
    override fun updateFullSeason(user: User) {
        val now = LocalDate.now()
        for(year in 1995..now.year) {
            for(season in 1..4) {
                seasonManager.update(user, year, season)
            }
        }
        seasonManager.updateOverview(user)
    }

    @Transactional
    override fun updateSeason(user: User, year: Int, season: Int) {
        seasonManager.update(user, year, season, forceGenerate = true)
        seasonManager.updateOverview(user, year, season)
    }

    @Transactional
    override fun updateHistoryLine(user: User) {
        historyLineManager.update(user)
    }
}
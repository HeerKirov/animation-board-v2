package com.heerkirov.animation.service.impl

import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.PeriodOverviewRes
import com.heerkirov.animation.model.result.PeriodRes
import com.heerkirov.animation.service.StatisticsService
import com.heerkirov.animation.service.statistics.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StatisticsServiceImpl(@Autowired private val overviewManager: OverviewManager,
                            @Autowired private val seasonManager: SeasonManager,
                            @Autowired private val historyLineManager: HistoryLineManager,
                            @Autowired private val timelineManager: TimelineManager,
                            @Autowired private val periodManager: PeriodManager) : StatisticsService {

    override fun getOverview(user: User) = overviewManager.get(user)

    override fun getSeasonOverview(user: User) = seasonManager.getOverview(user)

    override fun getSeasonLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int) = seasonManager.getSeasonLine(user, lowerYear, lowerSeason, upperYear, upperSeason)

    override fun getSeason(user: User, year: Int, season: Int) = seasonManager.get(user, year, season)

    override fun getTimelineOverview(user: User) = timelineManager.getOverview(user)

    override fun getTimeline(user: User, lower: LocalDate, upper: LocalDate, aggregateTimeUnit: AggregateTimeUnit) = timelineManager.get(user, lower, upper, aggregateTimeUnit)

    override fun getHistoryLineOverview(user: User) = historyLineManager.getOverview(user)

    override fun getHistoryLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int, aggregateTimeUnit: AggregateTimeUnit) = historyLineManager.get(user, lowerYear, lowerSeason, upperYear, upperSeason, aggregateTimeUnit)

    override fun getPeriodOverview(user: User) = periodManager.getOverview(user)

    override fun getPeriod(user: User, year: Int?) = periodManager.get(user, year)

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
    override fun updateTimeline(user: User) {
        timelineManager.update(user)
    }

    @Transactional
    override fun updateHistoryLine(user: User) {
        historyLineManager.update(user)
    }

    @Transactional
    override fun updatePeriod(user: User) {
        periodManager.update(user)
    }
}
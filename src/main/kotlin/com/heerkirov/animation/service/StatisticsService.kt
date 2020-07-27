package com.heerkirov.animation.service

import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.*
import java.time.LocalDate

interface StatisticsService {
    fun getOverview(user: User): OverviewRes

    fun getSeasonOverview(user: User): SeasonOverviewRes

    fun getSeasonLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int): SeasonLineRes

    fun getSeason(user: User, year: Int, season: Int): SeasonRes

    fun getTimelineOverview(user: User): TimelineOverviewRes

    fun getTimeline(user: User, lower: LocalDate, upper: LocalDate, aggregateTimeUnit: AggregateTimeUnit): TimelineRes

    fun getHistoryLineOverview(user: User): SeasonOverviewRes

    fun getHistoryLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int, aggregateTimeUnit: AggregateTimeUnit): HistoryLineRes

    fun getPeriodOverview(user: User): PeriodOverviewRes

    fun getPeriod(user: User, year: Int? = null): PeriodRes

    fun updateOverview(user: User)

    fun updateFullSeason(user: User)

    fun updateSeason(user: User, year: Int, season: Int)

    fun updateTimeline(user: User)

    fun updateHistoryLine(user: User)

    fun updatePeriod(user: User)
}
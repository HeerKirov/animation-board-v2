package com.heerkirov.animation.service

import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.*

interface StatisticsService {
    fun getOverview(user: User): OverviewRes

    fun getSeasonOverview(user: User): SeasonOverviewRes

    fun getSeasonLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int): SeasonLineRes

    fun getSeason(user: User, year: Int, season: Int): SeasonRes

    fun getHistoryLineOverview(user: User): SeasonOverviewRes

    fun getHistoryLine(user: User, lowerYear: Int, lowerSeason: Int, upperYear: Int, upperSeason: Int, aggregateTimeUnit: AggregateTimeUnit): HistoryLineRes

    fun updateOverview(user: User)

    fun updateFullSeason(user: User)

    fun updateSeason(user: User, year: Int, season: Int)

    fun updateHistoryLine(user: User)
}
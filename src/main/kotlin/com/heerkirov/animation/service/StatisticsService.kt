package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.OverviewRes
import com.heerkirov.animation.model.result.SeasonOverviewRes
import com.heerkirov.animation.model.result.SeasonRes

interface StatisticsService {
    fun getOverview(user: User): OverviewRes

    fun getSeasonOverview(user: User): SeasonOverviewRes

    fun getSeason(user: User, year: Int, season: Int): SeasonRes

    fun updateOverview(user: User)

    fun updateFullSeason(user: User)

    fun updateSeason(user: User, year: Int, season: Int)
}
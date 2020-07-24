package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.OverviewRes
import com.heerkirov.animation.model.result.SeasonOverviewRes
import com.heerkirov.animation.model.result.SeasonRes
import com.heerkirov.animation.service.StatisticsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/statistics")
class StatisticsController(@Autowired private val statisticsService: StatisticsService) {
    @Authorization
    @GetMapping("/overview")
    fun overview(@UserIdentity user: User): OverviewRes {
        return statisticsService.getOverview(user)
    }

    @Authorization
    @PostMapping("/overview")
    fun overviewRefresh(@UserIdentity user: User): OverviewRes {
        statisticsService.updateOverview(user)
        return statisticsService.getOverview(user)
    }

    @Authorization
    @GetMapping("/season")
    fun seasonOverview(@UserIdentity user: User): SeasonOverviewRes {
        return statisticsService.getSeasonOverview(user)
    }

    @Authorization
    @GetMapping("/season/{year}-{season}")
    fun season(@UserIdentity user: User, @PathVariable year: Int, @PathVariable season: Int): SeasonRes {
        if(year < 1995) throw BadRequestException(ErrCode.PARAM_ERROR, "Cannot query data before year 1995.")
        if(season <= 0 || season > 4) throw BadRequestException(ErrCode.PARAM_ERROR, "Season must be [1, 4].")
        return statisticsService.getSeason(user, year, season)
    }

    @Authorization
    @PostMapping("/season")
    fun seasonFullRefresh(@UserIdentity user: User): SeasonOverviewRes {
        statisticsService.updateFullSeason(user)
        return statisticsService.getSeasonOverview(user)
    }

    @Authorization
    @PostMapping("/season/{year}-{season}")
    fun seasonRefresh(@UserIdentity user: User, @PathVariable year: Int, @PathVariable season: Int): SeasonRes {
        if(year < 1995) throw BadRequestException(ErrCode.PARAM_ERROR, "Cannot query data before year 1995.")
        if(season <= 0 || season > 4) throw BadRequestException(ErrCode.PARAM_ERROR, "Season must be [1, 4].")
        statisticsService.updateSeason(user, year, season)
        return statisticsService.getSeason(user, year, season)
    }
}
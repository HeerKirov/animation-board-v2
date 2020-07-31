package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.filter.Query
import com.heerkirov.animation.enums.AggregateTimeUnit
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.LineFilter
import com.heerkirov.animation.model.filter.SeasonLineFilter
import com.heerkirov.animation.model.result.*
import com.heerkirov.animation.service.StatisticsService
import com.heerkirov.animation.util.parseDateMonth
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.lang.Exception
import java.time.LocalDate

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
    @GetMapping("/season/overview")
    fun seasonOverview(@UserIdentity user: User): SeasonOverviewRes {
        return statisticsService.getSeasonOverview(user)
    }

    @Authorization
    @GetMapping("/season/line")
    fun seasonLine(@UserIdentity user: User, @Query filter: SeasonLineFilter): SeasonLineRes {
        if(filter.lower.isNullOrBlank() || filter.upper.isNullOrBlank()) throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'lower' and 'upper' are required.")
        val lower = try { filter.lower.split('-').let { Pair(it[0].toInt(), it[1].toInt()) } }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'lower' must be 'yyyy-S'.")}
        val upper = try { filter.upper.split('-').let { Pair(it[0].toInt(), it[1].toInt()) } }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'upper' must be 'yyyy-S'.")}
        return statisticsService.getSeasonLine(user, lower.first, lower.second, upper.first, upper.second)
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

    @Authorization
    @GetMapping("/timeline/overview")
    fun timelineOverview(@UserIdentity user: User): TimelineOverviewRes {
        return statisticsService.getTimelineOverview(user)
    }

    @Authorization
    @GetMapping("/timeline")
    fun timeline(@UserIdentity user: User, @Query filter: LineFilter): TimelineRes {
        if(filter.aggregateTimeUnit == null) throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'aggregate' is required.")

        if(filter.lower.isNullOrBlank() || filter.upper.isNullOrBlank()) throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'lower' and 'upper' are required.")
        return when (filter.aggregateTimeUnit) {
            AggregateTimeUnit.YEAR -> {
                val lower = try { filter.lower.toInt() }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'lower' must be 'yyyy'.") }
                val upper = try { filter.upper.toInt() }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'upper' must be 'yyyy'.") }

                statisticsService.getTimeline(user, LocalDate.of(lower, 1, 1), LocalDate.of(upper, 12, 1), AggregateTimeUnit.YEAR)
            }
            AggregateTimeUnit.SEASON -> {
                val lower = try { filter.lower.split('-').let { Pair(it[0].toInt(), it[1].toInt()) } }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'lower' must be 'yyyy-S'.") }
                val upper = try { filter.upper.split('-').let { Pair(it[0].toInt(), it[1].toInt()) } }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'upper' must be 'yyyy-S'.") }

                statisticsService.getTimeline(user, LocalDate.of(lower.first, (lower.second - 1) * 3 + 1, 1), LocalDate.of(upper.first, upper.second * 3, 1), AggregateTimeUnit.SEASON)
            }
            AggregateTimeUnit.MONTH -> {
                val lower = filter.lower.parseDateMonth() ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'lower' must be 'yyyy-MM'.")
                val upper = filter.upper.parseDateMonth() ?: throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'upper' must be 'yyyy-MM'.")

                statisticsService.getTimeline(user, lower, upper, AggregateTimeUnit.MONTH)
            }
        }
    }

    @Authorization
    @PostMapping("/timeline")
    fun timelineRefresh(@UserIdentity user: User): TimelineOverviewRes {
        statisticsService.updateTimeline(user)
        return statisticsService.getTimelineOverview(user)
    }

    @Authorization
    @GetMapping("/historyline/overview")
    fun historyLineOverview(@UserIdentity user: User): SeasonOverviewRes {
        return statisticsService.getHistoryLineOverview(user)
    }

    @Authorization
    @GetMapping("/historyline")
    fun historyLine(@UserIdentity user: User, @Query filter: LineFilter): HistoryLineRes {
        if(filter.aggregateTimeUnit == null) throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'aggregate' is required.")
        else if(filter.aggregateTimeUnit == AggregateTimeUnit.MONTH) throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'aggregate' cannot be MONTH.")
        if(filter.lower.isNullOrBlank() || filter.upper.isNullOrBlank()) throw BadRequestException(ErrCode.PARAM_REQUIRED, "Query 'lower' and 'upper' are required.")
        return if(filter.aggregateTimeUnit == AggregateTimeUnit.YEAR) {
            val lower = try { filter.lower.toInt() }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'lower' must be 'yyyy'.")}
            val upper = try { filter.upper.toInt() }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'upper' must be 'yyyy'.")}

            statisticsService.getHistoryLine(user, lower, 1, upper, 4, AggregateTimeUnit.YEAR)
        }else{
            val lower = try { filter.lower.split('-').let { Pair(it[0].toInt(), it[1].toInt()) } }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'lower' must be 'yyyy-S'.")}
            val upper = try { filter.upper.split('-').let { Pair(it[0].toInt(), it[1].toInt()) } }catch (e: Exception) { throw BadRequestException(ErrCode.PARAM_ERROR, "Query 'upper' must be 'yyyy-S'.")}

            statisticsService.getHistoryLine(user, lower.first, lower.second, upper.first, upper.second, AggregateTimeUnit.SEASON)
        }
    }

    @Authorization
    @PostMapping("/historyline")
    fun historyLineRefresh(@UserIdentity user: User): SeasonOverviewRes {
        statisticsService.updateHistoryLine(user)
        return statisticsService.getHistoryLineOverview(user)
    }

    @Authorization
    @GetMapping("/period/overview")
    fun periodOverview(@UserIdentity user: User): PeriodOverviewRes {
        return statisticsService.getPeriodOverview(user)
    }

    @Authorization
    @GetMapping("/period/all")
    fun periodAll(@UserIdentity user: User): PeriodRes {
        return statisticsService.getPeriod(user)
    }

    @Authorization
    @GetMapping("/period/{year}")
    fun period(@UserIdentity user: User, @PathVariable year: Int): PeriodRes {
        if(year < 1995) throw BadRequestException(ErrCode.PARAM_ERROR, "Cannot query data before year 1995.")
        return statisticsService.getPeriod(user, year)
    }

    @Authorization
    @PostMapping("/period")
    fun periodRefresh(@UserIdentity user: User): PeriodOverviewRes {
        statisticsService.updatePeriod(user)
        return statisticsService.getPeriodOverview(user)
    }
}
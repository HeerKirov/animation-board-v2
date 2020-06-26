package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.OverviewRes
import com.heerkirov.animation.service.statistics.OverviewManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/statistics")
class StatisticsController(@Autowired private val overviewManager: OverviewManager) {
    @Authorization
    @GetMapping("/overview")
    fun overview(@UserIdentity user: User): OverviewRes {
        return overviewManager.get(user)
    }

    @Authorization
    @PostMapping("/overview")
    fun overviewRefresh(@UserIdentity user: User): OverviewRes {
        overviewManager.update(user)
        return overviewManager.get(user)
    }
}
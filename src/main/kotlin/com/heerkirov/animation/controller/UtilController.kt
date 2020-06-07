package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.filter.Query
import com.heerkirov.animation.model.filter.TimeZoneFilter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId

@RestController
@RequestMapping("/api/util")
class UtilController {
    private val shortTimeZones = ZoneId.SHORT_IDS.values.filter { it.contains('/') }.sorted().toList() + "UTC"
    private val longTimeZones = ZoneId.getAvailableZoneIds().toList()

    @GetMapping("/timezones")
    fun getTimeZones(@Query form: TimeZoneFilter): List<String> {
        return if(form.usual == true) {
            shortTimeZones
        }else{
            longTimeZones
        }
    }
}
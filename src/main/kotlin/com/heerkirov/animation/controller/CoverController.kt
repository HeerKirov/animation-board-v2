package com.heerkirov.animation.controller

import com.heerkirov.animation.enums.CoverType
import com.heerkirov.animation.service.CoverService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/api/database/cover")
class CoverController(@Autowired private val coverService: CoverService) {
    @GetMapping("/{type}/{file:.+}")
    fun get(@PathVariable type: String, @PathVariable file: String): Any {
        val coverType = CoverType.valueOf(type.toUpperCase())
        return RedirectView(coverService.getCoverURL(coverType, file).toString())
    }
}
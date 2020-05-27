package com.heerkirov.animation.controller

import com.heerkirov.animation.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController(@Autowired private val userService: UserService) {
    @GetMapping("/setting")
    fun getSetting(): Any {
        return userService.get("admin2")
    }
}
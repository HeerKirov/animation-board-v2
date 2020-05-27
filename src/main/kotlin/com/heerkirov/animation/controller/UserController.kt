package com.heerkirov.animation.controller

import com.heerkirov.animation.authorization.Authorization
import com.heerkirov.animation.authorization.UserIdentity
import com.heerkirov.animation.form.SettingForm
import com.heerkirov.animation.form.StaffRes
import com.heerkirov.animation.form.toForm
import com.heerkirov.animation.form.toModel
import com.heerkirov.animation.model.User
import com.heerkirov.animation.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(@Autowired private val userService: UserService) {
    @Authorization
    @GetMapping("/staff")
    fun isStaff(@UserIdentity user: User): Any {
        return StaffRes(user.isStaff)
    }

    @Authorization
    @GetMapping("/setting")
    fun getSetting(@UserIdentity user: User): Any {
        return user.setting.toForm()
    }

    @Authorization
    @PutMapping("/setting")
    fun updateSetting(@UserIdentity user: User, @RequestBody body: SettingForm): Any {
        userService.updateSetting(user.username, body.toModel())
        return userService.get(user.username).setting.toForm()
    }
}
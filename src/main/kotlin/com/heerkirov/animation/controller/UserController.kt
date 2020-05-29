package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.form.SettingForm
import com.heerkirov.animation.form.IsStaffRes
import com.heerkirov.animation.form.toRes
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
        return IsStaffRes(user.isStaff)
    }

    @Authorization
    @GetMapping("/setting")
    fun getSetting(@UserIdentity user: User): Any {
        return user.setting.toRes()
    }

    @Authorization
    @PutMapping("/setting")
    fun updateSetting(@UserIdentity user: User, @Body body: SettingForm): Any {
        userService.updateSetting(user.username, body.toModel())
        return userService.get(user.username).setting.toRes()
    }
}
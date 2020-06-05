package com.heerkirov.animation.controller

import com.heerkirov.animation.aspect.authorization.Authorization
import com.heerkirov.animation.aspect.authorization.UserIdentity
import com.heerkirov.animation.aspect.filter.Query
import com.heerkirov.animation.aspect.validation.Body
import com.heerkirov.animation.model.form.SettingForm
import com.heerkirov.animation.model.form.toModel
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.MessageFilter
import com.heerkirov.animation.model.form.MarkAsReadForm
import com.heerkirov.animation.model.result.IsStaffRes
import com.heerkirov.animation.model.result.MessageRes
import com.heerkirov.animation.model.result.SettingRes
import com.heerkirov.animation.model.result.toRes
import com.heerkirov.animation.service.MessageService
import com.heerkirov.animation.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(@Autowired private val userService: UserService,
                     @Autowired private val messageService: MessageService) {
    @Authorization
    @GetMapping("/staff")
    fun isStaff(@UserIdentity user: User): IsStaffRes {
        return IsStaffRes(user.isStaff)
    }

    @Authorization
    @GetMapping("/setting")
    fun getSetting(@UserIdentity user: User): SettingRes {
        return user.setting.toRes()
    }

    @Authorization
    @PutMapping("/setting")
    fun updateSetting(@UserIdentity user: User, @Body body: SettingForm): SettingRes {
        userService.updateSetting(user.username, body.toModel())
        return userService.get(user.username).setting.toRes()
    }

    @Authorization
    @GetMapping("/messages")
    fun getMessages(@UserIdentity user: User, @Query filter: MessageFilter): List<MessageRes> {
        return messageService.messages(filter, user)
    }

    @Authorization
    @GetMapping("/messages/count")
    fun getMessageCount(@UserIdentity user: User, @Query filter: MessageFilter): Int {
        return messageService.messageCount(filter, user)
    }

    @Authorization
    @PostMapping("/messages/mark-as-read")
    fun markAsRead(@UserIdentity user: User, @Body form: MarkAsReadForm): Int {
        return messageService.markAsRead(form, user)
    }
}
package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.MessageFilter
import com.heerkirov.animation.model.form.MarkAsReadForm
import com.heerkirov.animation.model.result.MessageRes

interface MessageService {
    fun messages(filter: MessageFilter, user: User): List<MessageRes>

    fun messageCount(filter: MessageFilter, user: User): Int

    fun markAsRead(form: MarkAsReadForm, user: User): Int
}
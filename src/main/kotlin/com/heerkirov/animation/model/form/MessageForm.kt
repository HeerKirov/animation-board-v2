package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field

data class MarkAsReadForm(@Field("message_ids") val ids: List<Long>)
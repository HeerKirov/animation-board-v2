package com.heerkirov.animation.model.filter

import com.heerkirov.animation.aspect.filter.Filter

data class MessageFilter(@Filter("from") val from: Long?, @Filter("unread") val unread: Boolean?)
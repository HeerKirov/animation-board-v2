package com.heerkirov.animation.filter

import com.heerkirov.animation.aspect.filter.Limit
import com.heerkirov.animation.aspect.filter.Offset
import com.heerkirov.animation.aspect.filter.Order
import com.heerkirov.animation.aspect.filter.Search

data class TagFilter(@Limit val limit: Int?,
                     @Offset val offset: Int?,
                     @Search val search: String?,
                     @Order(options = ["name", "create_time"], default = "name") val order: String?)
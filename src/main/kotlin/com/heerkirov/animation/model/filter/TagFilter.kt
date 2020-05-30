package com.heerkirov.animation.model.filter

import com.heerkirov.animation.aspect.filter.*

data class TagFilter(@Limit val limit: Int?,
                     @Offset val offset: Int?,
                     @Search val search: String?,
                     @Order(options = ["name", "create_time", "update_time"], default = "create_time") val order: List<Pair<Int, String>>)
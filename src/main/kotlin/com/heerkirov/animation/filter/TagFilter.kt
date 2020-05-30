package com.heerkirov.animation.filter

import com.heerkirov.animation.aspect.filter.*

data class TagFilter(@Limit val limit: Int?,
                     @Offset val offset: Int?,
                     @Search val search: String?,
                     @Order(options = ["name", "create_time"], default = "name") val order: List<Pair<Int, String>>)
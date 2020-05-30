package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.NotBlank

data class TagForm(@NotBlank val name: String,
                   val introduction: String)

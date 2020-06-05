package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.MaxLength
import com.heerkirov.animation.aspect.validation.NotBlank

data class TagForm(@Field("name") @NotBlank @MaxLength(16) val name: String,
                   @Field("introduction") val introduction: String)

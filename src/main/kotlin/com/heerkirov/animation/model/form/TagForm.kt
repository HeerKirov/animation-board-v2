package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.MaxLength
import com.heerkirov.animation.aspect.validation.Min
import com.heerkirov.animation.aspect.validation.NotBlank

data class TagCreateForm(@Field("name") @NotBlank @MaxLength(16) val name: String,
                         @Field("introduction") val introduction: String,
                         @Field("group") @MaxLength(16) val group: String? = null)

data class TagPartialForm(@Field("name") @NotBlank @MaxLength(16) val name: String? = null,
                          @Field("introduction") val introduction: String? = null,
                          @Field("group") @MaxLength(16) val group: String? = null,
                          @Field("ordinal") @Min(1) val ordinal: Int? = null)

data class GroupPartialForm(@Field("group") @NotBlank @MaxLength(16) val group: String? = null,
                            @Field("ordinal") @Min(1) val ordinal: Int? = null)
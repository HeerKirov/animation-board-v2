package com.heerkirov.animation.form

import com.heerkirov.animation.aspect.validation.NotBlank
import com.heerkirov.animation.model.Tag
import com.heerkirov.animation.util.toDateTimeString

data class TagRes(val id: Int, val name: String)

data class TagDetailRes(val id: Int, val name: String, val introduction: String?, val createTime: String, val updateTime: String)

data class TagForm(@NotBlank val name: String,
                   val introduction: String)

fun Tag.toRes() = TagRes(id, name)

fun Tag.toDetailRes() = TagDetailRes(id, name, introduction, createTime.toDateTimeString(), updateTime.toDateTimeString())
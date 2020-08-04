package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.model.data.Tag
import com.heerkirov.animation.util.toDateTimeString

data class TagRes(val id: Int, val name: String)

fun Tag.toRes() = TagRes(id, name)

data class TagListRes(val id: Int, val name: String, val group: String?)

fun Tag.toListRes() = TagListRes(id, name, group)

data class TagDetailRes(val id: Int,
                        val name: String,
                        val introduction: String?,
                        val group: String?,
                        val ordinal: Int,
                        @JsonProperty("animation_count") val animationCount: Int,
                        val createTime: String,
                        val updateTime: String)

fun Tag.toDetailRes() = TagDetailRes(id, name, introduction, group, ordinal, animationCount, createTime.toDateTimeString(), updateTime.toDateTimeString())
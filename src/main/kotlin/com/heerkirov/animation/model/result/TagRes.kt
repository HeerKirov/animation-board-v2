package com.heerkirov.animation.model.result

import com.heerkirov.animation.model.data.Tag
import com.heerkirov.animation.util.toDateTimeString

data class TagRes(val id: Int, val name: String)

data class TagDetailRes(val id: Int, val name: String, val introduction: String?, val createTime: String, val updateTime: String)

fun Tag.toRes() = TagRes(id, name)

fun Tag.toDetailRes() = TagDetailRes(id, name, introduction, createTime.toDateTimeString(), updateTime.toDateTimeString())
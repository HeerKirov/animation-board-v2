package com.heerkirov.animation.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper


fun <T> T.toJSONString(): String = jacksonObjectMapper().writeValueAsString(this)

inline fun <reified T> String.parseJSONObject(): T = jacksonObjectMapper().readValue(this, T::class.java)
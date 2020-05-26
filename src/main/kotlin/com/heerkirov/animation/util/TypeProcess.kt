package com.heerkirov.animation.util

fun Long.mapZeroToNull(): Long? {
    return if(this == 0L) null else this
}
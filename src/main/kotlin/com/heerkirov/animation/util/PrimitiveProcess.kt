package com.heerkirov.animation.util

fun Long.mapZeroToNull(): Long? {
    return if(this == 0L) null else this
}

fun <T, R> Iterator<T>.map (transform: (T) -> R): List<R> {
    val list = arrayListOf<R>()
    this.forEach { list.add(transform(it)) }
    return list
}
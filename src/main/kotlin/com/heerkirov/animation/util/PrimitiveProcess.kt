package com.heerkirov.animation.util

fun <T, R> Iterator<T>.map (transform: (T) -> R): List<R> {
    val list = arrayListOf<R>()
    this.forEach { list.add(transform(it)) }
    return list
}

fun <T> Iterable<Iterable<T>>.reduce(): List<T> {
    val list = arrayListOf<T>()
    for (i in this) {
        list.addAll(i)
    }
    return list
}
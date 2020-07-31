package com.heerkirov.animation.util

import java.util.*
import kotlin.collections.ArrayList

fun <T> arrayListFor(size: Int, builder: (Int) -> T): ArrayList<T> {
    val arr = ArrayList<T>(size)
    for(i in 0 until size) {
        arr += builder(i)
    }
    return arr
}

fun <T: Comparable<T>> stepFor(start: T, end: T, step: (T) -> T): List<T> {
    val list = LinkedList<T>()
    var i = start
    while (i <= end) {
        list += i
        i = step(i)
    }
    return list
}
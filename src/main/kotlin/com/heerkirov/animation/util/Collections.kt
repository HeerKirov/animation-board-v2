package com.heerkirov.animation.util

fun <T> arrayListFor(size: Int, builder: (Int) -> T): ArrayList<T> {
    val arr = ArrayList<T>(size)
    for(i in 0 until size) {
        arr += builder(i)
    }
    return arr
}
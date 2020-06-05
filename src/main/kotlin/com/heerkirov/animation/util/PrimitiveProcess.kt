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

fun <T> Iterable<T>.filterInto(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val trueList = ArrayList<T>()
    val falseList = ArrayList<T>()
    for (element in this) {
        if(predicate(element)) {
            trueList.add(element)
        }else{
            falseList.add(element)
        }
    }
    return Pair(trueList, falseList)
}

fun <T> T.runIf(predicate: Boolean, transform: (T) -> T): T {
    return if(predicate) {
        transform(this)
    }else{
        this
    }
}

fun <T, R: Comparable<R>> Iterable<T>.sortedByOptimally(selector: (T) -> R?): List<T> {
    return this.asSequence().map { Pair(selector(it), it) }.sortedBy { it.first }.map { it.second }.toList()
}

fun <T, R: Comparable<R>> Iterable<T>.sortedByDescendingOptimally(selector: (T) -> R?): List<T> {
    return this.asSequence().map { Pair(selector(it), it) }.sortedByDescending { it.first }.map { it.second }.toList()
}
package com.heerkirov.animation.util.relation

interface IRelation<R> : Comparable<R> {
    fun spread(r: R): R
    fun reverse(): R
}
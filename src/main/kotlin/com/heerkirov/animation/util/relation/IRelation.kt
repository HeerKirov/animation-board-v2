package com.heerkirov.animation.util.relation

interface IRelation<R> {
    operator fun plus(r: R): R
    operator fun unaryMinus(): R
    val level: Int
}
package com.heerkirov.animation.util.ktorm.dsl

import me.liuwj.ktorm.dsl.Query
import me.liuwj.ktorm.dsl.QueryRowSet
import me.liuwj.ktorm.dsl.asIterable

fun Query.first(): QueryRowSet = this.asIterable().first()

fun Query.firstOrNull(): QueryRowSet? = this.asIterable().firstOrNull()

fun Query.asSequence(): Sequence<QueryRowSet> = this.asIterable().asSequence()
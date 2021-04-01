package com.heerkirov.animation.util.ktorm.dsl

import org.ktorm.dsl.Query
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.asIterable

fun Query.first(): QueryRowSet = this.asIterable().first()

fun Query.firstOrNull(): QueryRowSet? = this.asIterable().firstOrNull()

fun Query.asSequence(): Sequence<QueryRowSet> = this.asIterable().asSequence()
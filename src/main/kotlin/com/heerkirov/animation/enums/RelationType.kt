package com.heerkirov.animation.enums

import com.heerkirov.animation.util.relation.IRelation

enum class RelationType(override val level: Int) : IRelation<RelationType> {
    PREV(4),            //前作
    NEXT(4),            //续作
    FANWAI(3),          //番外篇
    MAIN_ARTICLE(3),    //正篇
    RUMOR(2),           //外传
    TRUE_PASS(2),       //正传
    SERIES(1);          //同系列

    override fun unaryMinus(): RelationType {
        return when(this) {
            PREV -> NEXT
            NEXT -> PREV
            FANWAI -> MAIN_ARTICLE
            MAIN_ARTICLE -> FANWAI
            RUMOR -> TRUE_PASS
            TRUE_PASS -> RUMOR
            SERIES -> SERIES
        }
    }

    override fun plus(r: RelationType): RelationType {
        return when {
            this == r -> this
            this == SERIES || r == SERIES || this.level == r.level -> SERIES
            this == RUMOR || r == RUMOR -> RUMOR
            this == TRUE_PASS || r == TRUE_PASS -> TRUE_PASS
            this == PREV || r == PREV -> PREV
            this == NEXT || r == NEXT -> NEXT
            else -> throw UnsupportedOperationException("This case may not occurred.")
        }
    }
}

fun String.toRelationType(): RelationType {
    return RelationType.valueOf(this.toUpperCase())
}
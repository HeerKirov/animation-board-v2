package com.heerkirov.animation.enums

enum class RelationType(val level: Int, private val storage: Boolean = true) {
    PREV(4),            //前作
    NEXT(4),            //续作
    FANWAI(3),          //番外篇
    MAIN_ARTICLE(3),    //正篇
    RUMOR(2),           //外传
    TRUE_PASS(2),       //正传
    SERIES(1),          //同系列
    NONE(0, false),
    DELETED(5, false),
    SELF(6, false);

    val title: String get() = if(storage) name.toLowerCase() else throw RuntimeException("$name cannot be storage and print.")
}
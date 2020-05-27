package com.heerkirov.animation.enums

enum class RecordStatus {
    NO_PROGRESS,        //无进度
    NOT_START,          //有进度，动画放送无计划
    WATCHING,           //观看中
    REWATCHING,         //重看中
    COMPLETED           //完成
}
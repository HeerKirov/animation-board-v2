package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.ActiveEventType
import java.time.LocalDateTime

data class Record(val id: Long,
                  val ownerId: Int,             //所属用户id
                  val animationId: Int,         //关联animation的id
                  val seenOriginal: Boolean,    //看过原作
                  val inDiary: Boolean,         //在日记本中

                  val scatterRecord: List<ScatterRecord>,   //目前不记在观看进度中的观看记录
                  val progressCount: Int,                   //进度数，也就是观看回数

                  val lastActiveTime: LocalDateTime?,       //上次活跃时间
                  val lastActiveEvent: ActiveEvent,         //上次活跃事件

                  val createTime: LocalDateTime,
                  val updateTime: LocalDateTime)

data class ScatterRecord(val episode: Int, val watchedTime: String)

data class ActiveEvent(val type: ActiveEventType, val episode: List<Int>? = null)
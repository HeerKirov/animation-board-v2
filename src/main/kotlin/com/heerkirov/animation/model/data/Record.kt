package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.enums.RecordStatus
import java.time.LocalDateTime

data class Record(val id: Long,
                  val ownerId: Int,             //所属用户id
                  val animationId: Int,         //关联animation的id
                  val seenOriginal: Boolean,    //看过原作
                  val status: RecordStatus,     //与进度有关的当前观看状态
                  val inDiary: Boolean,         //在日记本中

                  val watchedEpisodes: Int,                 //最新进度已观看话数，与最新进度的watchedRecord.size等效
                  val watchedRecord: List<WatchedRecord>,   //目前不记在观看进度中的观看记录
                  val progressCount: Int,                   //进度数，也就是观看回数
                  val latestProgressId: Long?,               //最新一次观看进度的id

                  val subscriptionTime: LocalDateTime?,     //订阅时间(首个观看记录创建时间)
                  val finishTime: LocalDateTime?,           //看完时间(首个观看记录完成时间)
                  val lastActiveTime: LocalDateTime?,       //上次活跃时间
                  val lastActiveEvent: ActiveEvent,         //上次活跃事件

                  val createTime: LocalDateTime,
                  val updateTime: LocalDateTime)

data class WatchedRecord(val episode: Int, val watchedTime: String)

data class ActiveEvent(val type: ActiveEventType, val episode: List<Int>? = null)
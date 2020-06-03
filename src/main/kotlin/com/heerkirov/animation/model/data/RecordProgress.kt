package com.heerkirov.animation.model.data

import java.time.LocalDateTime

data class RecordProgress(val id: Long,
                          val recordId: Long,   //关联的记录的id
                          val ordinal: Int,     //在记录中的顺序号，从1开始
                          val watchedRecord: List<LocalDateTime?>,   //此进度的观看记录。只记录有效的记录，因此没有记录可以为空。实际观看数只对最新进度有效，并记录在record中
                          val startTime: LocalDateTime?,        //此进度的开始时间
                          val finishTime: LocalDateTime?)       //此进度的结束时间
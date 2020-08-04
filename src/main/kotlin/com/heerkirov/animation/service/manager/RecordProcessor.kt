package com.heerkirov.animation.service.manager

import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.util.arrayListFor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecordProcessor {
    /**
     * 计算对外公布的status。
     */
    fun getStatus(progressCount: Int, totalEpisodes: Int, watchedEpisodes: Int?): RecordStatus {
        return when {
            progressCount == 0 -> RecordStatus.NO_PROGRESS
            watchedEpisodes ?: 0 >= totalEpisodes -> RecordStatus.COMPLETED
            progressCount == 1 -> RecordStatus.WATCHING
            else -> RecordStatus.REWATCHING
        }
    }

    /**
     * 计算综合进度百分比。
     */
    fun calculateProgress(progressCount: Int, totalEpisodes: Int, watchedEpisodes: Int?): Double? {
        return if(progressCount == 0 || watchedEpisodes == null) null else progressCount - 1 + watchedEpisodes * 1.0 / totalEpisodes
    }

    /**
     * 计算要写入到数据库的、更新后的progress观看记录表。
     * 首先将旧record补充null，补充到old watched的数量。然后根据new watched，补now。
     * 多余的部分则会被截去。
     */
    fun calculateProgressWatchedRecord(watchedRecord: List<LocalDateTime?>, oldWatchedRecord: Int, newWatchedRecord: Int, now: LocalDateTime): List<LocalDateTime?> {
        val temp = when {
            oldWatchedRecord == watchedRecord.size -> watchedRecord
            oldWatchedRecord > watchedRecord.size -> watchedRecord + arrayListFor(oldWatchedRecord - watchedRecord.size) { null }
            else -> watchedRecord.subList(0, oldWatchedRecord)
        }
        return when {
            newWatchedRecord == temp.size -> temp
            newWatchedRecord > temp.size -> temp + arrayListFor(newWatchedRecord - temp.size) { now }
            else -> temp.subList(0, newWatchedRecord)
        }
    }
}
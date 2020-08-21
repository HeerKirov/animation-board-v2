package com.heerkirov.animation.service.manager

import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.model.result.DiaryItem
import com.heerkirov.animation.util.*
import me.liuwj.ktorm.entity.Tuple4
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoField

@Component
class RecordProcessor {
    val nightTimeTableHourOffset = 2L   //夜晚时间表将0点之后2个小时内的时间视作今天
    private val weekDurationAvailable = 2       //距今天差2周以内的被放入周历表排序

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

    /**
     * 对日记项目进行复杂排序。
     */
    fun sortDiary(items: Sequence<DiaryItem>, direction: Int, order: String, nightTimeTable: Boolean, timezone: ZoneId): Sequence<DiaryItem> {
        val now = DateTimeUtil.now()
        return when(order) {
            "weekly_calendar" -> {
                val zonedNow = now.asZonedTime(timezone)
                items.map {
                    //将next publish plan拆解成几项参数: 完整时间, 周数差，周内时间
                    if(it.nextPublishPlan != null) {
                        val datetime = it.nextPublishPlan
                                .parseDateTime()
                                .asZonedTime(timezone)
                                .runIf(nightTimeTable) { t -> t.minusHours(nightTimeTableHourOffset) }
                        val weekDuration = weekDuration(zonedNow, datetime)
                        val weekday = datetime.dayOfWeek.value
                        val minute = datetime.getLong(ChronoField.MINUTE_OF_DAY)
                        val timeInWeek = weekday * 60 * 24 + minute
                        Tuple4(it, datetime, weekDuration, timeInWeek)
                    }else{
                        Tuple4(it, null, 0, 0L)
                    }
                }.sortedWith(Comparator { (a, aTime, aWeekDuration, aMinute), (b, bTime, bWeekDuration, bMinute) ->
                    if(aTime != null && bTime != null) { //都有更新计划
                        if(aWeekDuration <= weekDurationAvailable && bWeekDuration <= weekDurationAvailable) {  //都在最近2周以内
                            if(aMinute != bMinute) {    //按周内时间排序
                                aMinute.compareTo(bMinute) * direction
                            }else{  //最后按id排序
                                a.animationId.compareTo(b.animationId) * direction
                            }
                        }else if(aWeekDuration > weekDurationAvailable && aWeekDuration > weekDurationAvailable) {  //都在最近两周以外
                            aTime.compareTo(bTime) * direction   //按完整时间排序
                        }else if(aWeekDuration <= weekDurationAvailable) { -direction }else{ direction } //a在以内，那么a优先，且降序时反转
                    }else if(aTime == null && bTime == null) { //都没有更新计划
                        val aWatched = a.watchedEpisodes ?: 0
                        val bWatched = b.watchedEpisodes ?: 0
                        if(aWatched < a.publishedEpisodes && bWatched < b.publishedEpisodes) {    //都有存货
                            a.subscriptionTime.compareTo(b.subscriptionTime) //按订阅时间排序。UTC时间戳可以直接这么比
                        }else if(a.watchedEpisodes ?: 0 >= a.publishedEpisodes && b.watchedEpisodes ?: 0 >= b.publishedEpisodes) {    //都没有存货
                            if(!((a.status == RecordStatus.COMPLETED) xor (b.status == RecordStatus.COMPLETED))) {  //都已完结或都未完结
                                a.animationId.compareTo(b.animationId)  //按id排序
                            }else if(a.status == RecordStatus.COMPLETED) { -1 }else{ 1 }    //a已完结，那么不论direction总是优先，否则就是b优先
                        }else if(a.watchedEpisodes ?: 0 < a.publishedEpisodes) { -1 }else{ 1 }   //a有存货，那么不论direction总是优先，否则就是b优先
                    }else if(aTime != null) { -1 }else{ 1 } //a有更新计划，那么不论direction总是优先，否则就是b优先
                }).map { it.element1 }
            }
            "update_soon" -> {
                items.map {
                    //将下次更新计划时间转换为它对now的时间差，越早的差越小
                    Pair(it, if(it.nextPublishPlan != null) Duration.between(now, it.nextPublishPlan.parseDateTime()).toMinutes() else null)
                }.sortedWith(Comparator { (a, aDuration), (b, bDuration) ->
                    if(aDuration != null && bDuration != null) {    //a和b都有时间差
                        aDuration.compareTo(bDuration) * direction
                    }else if(aDuration == null && bDuration == null) {  //a和b都没有时间差
                        val aWatched = a.watchedEpisodes ?: 0
                        val bWatched = b.watchedEpisodes ?: 0
                        if(aWatched < a.publishedEpisodes && bWatched < b.publishedEpisodes) {    //都有存货
                            a.subscriptionTime.compareTo(b.subscriptionTime) //按订阅时间排序。UTC时间戳可以直接这么比
                        }else if(a.watchedEpisodes ?: 0 >= a.publishedEpisodes && b.watchedEpisodes ?: 0 >= b.publishedEpisodes) {    //都没有存货
                            if(!((a.status == RecordStatus.COMPLETED) xor (b.status == RecordStatus.COMPLETED))) {  //都已完结或都未完结
                                a.animationId.compareTo(b.animationId)  //按id排序
                            }else if(a.status == RecordStatus.COMPLETED) { -1 }else{ 1 }    //a已完结，那么不论direction总是优先，否则就是b优先
                        }else if(a.watchedEpisodes ?: 0 < a.publishedEpisodes) { -1 }else{ 1 }   //a有存货，那么不论direction总是优先，否则就是b优先
                    }else if(aDuration != null) { -1 }else{ 1 } //a有时间差，那么不论direction总是a优先
                }).map { it.first }
            }
            "subscription_time" -> {
                items.map { Pair(it, it.subscriptionTime.parseDateTime()) }.sortedBy { it.second }.map { it.first }
            }
            else -> throw UnsupportedOperationException()
        }
    }
}
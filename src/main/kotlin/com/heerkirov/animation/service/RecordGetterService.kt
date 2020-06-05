package com.heerkirov.animation.service

import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.ActivityFilter
import com.heerkirov.animation.model.filter.DiaryFilter
import com.heerkirov.animation.model.filter.FindFilter
import com.heerkirov.animation.model.filter.HistoryFilter
import com.heerkirov.animation.model.result.*

interface RecordGetterService {
    fun diary(filter: DiaryFilter, user: User): DiaryResult

    fun timetable(user: User): TimetableResult

    fun activity(filter: ActivityFilter, user: User): ListResult<ActivityRes>

    fun history(filter: HistoryFilter, user: User): ListResult<HistoryRes>

    fun find(filter: FindFilter, user: User): ListResult<FindRes>

    fun get(animationId: Int, user: User): RecordDetailRes
}
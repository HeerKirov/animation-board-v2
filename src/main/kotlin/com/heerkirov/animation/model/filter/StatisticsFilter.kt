package com.heerkirov.animation.model.filter

import com.heerkirov.animation.aspect.filter.Filter
import com.heerkirov.animation.enums.AggregateTimeUnit

data class SeasonLineFilter(@Filter("lower") val lower: String?,
                            @Filter("upper") val upper: String?)

data class HistoryLineFilter(@Filter("aggregate") val aggregateTimeUnit: AggregateTimeUnit?,
                             @Filter("lower") val lower: String?,
                             @Filter("upper") val upper: String?)
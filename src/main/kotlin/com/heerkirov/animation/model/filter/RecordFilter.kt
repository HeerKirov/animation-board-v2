package com.heerkirov.animation.model.filter

import com.heerkirov.animation.aspect.filter.*

data class DiaryFilter(@Search val search: String?,
                       @Filter("filter", options = ["active", "watchable", "updating", "completed", "shelve"]) val filter: String?,
                       @Order(options = ["weekly_calendar", "update_soon", "subscription_time"], default = "weekly_calendar") val order: List<Pair<Int, String>>)

data class ActivityFilter(@Limit val limit: Int?,
                          @Offset val offset: Int?,
                          @Search val search: String?)

data class HistoryFilter(@Limit val limit: Int?,
                         @Offset val offset: Int?,
                         @Search val search: String?,
                         @Filter("ordinal", options = ["first", "last"]) val ordinal: String?)

data class FindFilter(@Limit val limit: Int?,
                      @Offset val offset: Int?,
                      @Search val search: String?,
                      @Filter("filter", options = ["not_seen", "recorded", "incomplete"]) val filter: String?,
                      @Order(options = ["publish_time", "create_time", "update_time"], default = "-publish_time") val order: List<Pair<Int, String>>)
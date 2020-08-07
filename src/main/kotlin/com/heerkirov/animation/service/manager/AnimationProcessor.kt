package com.heerkirov.animation.service.manager

import com.heerkirov.animation.util.filterInto
import com.heerkirov.animation.util.runIf
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AnimationProcessor {
    /**
     * 处理每次更新之后的新的totalEpisodes、publishedEpisodes、publishPlan、publishedRecord的值。
     * 返回值依次为publishedEpisodes, publishPlan, publishedRecord.
     */
    fun processQuantityAndPlan(totalEpisodes: Int,
                               newPublishedEpisodes: Int,
                               newPublishPlan: List<LocalDateTime>,
                               oldPublishedRecord: List<LocalDateTime?>,
                               now: LocalDateTime): Triple<Int, List<LocalDateTime>, List<LocalDateTime?>> {
        //计算实际published quantity
        val publishedEpisodes = if (newPublishedEpisodes > totalEpisodes) totalEpisodes else newPublishedEpisodes
        //计算余量
        val remainQuantity = totalEpisodes - publishedEpisodes
        //plan的总量不能超过余量，多余部分会被截去
        val totalPlan = newPublishPlan.runIf(remainQuantity < newPublishPlan.size) {
            it.subList(0, remainQuantity)
        }.sorted()
        //分割已完成计划和未完成计划
        val (publishPlan, publishedPlan) = totalPlan.filterInto { it > now }
        //旧记录的数量如果和旧的publishedEpisodes不匹配，多的截去，少的补null
        val publishedRecord = when {
            oldPublishedRecord.size > publishedEpisodes -> oldPublishedRecord.subList(0, publishedEpisodes)
            oldPublishedRecord.size < publishedEpisodes -> oldPublishedRecord + listOf(*Array<LocalDateTime?>(publishedEpisodes - oldPublishedRecord.size) { null })
            else -> oldPublishedRecord
        }

        //published quantity部分追加已完成计划的数量，同时已完成计划也追加这部分计划
        return Triple(publishedEpisodes + publishedPlan.size, publishPlan, publishedRecord + publishedPlan)
    }
}
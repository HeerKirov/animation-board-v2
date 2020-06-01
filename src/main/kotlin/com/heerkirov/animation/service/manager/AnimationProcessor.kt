package com.heerkirov.animation.service.manager

import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.filterInto
import com.heerkirov.animation.util.runIf
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AnimationProcessor {
    /**
     * 处理每次更新之后的新的sumQuantity、publishedQuantity、publishPlan、publishedRecord的值。
     * 返回值依次为publishedQuantity, publishPlan, publishedRecord.
     */
    fun processQuantityAndPlan(sumQuantity: Int,
                               newPublishedQuantity: Int,
                               newPublishPlan: List<LocalDateTime>,
                               oldPublishRecord: List<LocalDateTime>,
                               now: LocalDateTime): Triple<Int, List<LocalDateTime>, List<LocalDateTime>> {
        //计算实际published quantity
        val publishedQuantity = if (newPublishedQuantity > sumQuantity) sumQuantity else newPublishedQuantity
        //计算余量
        val remainQuantity = sumQuantity - publishedQuantity
        //plan的总量不能超过余量，多余部分会被截去
        val totalPlan = newPublishPlan.runIf(remainQuantity < newPublishPlan.size) {
            it.subList(0, remainQuantity)
        }.sorted()
        //分割已完成计划和未完成计划
        val (publishPlan, publishedPlan) = totalPlan.filterInto { it > now }
        //旧记录如果数量超过published quantity，多余部分也会被截去
        val publishRecord = oldPublishRecord.runIf(oldPublishRecord.size > publishedQuantity) {
            it.subList(0, publishedQuantity)
        }

        //published quantity部分追加已完成计划的数量，同时已完成计划也追加这部分计划
        return Triple(publishedQuantity + publishedPlan.size, publishPlan, publishRecord + publishedPlan)
    }
}
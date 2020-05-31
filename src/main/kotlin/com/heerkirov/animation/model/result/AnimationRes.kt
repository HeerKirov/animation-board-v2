package com.heerkirov.animation.model.result

import com.fasterxml.jackson.annotation.JsonProperty
import com.heerkirov.animation.enums.*
import com.heerkirov.animation.model.data.Animation
import com.heerkirov.animation.util.toDateMonthString
import com.heerkirov.animation.util.toDateTimeString

data class AnimationRes(val id: Int,
                        val title: String,
                        val cover: String?,
                        @JsonProperty("publish_time") val publishTime: String?,
                        @JsonProperty("sum_quantity") val sumQuantity: Int?,
                        @JsonProperty("published_quantity") val publishedQuantity: Int?,
                        @JsonProperty("create_time") val createTime: String,
                        @JsonProperty("update_time") val updateTime: String)

data class AnimationDetailRes(val id: Int,
                              val title: String,
                              @JsonProperty("origin_title") val originTitle: String?,
                              @JsonProperty("other_title") val otherTitle: String?,
                              @JsonProperty("cover") val cover: String?,

                              @JsonProperty("publish_type") val publishType: PublishType?,
                              @JsonProperty("publish_time") val publishTime: String?,
                              val duration: Int?,
                              @JsonProperty("sum_quantity") val sumQuantity: Int?,
                              @JsonProperty("published_quantity") val publishedQuantity: Int?,
                              @JsonProperty("publish_plan") val publishPlan: List<String>,

                              val introduction: String?,
                              val keyword: String?,
                              @JsonProperty("sex_limit_level") val sexLimitLevel: SexLimitLevel?,
                              @JsonProperty("violence_limit_level") val violenceLimitLevel: ViolenceLimitLevel?,
                              @JsonProperty("original_work_type") val originalWorkType: OriginalWorkType?,

                              val tags: List<TagRes>,
                              val staffs: List<StaffRelationRes>,
                              val relations: Map<RelationType, List<Int>>,
                              @JsonProperty("relations_topology") val relationsTopology: List<AnimationRelationRes>,

                              @JsonProperty("create_time") val createTime: String,
                              @JsonProperty("update_time") val updateTime: String)

data class AnimationRelationRes(val id: Int,
                                val title: String,
                                val cover: String?,
                                @JsonProperty("relation_type") val relationType: RelationType)

data class AnimationResult(val animation: Animation, val tags: List<TagRes>, val staffs: List<StaffRelationRes>, val relations: List<AnimationRelationRes>)

fun Animation.toRes(): AnimationRes {
    return AnimationRes(id, title, cover, publishTime?.toDateMonthString(), sumQuantity, publishedQuantity, createTime.toDateTimeString(), updateTime.toDateTimeString())
}

fun AnimationResult.toDetailRes(): AnimationDetailRes {
    return AnimationDetailRes(
            animation.id,
            animation.title,
            animation.originTitle,
            animation.otherTitle,
            animation.cover,
            animation.publishType,
            animation.publishTime?.toDateMonthString(),
            animation.duration, animation.sumQuantity,
            animation.publishedQuantity,
            animation.publishPlan.map { it.toDateTimeString() },
            animation.introduction,
            animation.keyword,
            animation.sexLimitLevel,
            animation.violenceLimitLevel,
            animation.originalWorkType,
            tags,
            staffs,
            animation.relations,
            relations,
            animation.createTime.toDateTimeString(),
            animation.updateTime.toDateTimeString())
}
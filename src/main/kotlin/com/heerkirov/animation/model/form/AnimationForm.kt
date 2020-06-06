package com.heerkirov.animation.model.form

import com.heerkirov.animation.aspect.validation.Field
import com.heerkirov.animation.aspect.validation.MaxLength
import com.heerkirov.animation.aspect.validation.Min
import com.heerkirov.animation.aspect.validation.NotBlank
import com.heerkirov.animation.enums.*
import java.time.LocalDateTime

data class AnimationForm(@Field("title") @NotBlank @MaxLength(128) val title: String,
                         @Field("origin_title") @NotBlank @MaxLength(128) val originTitle: String? = null,
                         @Field("other_title") @NotBlank @MaxLength(128) val otherTitle: String? = null,
                         @Field("introduction") val introduction: String? = null,
                         @Field("keyword") @MaxLength(255) val keyword: String? = null,
                         @Field("sex_limit_level") val sexLimitLevel: SexLimitLevel? = null,
                         @Field("violence_limit_level") val violenceLimitLevel: ViolenceLimitLevel? = null,
                         @Field("original_work_type") val originalWorkType: OriginalWorkType? = null,
                         @Field("publish_type") val publishType: PublishType? = null,
                         @Field("episode_duration") @Min(0) val episodeDuration: Int? = null,
                         @Field("publish_time") val publishTime: String? = null,
                         @Field("total_episodes") @Min(1) val totalEpisodes: Int = 1,
                         @Field("published_episodes") @Min(0) val publishedEpisodes: Int = 0,
                         @Field("publish_plan") val publishPlan: List<LocalDateTime> = emptyList(),
                         @Field("tags") val tags: List<Any> = emptyList(),
                         @Field("staffs") val staffs: Map<StaffTypeInAnimation, List<Int>> = emptyMap(),
                         @Field("relations") val relations: Map<RelationType, List<Int>> = emptyMap())

class AnimationPartialForm(@Field("title") @NotBlank @MaxLength(128) val title: String? = null,
                           @Field("origin_title") @NotBlank @MaxLength(128) val originTitle: String? = null,
                           @Field("other_title") @NotBlank @MaxLength(128) val otherTitle: String? = null,
                           @Field("introduction") val introduction: String? = null,
                           @Field("keyword") @MaxLength(255) val keyword: String? = null,
                           @Field("sex_limit_level") val sexLimitLevel: SexLimitLevel? = null,
                           @Field("violence_limit_level") val violenceLimitLevel: ViolenceLimitLevel? = null,
                           @Field("original_work_type") val originalWorkType: OriginalWorkType? = null,
                           @Field("publish_type") val publishType: PublishType? = null,
                           @Field("episode_duration") @Min(0) val episodeDuration: Int? = null,
                           @Field("publish_time") val publishTime: String? = null,
                           @Field("total_episodes") @Min(1) val totalEpisodes: Int? = null,
                           @Field("published_episodes") @Min(0) val publishedEpisodes: Int? = null,
                           @Field("publish_plan") val publishPlan: List<LocalDateTime>? = null,
                           @Field("tags") val tags: List<Any>? = null,
                           @Field("staffs") val staffs: Map<StaffTypeInAnimation, List<Int>>? = null,
                           @Field("relations") val relations: Map<RelationType, List<Int>>? = null)
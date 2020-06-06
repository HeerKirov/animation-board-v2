package com.heerkirov.animation.util.transform

import java.time.LocalDate
import java.time.LocalDateTime

data class V1Animation(val id: Long,
                       val title: String,
                       val originTitle: String?,
                       val otherTitle: String?,
                       val cover: String?,
                       val originalWorkType: String?,
                       val publishType: String?,
                       val publishTime: LocalDate?,
                       val sumQuantity: Int?,
                       val publishedQuantity: Int?,
                       val duration: Int?,
                       val publishPlan: List<LocalDateTime?>,
                       val publishedRecord: List<LocalDateTime?>,
                       val introduction: String?,
                       val keyword: String?,
                       val limitLevel: String?,
                       val relations: String,
                       val createTime: LocalDateTime,
                       val creator: String,
                       val updateTime: LocalDateTime?,
                       val updater: String?)

data class V1Staff(val id: Long,
                   val name: String,
                   val originName: String?,
                   val remark: String?,
                   val isOrganization: Boolean,
                   val createTime: LocalDateTime,
                   val creator: String,
                   val updateTime: LocalDateTime?,
                   val updater: String?)

data class V1Tag(val id: Long,
                 val name: String,
                 val introduction: String?,
                 val createTime: LocalDateTime,
                 val creator: String,
                 val updateTime: LocalDateTime?,
                 val updater: String?)

data class V1AnimationStaffByAuthor(val id: Long, val animationId: Long, val staffId: Long)

data class V1AnimationStaffByCompany(val id: Long, val animationId: Long, val staffId: Long)

data class V1AnimationStaffBySupervisor(val id: Long, val animationId: Long, val staffId: Long)

data class V1AnimationTag(val id: Long, val animationId: Long, val tagId: Long)

data class V1Comment(val id: Long,
                     val ownerId: Int,
                     val animationId: Long,
                     val score: Int?,
                     val shortComment: String?,
                     val article: String?,
                     val createTime: LocalDateTime,
                     val updateTime: LocalDateTime?)

data class V1Diary(val id: Long,
                   val ownerId: Int,
                   val animationId: Long,
                   val status: String,
                   val watchedQuantity: Int,
                   val watchedRecord: List<LocalDateTime?>,
                   val watchManyTimes: Boolean,
                   val watchOriginalWork: Boolean,
                   val subscriptionTime: LocalDateTime?,
                   val finishTime: LocalDateTime?,
                   val createTime: LocalDateTime,
                   val updateTime: LocalDateTime?)

data class V1User(val id: Int, val isStaff: Boolean)

data class V1Profile(val id: Int,
                     val username: String,
                     val userId: Int,
                     val animationUpdateNotice: Boolean,
                     val nightUpdateMode: Boolean)
package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.*
import java.time.LocalDate
import java.time.LocalDateTime

data class Animation(val id: Int,
                     val title: String,             //标题
                     val originTitle: String?,      //原语言标题
                     val otherTitle: String?,       //其他标题
                     val cover: String?,            //封面文件名

                     val publishType: PublishType?,             //放送类型
                     val publishTime: LocalDate?,               //放送月份
                     val episodeDuration: Int?,                 //平均单话时长
                     val totalEpisodes: Int,                    //总话数
                     val publishedEpisodes: Int,                //已发布话数
                     val publishedRecord: List<LocalDateTime?>,  //已发布话数的发布时间记录
                     val publishPlan: List<LocalDateTime>,      //后续话数的发布计划时间

                     val introduction: String?,                     //介绍文本
                     val keyword: String?,                          //非正式关键字
                     val sexLimitLevel: SexLimitLevel?,             //限制等级(性)
                     val violenceLimitLevel: ViolenceLimitLevel?,   //限制等级(暴力)
                     val originalWorkType: OriginalWorkType?,       //原作类型

                     val relations: Map<RelationType, List<Int>>,           //用户标注的关系模型
                     val relationsTopology: Map<RelationType, List<Int>>,   //推导出的全量关系模型

                     val createTime: LocalDateTime,
                     val updateTime: LocalDateTime,
                     val creator: Int,
                     val updater: Int)
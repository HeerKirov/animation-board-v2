package com.heerkirov.animation.service.schedule

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Messages
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.service.manager.MessageProcessor
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.filterInto
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import kotlin.math.min

@Service
class AnimationUpdateService(@Autowired private val database: Database,
                             @Autowired private val messageProcessor: MessageProcessor) {
    private val log = logger<AnimationUpdateService>()

    /**
     * 执行自动发布动画更新的任务。
     */
    @Scheduled(cron = "\${service.schedule.animation.publish}")
    @Transactional
    fun publishAnimationByPlan() {
        data class Row(val id: Int, val publishedEpisodes: Int, val newPublishedEpisodes: Int, val newPublishPlan: List<LocalDateTime>, val newPublishedRecord: List<LocalDateTime?>)

        val now = DateTimeUtil.now()

        val updatedList = LinkedList<Row>()

        database.from(Animations)
                .select(Animations.id, Animations.publishPlan, Animations.publishedRecord, Animations.publishedEpisodes, Animations.totalEpisodes)
                .where { (Animations.publishedEpisodes less Animations.totalEpisodes) and (Animations.publishPlan notEq emptyList()) }
                .forEach { rowSet ->
                    val publishPlan = rowSet[Animations.publishPlan]!!

                    if(publishPlan.first() <= now) {
                        val oldPublishedRecord = rowSet[Animations.publishedRecord]!!
                        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
                        val totalEpisodes = rowSet[Animations.totalEpisodes]!!

                        val (newPublishPlan, appendPublishedRecord) = publishPlan.filterInto { it > now }
                        val newPublishedEpisodes = min(publishedEpisodes + publishPlan.size - newPublishPlan.size, totalEpisodes)

                        if(newPublishedEpisodes > publishedEpisodes) {
                            val id = rowSet[Animations.id]!!
                            val publishedRecord = when {
                                oldPublishedRecord.size > publishedEpisodes -> oldPublishedRecord.subList(0, publishedEpisodes)
                                oldPublishedRecord.size < publishedEpisodes -> oldPublishedRecord + listOf(*Array<LocalDateTime?>(publishedEpisodes - oldPublishedRecord.size) { null })
                                else -> oldPublishedRecord
                            }

                            updatedList.add(Row(id, publishedEpisodes, newPublishedEpisodes, newPublishPlan, publishedRecord + appendPublishedRecord))
                        }
                    }
                }

        database.batchUpdate(Animations) {
            for ((id, _, publishedEpisodes, publishPlan, publishedRecord) in updatedList) {
                item {
                    where { it.id eq id }
                    it.publishedEpisodes to publishedEpisodes
                    it.publishPlan to publishPlan
                    it.publishedRecord to publishedRecord
                }
            }
        }

        database.batchInsert(Messages) {
            for ((id, oldPublishedEpisodes, publishedEpisodes, _) in updatedList) {
                val users = database.from(Users)
                        .innerJoin(Records, (Records.ownerId eq Users.id) and (Records.animationId eq id))
                        .select(Users.id, Users.username, Users.isStaff, Users.setting)
                        .where { Records.inDiary eq true }
                        .map { Users.createEntity(it) }

                for (user in users) {
                    if(user.setting.animationUpdateNotice) {
                        item(messageProcessor.createPublishMessage(user.id, id, oldPublishedEpisodes, publishedEpisodes, now))
                    }
                }
            }
        }

        if(updatedList.isNotEmpty()) {
            log.info("Animation [${updatedList.map { it.id }.joinToString(", ")}] publish updated.")
        }
    }
}
package com.heerkirov.animation.service.schedule

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Messages
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.dao.Users
import com.heerkirov.animation.service.manager.MessageProcessor
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.Tuple4
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
        val now = DateTimeUtil.now()

        val updatedList = LinkedList<Tuple4<Int, Int, Int, List<LocalDateTime?>>>()

        database.from(Animations)
                .select(Animations.id, Animations.publishPlan, Animations.publishedEpisodes, Animations.totalEpisodes)
                .where { (Animations.publishedEpisodes less Animations.totalEpisodes) and (Animations.publishPlan notEq emptyList()) }
                .forEach { rowSet ->
                    val id = rowSet[Animations.id]!!
                    val publishPlan = rowSet[Animations.publishPlan]!!
                    val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
                    val totalEpisodes = rowSet[Animations.totalEpisodes]!!

                    if(publishPlan.first() <= now) {
                        val newPublishPlan = publishPlan.filter { it > now }
                        val newPublishedEpisodes = min(publishedEpisodes + publishPlan.size - newPublishPlan.size, totalEpisodes)

                        if(newPublishedEpisodes > publishedEpisodes) {
                            updatedList.add(Tuple4(id, publishedEpisodes, newPublishedEpisodes, newPublishPlan))
                        }
                    }
                }

        database.batchUpdate(Animations) {
            for ((id, _, publishedEpisodes, publishPlan) in updatedList) {
                item {
                    where { it.id eq id }
                    it.publishedEpisodes to publishedEpisodes
                    it.publishPlan to publishPlan
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
            log.info("Animation [${updatedList.map { it.element1 }.joinToString(", ")}] publish updated.")
        }
    }
}
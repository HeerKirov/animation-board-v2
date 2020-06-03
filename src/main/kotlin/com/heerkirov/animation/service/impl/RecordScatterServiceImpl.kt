package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.data.WatchedRecord
import com.heerkirov.animation.service.RecordScatterService
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecordScatterServiceImpl(@Autowired private val database: Database) : RecordScatterService {
    @Transactional
    override fun watchScattered(animationId: Int, user: User, episode: Int) {
        val rowSet = database.from(Records).innerJoin(Animations, Animations.id eq Records.animationId)
                .select(Records.id, Records.watchedEpisodes, Records.progressCount, Records.watchedRecord, Animations.totalEpisodes, Animations.publishedEpisodes)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull() ?: throw NotFoundException("Record of animation $animationId not found.")

        val recordId = rowSet[Records.id]!!
        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!

        if(episode > totalEpisodes) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "Episode $episode cannot be greater than total_episodes $totalEpisodes.")
        }else if(episode > publishedEpisodes) {
            throw BadRequestException(ErrCode.INVALID_OPERATION, "Episode $episode is not published.")
        }

        val now = DateTimeUtil.now()

        val watchedRecord = rowSet[Records.watchedRecord]!!
        val newWatchedRecord = watchedRecord + WatchedRecord(episode, now.toDateTimeString())

        database.update(Records) {
            where { it.id eq recordId }
            it.watchedRecord to newWatchedRecord
            it.lastActiveTime to now
            it.lastActiveEvent to ActiveEvent(ActiveEventType.WATCH_EPISODE, listOf(episode))
            it.updateTime to now
        }
    }
}
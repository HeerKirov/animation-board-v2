package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.RecordDetailRes
import com.heerkirov.animation.service.RecordGetterService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RecordGetterServiceImpl(@Autowired private val database: Database,
                              @Autowired private val recordProcessor: RecordProcessor) : RecordGetterService {

    private val recordFields = arrayOf(
            Animations.title, Records.seenOriginal, Records.inDiary, Records.scatterRecord,
            Animations.totalEpisodes, Animations.publishedEpisodes, Records.progressCount,
            Records.createTime, Records.updateTime, RecordProgresses.watchedEpisodes
    )

    override fun get(animationId: Int, user: User): RecordDetailRes {
        val rowSet = database.from(Records)
                .innerJoin(Animations, Records.animationId eq Animations.id)
                .leftJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id) and (RecordProgresses.ordinal eq Records.progressCount))
                .select(*recordFields)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull()
                ?: throw NotFoundException("Record not found.")

        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
        val progressCount = rowSet[Records.progressCount]!!
        val watchedEpisodes = rowSet[RecordProgresses.watchedEpisodes]

        val status = when {
            progressCount == 0 -> RecordStatus.NO_PROGRESS
            watchedEpisodes!! >= totalEpisodes -> RecordStatus.COMPLETED
            progressCount == 1 -> RecordStatus.WATCHING
            else -> RecordStatus.REWATCHING
        }

        return RecordDetailRes(
                animationId = animationId,
                title = rowSet[Animations.title]!!,
                seenOriginal = rowSet[Records.seenOriginal]!!,
                status = status,
                inDiary = rowSet[Records.inDiary]!!,
                totalEpisodes = totalEpisodes,
                publishedEpisodes = publishedEpisodes,
                watchedEpisodes = watchedEpisodes ?: 0,
                progressCount = progressCount,
                createTime = rowSet[Records.createTime]!!.toDateTimeString(),
                updateTime = rowSet[Records.updateTime]!!.toDateTimeString()
        )
    }
}
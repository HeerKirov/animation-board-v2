package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Records
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
            Animations.title, Records.seenOriginal, Records.status, Records.inDiary, Records.watchedRecord,
            Animations.totalEpisodes, Animations.publishedEpisodes, Records.watchedEpisodes, Records.progressCount,
            Records.subscriptionTime, Records.finishTime, Records.createTime, Records.updateTime
    )

    override fun get(animationId: Int, user: User): RecordDetailRes {
        val rowSet = database.from(Records)
                .innerJoin(Animations, Records.animationId eq Animations.id)
                .select(*recordFields)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull()
                ?: throw NotFoundException("Record not found.")

        val totalEpisodes = rowSet[Animations.totalEpisodes]!!
        val publishedEpisodes = rowSet[Animations.publishedEpisodes]!!
        val watchedEpisodes = rowSet[Records.watchedEpisodes]!!
        val watchedRecord = rowSet[Records.watchedRecord]!!
        val progressCount = rowSet[Records.progressCount]!!
        val episodesCount = recordProcessor.calculateEpisodesCount(watchedRecord, progressCount, watchedEpisodes, publishedEpisodes)

        return RecordDetailRes(
                animationId = animationId,
                title = rowSet[Animations.title]!!,
                seenOriginal = rowSet[Records.seenOriginal]!!,
                status = rowSet[Records.status]!!,
                inDiary = rowSet[Records.inDiary]!!,
                totalEpisodes = totalEpisodes,
                publishedEpisodes = publishedEpisodes,
                watchedEpisodes = watchedEpisodes,
                progressCount = progressCount,
                episodesCount = episodesCount,
                subscriptionTime = rowSet[Records.subscriptionTime]?.toDateTimeString(),
                finishTime = rowSet[Records.finishTime]?.toDateTimeString(),
                createTime = rowSet[Records.createTime]!!.toDateTimeString(),
                updateTime = rowSet[Records.updateTime]!!.toDateTimeString()
        )
    }
}
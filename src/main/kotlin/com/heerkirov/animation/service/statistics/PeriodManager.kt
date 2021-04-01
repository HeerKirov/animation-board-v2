package com.heerkirov.animation.service.statistics

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.dao.Statistics
import com.heerkirov.animation.enums.StatisticType
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.PeriodModal
import com.heerkirov.animation.model.data.PeriodOverviewModal
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.result.PeriodOverviewRes
import com.heerkirov.animation.model.result.PeriodRes
import com.heerkirov.animation.model.result.toResWith
import com.heerkirov.animation.util.*
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class PeriodManager(@Autowired private val database: Database) {
    fun getOverview(user: User): PeriodOverviewRes {
        return database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.PERIOD_OVERVIEW) }
                .firstOrNull()
                ?.let { it[Statistics.content]!!.parseJSONObject<PeriodOverviewModal>().toResWith(it[Statistics.updateTime]!!) }
                ?: PeriodOverviewRes(null, null, null)
    }

    fun get(user: User, year: Int?): PeriodRes {
        return database.from(Statistics).select()
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.PERIOD) and (Statistics.key eq (year?.toString() ?: "ALL")) }
                .firstOrNull()
                ?.let { it[Statistics.content]!!.parseJSONObject<PeriodModal>().toResWith(it[Statistics.updateTime]!!) }
                ?: throw NotFoundException("Statistic not found.")
    }

    fun update(user: User) {
        val modal = generate(user)
        val now = DateTimeUtil.now()

        val min = modal.keys.minOrNull()
        val max = modal.keys.maxOrNull()
        val overview = PeriodOverviewModal(min, max)
        val overviewId = database.from(Statistics).select(Statistics.id)
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.PERIOD_OVERVIEW) }
                .firstOrNull()?.get(Statistics.id)
        if(overviewId == null) {
            database.insert(Statistics) {
                set(it.ownerId, user.id)
                set(it.type, StatisticType.PERIOD_OVERVIEW)
                set(it.key, null)
                set(it.content, overview.toJSONString())
                set(it.updateTime, now)
            }
        }else{
            database.update(Statistics) {
                where { it.id eq overviewId }
                set(it.content, overview.toJSONString())
                set(it.updateTime, now)
            }
        }

        val sumModal = generateSumModal(modal.values)
        val sumId = database.from(Statistics).select(Statistics.id)
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.PERIOD) and (Statistics.key eq "ALL") }
                .firstOrNull()?.get(Statistics.id)
        if(sumId == null) {
            database.insert(Statistics) {
                set(it.ownerId, user.id)
                set(it.type, StatisticType.PERIOD)
                set(it.key, "ALL")
                set(it.content, sumModal.toJSONString())
                set(it.updateTime, now)
            }
        }else{
            database.update(Statistics) {
                where { it.id eq sumId }
                set(it.content, sumModal.toJSONString())
                set(it.updateTime, now)
            }
        }

        val currentItems = database.from(Statistics)
                .select(Statistics.key)
                .where { (Statistics.ownerId eq user.id) and (Statistics.type eq StatisticType.PERIOD) and (Statistics.key notEq "ALL") }
                .map { it[Statistics.key]!! }.toSet()
        val modalKeys = modal.keys.map { it.toString() }

        val minus = currentItems - modalKeys
        if(minus.isNotEmpty()) {
            database.delete(Statistics) {
                (it.ownerId eq user.id) and (it.type eq StatisticType.PERIOD) and (it.key inList minus)
            }
        }
        val appends = modalKeys - currentItems
        if(appends.isNotEmpty()) {
            database.batchInsert(Statistics) {
                for (append in appends) {
                    item {
                        set(it.ownerId, user.id)
                        set(it.type, StatisticType.PERIOD)
                        set(it.key, append)
                        set(it.content, modal.getValue(append.toInt()).toJSONString())
                        set(it.updateTime, now)
                    }
                }
            }
        }
        val updates = modalKeys.intersect(currentItems)
        if(updates.isNotEmpty()) {
            database.batchUpdate(Statistics) {
                for (update in updates) {
                    item {
                        where { (it.ownerId eq user.id) and (it.type eq StatisticType.PERIOD) and (it.key eq update) }
                        set(it.content, modal.getValue(update.toInt()).toJSONString())
                        set(it.updateTime, now)
                    }
                }
            }
        }
    }

    fun generate(user: User): Map<Int, PeriodModal> {
        val zone = ZoneId.of(user.setting.timezone)

        val progressSequence = database.from(Animations)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .innerJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id))
                .select(Animations.episodeDuration, RecordProgresses.watchedRecord)
                .asSequence()
                .flatMap {
                    val episodeDuration = it[Animations.episodeDuration] ?: 0
                    it[RecordProgresses.watchedRecord]!!.asSequence()
                            .map { t -> t?.asZonedTime(zone)?.minusMinutes((episodeDuration / 2).toLong()) }
                            .filterNotNull()
                }
        val scatterSequence = database.from(Animations)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .select(Animations.episodeDuration, Records.scatterRecord)
                .asSequence()
                .flatMap {
                    val episodeDuration = it[Animations.episodeDuration] ?: 0
                    it[Records.scatterRecord]!!.asSequence()
                            .map { t -> t.watchedTime.parseDateTime().asZonedTime(zone).minusMinutes((episodeDuration / 2).toLong()) }
                }
        return (progressSequence + scatterSequence)
                .groupBy { it.year }
                .mapValues { (_, items) ->
                    val episodeOfHours = items.groupBy { it.hour }.mapValues { (_, values) -> values.size }
                    val episodeOfWeekdays = items.groupBy { it.dayOfWeek.value }.mapValues { (_, values) -> values.size }
                    val dayOfHours = items.groupBy { it.hour }.mapValues { (_, values) -> values.distinctBy { it.dayOfYear }.count() }
                    val dayOfWeekdays = items.groupBy { it.dayOfWeek.value }.mapValues { (_, values) -> values.distinctBy { it.dayOfYear }.count() }
                    PeriodModal(episodeOfWeekdays, episodeOfHours, dayOfWeekdays, dayOfHours)
                }
    }

    fun generateSumModal(data: Collection<PeriodModal>): PeriodModal {
        val episodeOfHours = data.asSequence()
                .flatMap { it.episodeOfHours.entries.asSequence() }
                .groupBy({ it.key }) { it.value }
                .mapValues { it.value.sum() }
        val episodeOfWeekdays = data.asSequence()
                .flatMap { it.episodeOfWeekdays.entries.asSequence() }
                .groupBy({ it.key }) { it.value }
                .mapValues { it.value.sum() }
        val dayOfHours = data.asSequence()
                .flatMap { it.dayOfHours.entries.asSequence() }
                .groupBy({ it.key }) { it.value }
                .mapValues { it.value.sum() }
        val dayOfWeekdays = data.asSequence()
                .flatMap { it.dayOfWeekdays.entries.asSequence() }
                .groupBy({ it.key }) { it.value }
                .mapValues { it.value.sum() }
        return PeriodModal(episodeOfWeekdays, episodeOfHours, dayOfWeekdays, dayOfHours)
    }
}
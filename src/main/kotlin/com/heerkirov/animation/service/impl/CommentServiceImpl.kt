package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Comments
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.filter.CommentActivityFilter
import com.heerkirov.animation.model.filter.CommentFindFilter
import com.heerkirov.animation.model.filter.RankFilter
import com.heerkirov.animation.model.form.CommentCreateForm
import com.heerkirov.animation.model.form.CommentUpdateForm
import com.heerkirov.animation.model.result.*
import com.heerkirov.animation.service.CommentService
import com.heerkirov.animation.util.*
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.support.postgresql.ilike
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentServiceImpl(@Autowired private val database: Database) : CommentService {
    private val findOrderTranslator = OrderTranslator {
        "finish_time" to RecordProgresses.finishTime
        "publish_time" to Animations.publishTime nulls last
        "create_time" to Animations.createTime
    }

    override fun activity(filter: CommentActivityFilter, user: User): ListResult<CommentRes> {
        return database.from(Comments)
                .innerJoin(Animations, Animations.id eq Comments.animationId)
                .select(Animations.id, Animations.title, Animations.cover,
                        Comments.score, Comments.title, Comments.article, Comments.createTime, Comments.updateTime)
                .whereWithConditions {
                    it += Comments.ownerId eq user.id
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                    if(filter.hasScore == true) it += Comments.score.isNotNull()
                    else if(filter.hasScore == false) it += Comments.score.isNull()
                    if(filter.hasArticle == true) it += Comments.article.isNotNull()
                    else if(filter.hasArticle == false) it += Comments.article.isNull()
                }
                .orderBy(Comments.updateTime.desc())
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { CommentRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Comments.score]!!,
                        it[Comments.title],
                        it[Comments.article],
                        it[Comments.createTime]!!.toDateTimeString(),
                        it[Comments.updateTime]!!.toDateTimeString()
                ) }
    }

    override fun rank(filter: RankFilter, user: User): ListResult<CommentRankRes> {
        return database.from(Comments)
                .innerJoin(Animations, Animations.id eq Comments.animationId)
                .select(Animations.id, Animations.title, Animations.cover, Comments.score)
                .whereWithConditions {
                    it += (Comments.ownerId eq user.id) and Comments.score.isNotNull()
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                    if(filter.minScore != null) it += Comments.score greaterEq filter.minScore
                    if(filter.maxScore != null) it += Comments.score lessEq filter.maxScore
                }
                .orderBy(Comments.score.desc(), Comments.createTime.desc())
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { CommentRankRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Comments.score]!!
                ) }
    }

    override fun find(filter: CommentFindFilter, user: User): ListResult<CommentFindRes> {
        return database.from(Animations)
                .innerJoin(Records, (Records.animationId eq Animations.id) and (Records.ownerId eq user.id))
                .innerJoin(RecordProgresses, (RecordProgresses.recordId eq Records.id) and (RecordProgresses.ordinal eq Records.progressCount))
                .leftJoin(Comments, Comments.animationId eq Animations.id)
                .select(Animations.id, Animations.title, Animations.cover, Animations.publishTime, Animations.createTime, RecordProgresses.finishTime)
                .whereWithConditions {
                    it += Comments.id.isNull()
                    it += (Records.progressCount greater 0) and (RecordProgresses.watchedEpisodes greaterEq Animations.totalEpisodes) and (RecordProgresses.finishTime.isNotNull())
                    if(filter.search != null) {
                        val s = "%${filter.search}%"
                        it += (Animations.title ilike s) or (Animations.originTitle ilike s) or (Animations.otherTitle ilike s)
                    }
                }
                .orderBy(filter.order, findOrderTranslator)
                .limit(filter.offset ?: 0, filter.limit ?: 0)
                .toListResult { CommentFindRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[RecordProgresses.finishTime]!!.toDateTimeString(),
                        it[Animations.publishTime]?.toDateMonthString(),
                        it[Animations.createTime]!!.toDateTimeString()
                ) }
    }

    @Transactional
    override fun create(form: CommentCreateForm, user: User) {
        database.sequenceOf(Comments).find { (it.animationId eq form.animationId) and (it.ownerId eq user.id) }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Comment of animation ${form.animationId} is already exists.")
        }
        if(database.sequenceOf(Animations).find { it.id eq form.animationId } == null) {
            throw BadRequestException(ErrCode.NOT_EXISTS, "Animation ${form.animationId} is not exists.")
        }
        if(form.score == null && form.article.isNullOrBlank()) {
            throw BadRequestException(ErrCode.PARAM_REQUIRED, "Param 'score' or 'article' is required.")
        }

        val now = DateTimeUtil.now()

        database.insert(Comments) {
            it.animationId to form.animationId
            it.ownerId to user.id
            it.score to form.score
            it.title to form.articleTitle
            it.article to form.article
            it.createTime to now
            it.updateTime to now
        }
    }

    override fun get(animationId: Int, user: User): CommentRes {
        return database.from(Comments)
                .innerJoin(Animations, Animations.id eq Comments.animationId)
                .select(Animations.id, Animations.title, Animations.cover,
                        Comments.score, Comments.title, Comments.article, Comments.createTime, Comments.updateTime)
                .where { (Comments.animationId eq animationId) and (Comments.ownerId eq user.id) }
                .firstOrNull()
                ?.let { CommentRes(
                        it[Animations.id]!!,
                        it[Animations.title]!!,
                        it[Animations.cover],
                        it[Comments.score]!!,
                        it[Comments.title],
                        it[Comments.article],
                        it[Comments.createTime]!!.toDateTimeString(),
                        it[Comments.updateTime]!!.toDateTimeString()
                ) }
                ?: throw NotFoundException("Comment not found.")
    }

    @Transactional
    override fun partialUpdate(animationId: Int, form: CommentUpdateForm, user: User) {
        if(database.update(Comments) {
            where { (it.animationId eq animationId) and (it.ownerId eq user.id) }
            if(form.score != null) it.score to form.score
            if(form.articleTitle != null) it.title to form.articleTitle
            if(form.article != null) it.article to form.article
            it.updateTime to DateTimeUtil.now()
        } == 0) throw NotFoundException("Comment of animation $animationId not found.")
    }

    @Transactional
    override fun delete(animationId: Int, user: User) {
        if(database.delete(Comments) { (it.animationId eq animationId) and (it.ownerId eq user.id) } == 0) throw NotFoundException("Comment of animation $animationId not found.")
    }
}
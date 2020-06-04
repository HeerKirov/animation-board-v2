package com.heerkirov.animation.service.impl

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.RecordProgresses
import com.heerkirov.animation.dao.Records
import com.heerkirov.animation.enums.ActiveEventType
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.enums.RecordStatus
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.model.data.ActiveEvent
import com.heerkirov.animation.model.data.User
import com.heerkirov.animation.model.form.RecordCreateForm
import com.heerkirov.animation.model.form.RecordPartialForm
import com.heerkirov.animation.model.result.RecordDetailRes
import com.heerkirov.animation.service.RecordSetterService
import com.heerkirov.animation.service.manager.RecordProcessor
import com.heerkirov.animation.util.DateTimeUtil
import com.heerkirov.animation.util.toDateTimeString
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecordSetterServiceImpl(@Autowired private val database: Database,
                              @Autowired private val recordProcessor: RecordProcessor) : RecordSetterService {

    @Transactional
    override fun create(form: RecordCreateForm, user: User) {
        database.sequenceOf(Records).find { (Records.animationId eq form.animationId) and (Records.ownerId eq user.id) }?.run {
            throw BadRequestException(ErrCode.ALREADY_EXISTS, "Record of animation ${form.animationId} is already exists.")
        }
        if(database.sequenceOf(Animations).find { Animations.id eq form.animationId } == null) {
            throw BadRequestException(ErrCode.NOT_EXISTS, "Animation ${form.animationId} is not exists.")
        }

        //创建进度模型并回存到记录表
        when (form.createType) {
            RecordCreateForm.CreateType.SUBSCRIBE -> recordProcessor.createSubscribe(form, user)
            RecordCreateForm.CreateType.SUPPLEMENT -> recordProcessor.createSupplement(form, user)
            RecordCreateForm.CreateType.RECORD -> recordProcessor.createRecord(form, user)
        }
    }

    @Transactional
    override fun partialUpdate(animationId: Int, form: RecordPartialForm, user: User) {
        val record = database.sequenceOf(Records).find { (it.animationId eq animationId) and (it.ownerId eq user.id) }
                ?: throw NotFoundException("Record of animation $animationId not found.")

        val now = DateTimeUtil.now()

        database.update(Records) {
            where { it.id eq record.id }
            if(form.seenOriginal != null) it.seenOriginal to form.seenOriginal
            if(form.inDiary != null) it.inDiary to form.inDiary
            it.updateTime to now
        }
    }

    @Transactional
    override fun delete(animationId: Int, user: User) {
        val id = database.from(Records).select(Records.id)
                .where { (Records.animationId eq animationId) and (Records.ownerId eq user.id) }
                .firstOrNull()?.get(Records.id) ?: throw NotFoundException("Record of animation $animationId not found.")
        database.delete(Records) { it.id eq id }
        database.delete(RecordProgresses) { it.recordId eq id }
    }
}
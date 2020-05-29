package com.heerkirov.animation.service.impl

import com.heerkirov.animation.service.AnimationService
import me.liuwj.ktorm.database.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AnimationServiceImpl(@Autowired private val database: Database) : AnimationService {

}
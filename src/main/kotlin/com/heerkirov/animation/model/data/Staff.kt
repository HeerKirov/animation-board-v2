package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.StaffOccupation
import java.time.LocalDateTime

data class Staff(val id: Int,
                 val name: String,
                 val originName: String?,
                 val remark: String?,
                 val cover: String?,
                 val isOrganization: Boolean,
                 val occupation: StaffOccupation?,
                 val createTime: LocalDateTime,
                 val updateTime: LocalDateTime,
                 val creator: Int,
                 val updater: Int)
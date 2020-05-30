package com.heerkirov.animation.model.filter

import com.heerkirov.animation.aspect.filter.*
import com.heerkirov.animation.enums.StaffOccupation

data class StaffFilter(@Limit val limit: Int?,
                       @Offset val offset: Int?,
                       @Search val search: String?,
                       @Order(options = ["name", "create_time", "update_time"], default = "-create_time") val order: List<Pair<Int, String>>,
                       @Filter("is_organization") val isOrganization: Boolean?,
                       @Filter("occupation") val occupation: StaffOccupation?)
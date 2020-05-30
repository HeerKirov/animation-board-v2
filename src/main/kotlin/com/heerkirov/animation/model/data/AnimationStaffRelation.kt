package com.heerkirov.animation.model.data

import com.heerkirov.animation.enums.StaffTypeInAnimation

data class AnimationStaffRelation(val id: Long,
                                  val animationId: Int,
                                  val staffId: Int,
                                  val staffType: StaffTypeInAnimation)
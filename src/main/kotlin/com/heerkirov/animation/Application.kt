package com.heerkirov.animation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class AnimationBoardV2Application

fun main(args: Array<String>) {
	runApplication<AnimationBoardV2Application>(*args)
}

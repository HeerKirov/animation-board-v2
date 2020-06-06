package com.heerkirov.animation.util

import com.heerkirov.animation.AnimationBoardV2Application
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*

fun loadProperties(filename: String): Properties {
    val properties = Properties()
    try {
        AnimationBoardV2Application::class.java.getResourceAsStream("/$filename").use { inputStream ->
            properties.load(inputStream)
        }
    }catch (e: NullPointerException) {
        //ignore
    }
    try {
        FileInputStream(filename).use { inputStream ->
            properties.load(inputStream)
        }
    }catch (e: FileNotFoundException) {
        //ignore
    }
    return properties
}
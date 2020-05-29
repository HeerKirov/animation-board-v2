package com.heerkirov.animation.service

import com.heerkirov.animation.enums.CoverType
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.net.URL

interface CoverService {
    fun uploadCover(type: CoverType, id: Int, srcFile: MultipartFile, inputStream: InputStream): String

    fun getCover(type: CoverType, id: Int): URL

    fun getCoverURL(type: CoverType, filename: String): URL
}
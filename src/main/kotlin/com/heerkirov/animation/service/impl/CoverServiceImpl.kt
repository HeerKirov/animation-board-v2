package com.heerkirov.animation.service.impl

import com.aliyun.oss.OSS
import com.heerkirov.animation.component.ImageMagick
import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.dao.Staffs
import com.heerkirov.animation.enums.CoverType
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.exception.InternalException
import com.heerkirov.animation.exception.NotFoundException
import com.heerkirov.animation.service.CoverService
import com.heerkirov.animation.util.logger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.net.URL
import java.util.*
import kotlin.random.Random

@Service
class CoverServiceImpl(@Autowired private val oss: OSS,
                       @Autowired private val database: Database,
                       @Autowired private val imageMagick: ImageMagick,
                       @Value("\${oss.bucket-name}") private val bucket: String,
                       @Value("\${oss.presigned-duration}") private val presignedDuration: Long) : CoverService {
    private val log = logger<CoverServiceImpl>()

    private val rand = Random(System.currentTimeMillis() % (1000 * 60 * 60 * 24))

    private val validContentType = arrayOf("image/jpeg", "image/png")

    private val validExtension = arrayOf("png", "jpg", "jpeg")

    @Transactional
    override fun uploadCover(type: CoverType, id: Int, srcFile: MultipartFile, inputStream: InputStream): String {
        if(srcFile.isEmpty) throw BadRequestException(ErrCode.PARAM_REQUIRED, "File is empty. Please upload cover by param 'file'.")
        if(!validContentType.contains(srcFile.contentType)) throw BadRequestException(ErrCode.PARAM_ERROR, "File content type must be image/png or image/jpeg.")
        if(srcFile.originalFilename.isNullOrBlank()) throw BadRequestException(ErrCode.PARAM_ERROR, "Filename of file cannot be empty.")

        val extension = getExtension(srcFile.originalFilename!!)
        if(extension.isBlank()) throw BadRequestException(ErrCode.PARAM_ERROR, "Extension of filename cannot be empty.")
        if(!validExtension.contains(extension)) throw BadRequestException(ErrCode.PARAM_ERROR, "Extension of filename must be png or jpg or jpeg.")

        val filename = newFilename(id, extension)
        when(type) {
            CoverType.ANIMATION -> if(database.update(Animations) {
                it.cover to filename
                where { it.id eq id }
            } == 0) throw NotFoundException("Animation not found.")
            CoverType.STAFF -> if(database.update(Staffs) {
                it.cover to filename
                where { it.id eq id }
            } == 0) throw NotFoundException("Staff not found.")
        }
        try {
            imageMagick.process(extension, inputStream) {
                oss.putObject(bucket, url(type, filename), this)
            }
        }catch (e: Throwable) {
            log.error("Error occurred in image magick processing. ", e)
            throw InternalException(e.message)
        }
        return filename
    }

    override fun getCover(type: CoverType, id: Int): URL {
        return when(type) {
            CoverType.ANIMATION -> {
                val cover = database.from(Animations).select(Animations.cover)
                        .where { Animations.id eq id }
                        .firstOrNull()?.get(Animations.cover) ?: throw NotFoundException("Animation not found.")
                getCoverURL(CoverType.ANIMATION, cover)
            }
            CoverType.STAFF -> {
                val cover = database.from(Staffs).select(Staffs.cover)
                        .where { Staffs.id eq id }
                        .firstOrNull()?.get(Staffs.cover) ?: throw NotFoundException("Staff not found.")
                getCoverURL(CoverType.STAFF, cover)
            }
        }
    }

    override fun getCoverURL(type: CoverType, filename: String): URL {
        return oss.generatePresignedUrl(bucket, url(type, filename), Date(Date().time + presignedDuration)) ?: throw NotFoundException("Not found.")
    }

    private fun url(type: CoverType, filename: String) = "cover/${type.name.toLowerCase()}/$filename"

    private fun newFilename(id: Int, extension: String): String {
        return String.format("%s.%013d.%03d.%s", id, System.currentTimeMillis(), rand.nextInt(), extension)
    }

    private fun getExtension(filename: String): String {
        val i = filename.lastIndexOf('.')
        return if(i >= 0) {
            filename.substring(i + 1)
        }else{
            filename
        }
    }
}
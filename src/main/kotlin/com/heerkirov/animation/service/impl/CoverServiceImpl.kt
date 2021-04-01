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
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.NoSuchElementException
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

    private val urlCache = ConcurrentHashMap<String, URLCache>()

    @Transactional
    override fun uploadCover(type: CoverType, id: Int, srcFile: MultipartFile, inputStream: InputStream): String {
        if(srcFile.isEmpty) throw BadRequestException(ErrCode.PARAM_REQUIRED, "File is empty. Please upload cover = param 'file'.")
        if(!validContentType.contains(srcFile.contentType)) throw BadRequestException(ErrCode.PARAM_ERROR, "File content type must be image/png or image/jpeg.")
        if(srcFile.originalFilename.isNullOrBlank()) throw BadRequestException(ErrCode.PARAM_ERROR, "Filename of file cannot be empty.")

        val extension = getExtension(srcFile.originalFilename!!)
        if(extension.isBlank()) throw BadRequestException(ErrCode.PARAM_ERROR, "Extension of filename cannot be empty.")
        if(!validExtension.contains(extension)) throw BadRequestException(ErrCode.PARAM_ERROR, "Extension of filename must be png or jpg or jpeg.")

        val filename = newFilename(id)
        val oldFilename = when(type) {  //查出旧的cover filename，并且更新成新的filename
            CoverType.ANIMATION -> {
                val oldCover = try {
                    database.from(Animations).select(Animations.cover).where { Animations.id eq id }.first()[Animations.cover]
                }catch (e: NoSuchElementException) {
                    throw NotFoundException("Animation not found.")
                }
                database.update(Animations) {
                    set(it.cover, filename)
                    where { it.id eq id }
                }
                oldCover
            }
            CoverType.STAFF -> {
                val oldCover = try {
                    database.from(Staffs).select(Staffs.cover).where { Staffs.id eq id }.first()[Staffs.cover]
                }catch (e: NoSuchElementException) {
                    throw NotFoundException("Staff not found.")
                }
                database.update(Staffs) {
                    set(it.cover, filename)
                    where { it.id eq id }
                }
                oldCover
            }
        }
        try {
            imageMagick.process(extension, inputStream) {
                oss.putObject(bucket, url(type, filename), this)
            }
        }catch (e: Throwable) {
            log.error("Error occurred in image magick processing. ", e)
            throw InternalException(e.message)
        }
        if(oldFilename != null) {
            //如果put失败，会引发数据库回滚；而如果delete在那之前，会因无法回滚外部操作导致异常。因此旧file的删除放在不会中断事务的最后。
            try {
                oss.deleteObject(bucket, url(type, oldFilename))
            }catch (e: Exception) {
                //并且因为删除操作不重要，用户不需要感知，将异常捕获写入log即可。抛出异常还可能引发回滚导致再度异常。
                log.error("Error occurred while deleting oss object. ", e)
            }
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
        val key = url(type, filename)
        return urlCache.compute(key) { _, cache ->
            val now = Date().time
            if(cache == null || cache.expireTime <= now) {
                URLCache(oss.generatePresignedUrl(bucket, key, Date(now + presignedDuration)), now + presignedDuration)
            }else{
                cache
            }
        }?.url ?: throw NotFoundException("Not found.")
    }

    private fun url(type: CoverType, filename: String) = "cover/${type.name.toLowerCase()}/$filename"

    private fun newFilename(id: Int): String {
        /* 文件名命名规约：
         * - 文件名由4个部分点隔组成。
         * - 第一部分是依附的对象的id。
         * - 第二部分是文件上传时的当前时间戳。
         * - 第三部分是一个[0, 1000)范围的随机数。
         * - 最后是扩展名，所有文件的扩展名都锁定为jpg。
         */
        return String.format("%s.%013d.%03d.%s", id, System.currentTimeMillis(), rand.nextInt(1000), "jpg")
    }

    private fun getExtension(filename: String): String {
        val i = filename.lastIndexOf('.')
        return if(i >= 0) {
            filename.substring(i + 1)
        }else{
            filename
        }
    }

    private data class URLCache(val url: URL, val expireTime: Long)
}
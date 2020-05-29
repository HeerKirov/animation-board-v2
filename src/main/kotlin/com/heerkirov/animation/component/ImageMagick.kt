package com.heerkirov.animation.component

import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.*
import kotlin.math.min

@Component
class ImageMagick(@Value("\${image.convert.size}") private val size: Int) {
    /**
     * 将输入流的图片保存到本地临时位置，进行处理，然后返回一个文件。
     * - 图片将以中点为重心切割成方形。
     * - 切割后，如果图片边长超过{size}，则将其缩小到size大小。
     * - 图片最终转换为jpg格式
     * 文件处理的lambda结束后，自动清除本地临时文件。
     */
    fun process(extension: String, src: InputStream, outputHandler: File.() -> Unit) {
        val tempFile = File.createTempFile("temp-cover", ".$extension")
        val tempOutputFile = File.createTempFile("temp-cover-output", ".jpg")

        try {
            src.use { inputStream ->
                FileOutputStream(tempFile).use { fos ->
                    val b = ByteArray(1024)
                    while (inputStream.read(b) >= 0) {
                        fos.write(b)
                    }
                }
            }

            val (originWidth, originHeight) = getImageSize(tempFile.absolutePath)
            val crop = getCropSize(originWidth, originHeight)
            val resize = if(min(originWidth, originHeight) > size) size else null

            transform(tempFile.absolutePath, tempOutputFile.absolutePath, crop, resize)

            outputHandler(tempOutputFile)
        }finally{
            tempFile.delete()
            tempOutputFile.delete()
        }
    }

    private fun transform(inFile: String, outFile: String, cropNum: Int?, resizeNum: Int?) {
        val cmd = ConvertCmd()
        cmd.run(IMOperation().apply {
            addImage(inFile)
            if(cropNum != null) {
                gravity("center")
                crop(cropNum, cropNum, 0, 0)
            }
            if(resizeNum != null) {
                resize(resizeNum, resizeNum)
            }
            addImage(outFile)
        })
    }

    private fun getImageSize(file: String): Pair<Int, Int> {
        val output = ArrayList<Int>(2)
        val cmd = ConvertCmd()
        cmd.setOutputConsumer { inputStream ->
            InputStreamReader(inputStream).use { isr ->
                BufferedReader(isr).use { br ->
                    br.lines().findFirst().get()
                        .split('*', ignoreCase = false, limit = 2)
                        .map { it.toInt() }
                        .let { output.addAll(it) }
                }
            }
        }
        cmd.run(IMOperation().apply {
            addImage(file)
            print("%w*%h")
            addImage("/dev/null")
        })
        val (w, h) = output
        return Pair(w, h)
    }

    private fun getCropSize(width: Int, height: Int): Int? {
        return when {
            width > height -> height
            width < height -> width
            else -> null
        }
    }
}
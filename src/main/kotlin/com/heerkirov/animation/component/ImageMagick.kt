package com.heerkirov.animation.component

import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.*
import kotlin.math.min

@Component
class ImageMagick(@Value("\${image.convert.size}") private val size: Int) {
    fun process(extension: String, src: InputStream, outputHandler: File.() -> Unit) {
        val tempFile = File.createTempFile("temp-cover", ".$extension")
        val tempOutputFile = File.createTempFile("temp-cover-output", ".$extension")

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

            doCropAndResize(tempFile.absolutePath, tempOutputFile.absolutePath, crop, resize)

            outputHandler(tempOutputFile)
        }finally{
            tempFile.delete()
            tempOutputFile.delete()
        }
    }

    private fun doCropAndResize(inFile: String, outFile: String, crop: Array<Int>?, resize: Int?) {
        val cmd = ConvertCmd()
        cmd.run(IMOperation().apply {
            addImage(inFile)
            if(crop != null) {
                val (w, h, l ,t) = crop
                gravity("center")
                crop(w, h, l, t)
            }
            if(resize != null) {
                resize(resize, resize)
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

    private fun getCropSize(width: Int, height: Int): Array<Int>? {
        return when {
            width > height -> arrayOf(height, height, (width - height) / 2, 0)
            width < height -> arrayOf(width, width, 0, (height - width) / 2)
            else -> null
        }
    }
}
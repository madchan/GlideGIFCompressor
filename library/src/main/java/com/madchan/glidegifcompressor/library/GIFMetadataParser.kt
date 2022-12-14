package com.madchan.glidegifcompressor.library

import android.net.Uri
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.util.ByteBufferUtil
import java.io.File
import java.nio.ByteBuffer

/**
 * GIF元数据解析器
 */
class GIFMetadataParser {

    lateinit var gifHeader: GifHeader
    lateinit var gifData : ByteBuffer

    fun parse(source: Uri): GIFMetadata {
        val file = File(source.path)

        gifData = ByteBufferUtil.fromFile(file)
        gifHeader = parseHeader(gifData)

        val duration = getDuration(gifHeader)

        return GIFMetadata(
            width = gifHeader.width,
            height = gifHeader.height,
            frameCount = gifHeader.numFrames,
            duration = getDuration(gifHeader),
            frameRate = getFrameRate(gifHeader.numFrames, duration),
            gctSize = getGctSize(gifHeader),
            fileSize = file.length()
        )
    }

    /**
     * 解析GIF文件头
     */
    private fun parseHeader(data: ByteBuffer): GifHeader {
        return GifHeaderParser().apply { setData(data) }.parseHeader()
    }

    private fun getFrameRate(frameCount: Int, duration: Long): Int {
        val durationSeconds = duration / 1000.0
        return Math.round(frameCount / durationSeconds).toInt()
    }

    private fun getDuration(header: GifHeader): Long {
        var duration = 0L
        try {
            val framesField = GifHeader::class.java.getDeclaredField("frames")
            framesField.isAccessible = true
            val frames = framesField[header] as List<*>
            val frameClass = Class.forName("com.bumptech.glide.gifdecoder.GifFrame")
            val frameDelayField = frameClass.getDeclaredField("delay")
            frameDelayField.isAccessible = true
            for (frame in frames) {
                duration += frameDelayField.getInt(frame)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return duration
    }

    private fun getGctSize(header: GifHeader): Int {
        var gctSize: Int
        try {
            val gctSizeField = GifHeader::class.java.getDeclaredField("gctSize")
            gctSizeField.isAccessible = true
            gctSize = gctSizeField[header] as Int
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return gctSize
    }
}
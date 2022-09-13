package com.madchan.glidegifcompressor.library

import android.net.Uri
import android.util.Log
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.util.ByteBufferUtil
import java.io.File
import java.nio.ByteBuffer

class GifInfoParser {

    fun parse(source: Uri): GifInfo {
        val file = File(source.path)
        val dataSource = ByteBufferUtil.fromFile(file)

        val header = parseHeader(dataSource)
        Log.i(CompressTask.TAG, "Parse header successfully: width = ${header.width}, height = ${header.height}, frameCount = ${header.numFrames}")

        val duration = getDuration(header)
        val inputFrameRate = getFramePerSecond(header.numFrames, duration)

        return GifInfo(
            dataSource = dataSource,
            filePath = source.path,
            header = header,
            duration = duration,
            inputFrameRate = inputFrameRate,
            fileSize = file.length()
        )
    }

    private fun parseHeader(dataSource: ByteBuffer): GifHeader {
        return GifHeaderParser().apply { setData(dataSource) }.parseHeader()
    }

    private fun getFramePerSecond(frameCount: Int, duration: Long): Int {
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
            val frameDisposeField = frameClass.getDeclaredField("dispose")
            val frameTransparencyField = frameClass.getDeclaredField("transparency")
            frameDelayField.isAccessible = true
            frameDisposeField.isAccessible = true
            frameTransparencyField.isAccessible = true
            for (frame in frames) {
                duration += frameDelayField.getInt(frame)
                Log.d("TAG", "dispose = ${frameDisposeField.getInt(frame)}, transparency = ${frameTransparencyField.getBoolean(frame)}")
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return duration
    }
}
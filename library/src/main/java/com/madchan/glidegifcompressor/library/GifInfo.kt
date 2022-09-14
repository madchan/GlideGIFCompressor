package com.madchan.glidegifcompressor.library

import com.bumptech.glide.gifdecoder.GifHeader
import java.nio.ByteBuffer

class GifInfo(
    val dataSource: ByteBuffer,
    val filePath: String?,
    val header: GifHeader,
    val duration: Long,
    val inputFrameRate: Int,
    val gctSize: Int,
    val fileSize: Long
){
    fun getWidth() = header.width

    fun getHeight() = header.height

    fun getFrameCount() = header.numFrames

}



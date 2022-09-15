package com.madchan.glidegifcompressor.library

import com.bumptech.glide.gifdecoder.GifHeader
import java.nio.ByteBuffer

/**
 * GIF元数据
 */
class GIFMetadata(
    val dataSource: ByteBuffer,
    /** 头部信息 */
    val header: GifHeader,
    /** 时长 */
    val duration: Long,
    /** 帧率 */
    val frameRate: Int,
    /** 全局调色盘大小 */
    val gctSize: Int,
    /** 文件大小 */
    val fileSize: Long
){
    fun getWidth() = header.width

    fun getHeight() = header.height

    fun getFrameCount() = header.numFrames
}



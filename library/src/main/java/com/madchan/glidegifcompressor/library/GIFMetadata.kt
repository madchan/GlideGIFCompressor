package com.madchan.glidegifcompressor.library

/**
 * GIF元数据
 */
class GIFMetadata(
    /** 宽度 */
    val width: Int,
    /** 高度 */
    val height: Int,
    /** 帧数 */
    val frameCount: Int,
    /** 时长 */
    val duration: Long,
    /** 帧率 */
    val frameRate: Int,
    /** 全局调色盘大小 */
    val gctSize: Int,
    /** 文件大小 */
    val fileSize: Long
)



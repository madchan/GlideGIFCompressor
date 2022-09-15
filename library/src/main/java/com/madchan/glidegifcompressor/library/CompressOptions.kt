package com.madchan.glidegifcompressor.library

import android.net.Uri

/**
 * GIF压缩选项
 */
class CompressOptions {
    /** 输入GIF文件 */
    var source: Uri? = null
    /** 输出GIF文件 */
    var sink: Uri? = null

    /** 目标宽度 */
    var targetWidth = 320
    /** 目标高度 */
    var targetHeight = 240
    /** 目标调色盘大小 */
    var targetGctSize = 256
    /** 目标帧率 */
    var targetFps = 24

    /** 压缩进度监听器 */
    var listener: CompressListener? = null
}
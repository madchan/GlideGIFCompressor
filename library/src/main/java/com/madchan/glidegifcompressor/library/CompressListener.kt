package com.madchan.glidegifcompressor.library

/** GIF压缩进度监听器 */
interface CompressListener {

    fun onStart()

    fun onCompleted()

    fun onFailed(exception: Throwable)

}
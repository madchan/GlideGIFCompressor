package com.madchan.glidegifcompressor.library

import android.net.Uri

class CompressOptions {

    var width = 320
    var height = 240
    var color = 256
    var fps = 24
    var repeat = 0

    var source: Uri? = null
    var sink: Uri? = null

    var listener: CompressListener? = null

    fun size(width: Int, height: Int): CompressOptions {
        this.width = width
        this.height = height
        return this
    }

    fun color(color: Int): CompressOptions {
        this.color = color
        return this
    }

    fun frameRate(fps: Int): CompressOptions {
        this.fps = fps
        return this
    }

    fun repeat(repeat: Int): CompressOptions {
        this.repeat = repeat
        return this
    }

    fun source(source: Uri): CompressOptions {
        this.source = source
        return this
    }

    fun sink(sink: Uri): CompressOptions {
        this.sink = sink
        return this
    }

    fun listener(listener: CompressListener): CompressOptions {
        this.listener = listener
        return this
    }
}
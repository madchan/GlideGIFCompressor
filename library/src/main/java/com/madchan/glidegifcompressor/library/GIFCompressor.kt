package com.madchan.glidegifcompressor.library

import android.annotation.SuppressLint
import android.content.Context
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object GIFCompressor {

    private lateinit var context: Context
    private var options = CompressOptions()

    private val mExecutor: ThreadPoolExecutor

    init {
        val pool = Runtime.getRuntime().availableProcessors() + 1
        mExecutor = ThreadPoolExecutor(
            pool, pool,
            60, TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(),
            Factory()
        )
    }

    fun with(context: Context): GIFCompressor {
        this.context = context.applicationContext
        return this
    }

    fun apply(options: CompressOptions): GIFCompressor {
        this.options = options
        return this
    }

    fun load() {
        mExecutor.submit(CompressTask(context, options))
    }

    private class Factory : ThreadFactory {
        private val count = AtomicInteger(1)
        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, " Thread #" + count.getAndIncrement())
        }
    }
}
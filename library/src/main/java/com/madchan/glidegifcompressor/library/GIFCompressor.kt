package com.madchan.glidegifcompressor.library

import android.annotation.SuppressLint
import android.content.Context
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("StaticFieldLeak")
object GIFCompressor {

    private val executor: ThreadPoolExecutor

    init {
        val pool = Runtime.getRuntime().availableProcessors() + 1
        executor = ThreadPoolExecutor(
            pool, pool,
            60, TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(),
            Factory()
        )
    }

    fun compress(context: Context, options: CompressOptions) {
        executor.submit(CompressJob(context.applicationContext, options))
    }

    private class Factory : ThreadFactory {
        private val count = AtomicInteger(1)
        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, " Thread #" + count.getAndIncrement())
        }
    }
}
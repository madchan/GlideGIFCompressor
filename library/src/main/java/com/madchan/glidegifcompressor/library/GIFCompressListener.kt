package com.madchan.glidegifcompressor.library

/**
 * Listeners for compression events. All the callbacks are called on the handler
 */
interface GIFCompressListener {

    fun onStart()

    /**
     * Called to notify progress.
     *
     * @param progress Progress in [0.0, 1.0] range, or negative value if progress is unknown.
     */
    fun onProgress(progress: Double)

    /**
     * Called when compress completed.
     */
    fun onCompleted()

    /**
     * Called when compression was canceled.
     */
    fun onCanceled()

    /**
     * Called when compression failed.
     * @param exception the failure exception
     */
    fun onFailed(exception: Throwable)
}
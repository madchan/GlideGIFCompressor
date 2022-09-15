package com.madchan.glidegifcompressor.library

/**
 * GIF抽帧器
 */
class GIFFrameDropper(frameRate: Int, outputFrameRate: Int) {
    
    private val inFrameRateReciprocal = 1.0 / frameRate
    private val outFrameRateReciprocal = 1.0 / outputFrameRate
    private var frameRateReciprocalSum = 0.0
    private var frameCount = 0
    
    fun shouldRenderFrame(presentationTimeUs: Long): Boolean {
        frameRateReciprocalSum += inFrameRateReciprocal
        return when {
            frameCount++ == 0 -> {
                true
            }
            frameRateReciprocalSum > outFrameRateReciprocal -> {
                frameRateReciprocalSum -= outFrameRateReciprocal
                true
            }
            else -> {
                false
            }
        }
    }

}
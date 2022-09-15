package com.madchan.glidegifcompressor.library

/**
 * GIF抽帧器
 */
class GIFFrameSampler(inputFrameRate: Int, outputFrameRate: Int) {
    
    private val inFrameRateReciprocal = 1.0 / inputFrameRate
    private val outFrameRateReciprocal = 1.0 / outputFrameRate
    private var frameRateReciprocalSum = 0.0
    private var frameCount = 0
    
    fun shouldRenderFrame(): Boolean {
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
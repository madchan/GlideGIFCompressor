package com.madchan.glidegifcompressor.library

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import com.madchan.glidegifcompressor.library.gif.AnimatedGifEncoder
import java.lang.Exception

class CompressTask(
    private val context: Context?,
    private val options: CompressOptions
) : Runnable {

    companion object {
        val TAG = CompressTask::class.java.simpleName
    }

    private var outWidth: Int = 0
    private var outHeight: Int = 0

    override fun run() {
        options.listener?.onStart()

        val info = GifInfoParser().parse(options.source!!)
        val decoder = constructDecoder(info)

        val decodedFrames = decoder.decodeFrames()
        val sampledFrames = decoder.sampleFrames(info, decodedFrames)
//        val sampledFrames = decoder.violentlySampleFrames(info)

        val encoder = constructEncoder()
        encoder.encode(sampledFrames)
    }

    private fun constructDecoder(info: GifInfo): StandardGifDecoder {
        if(context == null) throw IllegalArgumentException("Context can not be null.")

        val sampleSize = calculateSampleSize(info.getWidth(), info.getHeight(), options.width, options.height)
        Log.i(TAG, "Construct decoder with: sampleSize = $sampleSize")
        return StandardGifDecoder(GifBitmapProvider(Glide.get(context).bitmapPool)).apply {
            setData(
                info.header,
                info.dataSource,
//                sampleSize
                1
            )
        }
    }

    private fun calculateSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        val widthPercentage = targetWidth / sourceWidth.toFloat()
        val heightPercentage = targetHeight / sourceHeight.toFloat()
        val exactScaleFactor = Math.min(widthPercentage, heightPercentage)

        outWidth = round((exactScaleFactor * sourceWidth).toDouble())
        outHeight = round((exactScaleFactor * sourceHeight).toDouble())

        val widthScaleFactor = sourceWidth / outWidth
        val heightScaleFactor = sourceHeight / outHeight

        val scaleFactor = Math.max(widthScaleFactor, heightScaleFactor)

        var powerOfTwoSampleSize = Math.max(1, Integer.highestOneBit(scaleFactor))
//        if (powerOfTwoSampleSize < 1f / exactScaleFactor) {
//            powerOfTwoSampleSize = powerOfTwoSampleSize shl 1
//        }
        return powerOfTwoSampleSize
    }

    private fun round(value: Double): Int {
        return (value + 0.5).toInt()
    }

    private fun StandardGifDecoder.decodeFrames(): List<Bitmap> {
        return (0 until frameCount).mapNotNull {
            advance()
            nextFrame
        }
    }

    private fun StandardGifDecoder.sampleFrames(
        info: GifInfo,
        completeFrames: List<Bitmap>
    ): List<Bitmap> {
        val dropper = VideoFrameDropper.newDropper(info.inputFrameRate, options.fps)
        return (0 until frameCount).mapNotNull {
            if (dropper.shouldRenderFrame(0)){
                Log.i(TAG, "Sample ")
                completeFrames[it]
            } else {
                null
            }
        }
    }

    private fun StandardGifDecoder.violentlySampleFrames(
        info: GifInfo,
    ): List<Bitmap> {
        val dropper = VideoFrameDropper.newDropper(info.inputFrameRate, options.fps)
        return (0 until frameCount).mapNotNull {
            advance()
            if (dropper.shouldRenderFrame(0)){
                nextFrame
            } else {
                null
            }
        }
    }

    private fun constructEncoder(): AnimatedGifEncoder{
        return AnimatedGifEncoder().apply {
            setRepeat(options.repeat) // 重复播放
            setFrameRate(options.fps.toFloat())
        }
    }

    private fun AnimatedGifEncoder.encode(sampleFrames: List<Bitmap>) {
        try {
            setSize(outWidth, outHeight)
            start(options.sink?.path!!)
            val palSize = (Math.log(options.color.toDouble())/Math.log(2.0)).toInt() - 1
            setPalSize(palSize)
            sampleFrames.forEach { addFrame(it) }
            finish()
        }catch (e: Exception) {
            options.listener?.onFailed(e)
        } finally {
            options.listener?.onCompleted()
        }
    }

}
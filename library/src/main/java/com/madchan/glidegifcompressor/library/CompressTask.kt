package com.madchan.glidegifcompressor.library

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.gifencoder.AnimatedGifEncoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import com.bumptech.glide.util.ByteBufferUtil
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

class CompressTask(
    private val context: Context?,
    private val options: CompressOptions
) : Runnable {

    companion object {
        val TAG = CompressTask::class.java.simpleName
    }

    override fun run() {
        options.listener?.onStart()

        val info = GifInfoParser().parse(options.source!!)

        val decoder = constructDecoder(info)

        val completeFrames = decoder.collectFrames()
        val sampleFrames = decoder.sampleFrames(info, completeFrames)
//        val sampleFrames = decoder.violentlySampleFrames(info)

        val encoder = constructEncoder()
        encoder.encode(sampleFrames)
    }

    private fun constructDecoder(info: GifInfo): StandardGifDecoder {
        if(context == null) throw IllegalArgumentException()

        val sampleSize = calculateSampleSize(info.getWidth(), info.getHeight(), options.width, options.height)
        Log.i(TAG, "Construct decoder with: sampleSize = $sampleSize")
        return StandardGifDecoder(GifBitmapProvider(Glide.get(context).bitmapPool)).apply {
            setData(
                info.header,
                info.dataSource,
                sampleSize
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

        val outWidth = round((exactScaleFactor * sourceWidth).toDouble())
        val outHeight = round((exactScaleFactor * sourceHeight).toDouble())

        val widthScaleFactor = sourceWidth / outWidth
        val heightScaleFactor = sourceHeight / outHeight

        val scaleFactor = Math.max(widthScaleFactor, heightScaleFactor)

        return Math.max(1, Integer.highestOneBit(scaleFactor))
    }

    private fun round(value: Double): Int {
        return (value + 0.5).toInt()
    }

    private fun StandardGifDecoder.collectFrames(): List<Bitmap> {
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
        // 按计算好的采样间隔进行抽帧并缩放
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
        start(options.sink?.path!!)
        setQuality(options.quality)
        sampleFrames.forEach { addFrame(it) }
        finish()

        options.listener?.onCompleted()
    }

}
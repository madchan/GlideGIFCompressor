package com.madchan.glidegifcompressor.library

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import com.madchan.glidegifcompressor.library.gifencoder.AnimatedGifEncoder
import java.lang.Exception

/**
 * GIF异步压缩任务
 */
class CompressJob(
    private val context: Context?,
    private val options: CompressOptions
) : Runnable {

    companion object {
        val TAG: String = CompressJob::class.java.simpleName
    }

    private var outWidth: Int = 0
    private var outHeight: Int = 0

    override fun run() {
        options.listener?.onStart()

        val metadata = GIFMetadataParser().parse(options.source!!)
        val decoder = constructDecoder(metadata)

        val decodedFrames = decoder.decodeFrames()
        val sampledFrames = decoder.sampleFrames(metadata, decodedFrames)
//        val sampledFrames = decoder.violentlySampleFrames(metadata)

        val encoder = constructEncoder()
        encoder.encode(sampledFrames)
    }

    private fun constructDecoder(metadata: GIFMetadata): StandardGifDecoder {
        if(context == null) throw IllegalArgumentException("Context can not be null.")

        val sampleSize = calculateSampleSize(metadata.getWidth(), metadata.getHeight(), options.targetWidth, options.targetHeight)
        Log.i(TAG, "Construct decoder with: sampleSize = $sampleSize")
        return StandardGifDecoder(GifBitmapProvider(Glide.get(context).bitmapPool)).apply {
            setData(
                metadata.header,
                metadata.dataSource,
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
        metadata: GIFMetadata,
        completeFrames: List<Bitmap>
    ): List<Bitmap> {
        val dropper = GIFFrameDropper(metadata.inputFrameRate, options.targetFps)
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
        metadata: GIFMetadata,
    ): List<Bitmap> {
        val dropper = GIFFrameDropper(metadata.inputFrameRate, options.targetFps)
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
            setFrameRate(options.targetFps.toFloat())
        }
    }

    private fun AnimatedGifEncoder.encode(sampleFrames: List<Bitmap>) {
        try {
            setSize(outWidth, outHeight)
            start(options.sink?.path!!)
            val palSize = (Math.log(options.targetGctSize.toDouble())/Math.log(2.0)).toInt() - 1
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
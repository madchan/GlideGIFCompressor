package com.madchan.glidegifcompressor.library

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import com.madchan.glidegifcompressor.library.gifencoder.AnimatedGifEncoder
import java.lang.Exception
import java.nio.ByteBuffer

/**
 * GIF异步压缩任务
 */
class CompressJob(
    private val context: Context?,
    private val options: CompressOptions
) : Runnable {

    private var outWidth: Int = 0
    private var outHeight: Int = 0

    override fun run() {
        options.listener?.onStart()

        try {
            // 1.解析GIF文件元数据
            val gifMetadataParser = GIFMetadataParser()
            val gifMetadata = gifMetadataParser.parse(options.source!!)

            // 2.解码出完整的图像帧序列，并进行下采样
            val gifDecoder = constructGifDecoder(gifMetadataParser.gifHeader, gifMetadataParser.gifData, gifMetadata)
            val gifFrames = gifDecoder.decode()

            // 3.根据目标帧率进行抽帧
            val gifFrameSampler = GIFFrameSampler(gifMetadata.frameRate, options.targetFps)
            val sampledGifFrames = gifFrameSampler.sample(gifMetadata, gifFrames)
            // val sampledGifFrames = decoder.violentlySampleFrames(gifMetadata)

            // 4.将处理后的图像帧序列重新编码
            val gifEncoder = constructGifEncoder()
            gifEncoder.encode(sampledGifFrames)

        }catch (e: Exception) {
            options.listener?.onFailed(e)
        }
    }

    private fun constructGifDecoder(
        gifHeader: GifHeader,
        gifData: ByteBuffer,
        gifMetadata: GIFMetadata
    ): StandardGifDecoder {
        if(context == null) throw IllegalArgumentException("Context can not be null.")
        val sampleSize = calculateSampleSize(gifMetadata.width, gifMetadata.height, options.targetWidth, options.targetHeight)
        return StandardGifDecoder(GifBitmapProvider(Glide.get(context).bitmapPool)).apply {
            setData(
                gifHeader,
                gifData,
                sampleSize
            )
        }
    }

    /**
     * 计算下采样大小
     * @param sourceWidth 源宽度
     * @param sourceHeight 源高度
     * @param targetWidth 目标宽度
     * @param targetHeight 目标高度
     */
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

    private fun StandardGifDecoder.decode(): List<Bitmap> {
        return (0 until frameCount).mapNotNull {
            advance()
            nextFrame
        }
    }

    private fun GIFFrameSampler.sample(
        gifMetadata: GIFMetadata,
        gifFrames: List<Bitmap>
    ): List<Bitmap> {
        return (0 until gifMetadata.frameCount).mapNotNull {
            if (shouldRenderFrame()){
                gifFrames[it]
            } else {
                null
            }
        }
    }

    private fun StandardGifDecoder.violentlySample(
        gifMetadata: GIFMetadata,
    ): List<Bitmap> {
        val dropper = GIFFrameSampler(gifMetadata.frameRate, options.targetFps)
        return (0 until frameCount).mapNotNull {
            advance()
            if (dropper.shouldRenderFrame()){
                nextFrame
            } else {
                null
            }
        }
    }

    private fun constructGifEncoder(): AnimatedGifEncoder{
        return AnimatedGifEncoder().apply {
            // 调整全局调色盘大小
            val palSize = (Math.log(options.targetGctSize.toDouble())/Math.log(2.0)).toInt() - 1
            setPalSize(palSize)
            // 调整分辨率
            setSize(outWidth, outHeight)
            // 调整帧率
            setFrameRate(options.targetFps.toFloat())
        }
    }

    private fun AnimatedGifEncoder.encode(sampleFrames: List<Bitmap>) {
        // 开始写入
        start(options.sink?.path!!)
        // 逐一添加帧
        sampleFrames.forEach { addFrame(it) }
        // 完成，关闭输出文件
        finish()

        options.listener?.onCompleted()
    }

}
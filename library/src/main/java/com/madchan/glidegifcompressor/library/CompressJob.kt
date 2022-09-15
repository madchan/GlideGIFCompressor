package com.madchan.glidegifcompressor.library

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import com.madchan.glidegifcompressor.library.gifencoder.AnimatedGifEncoder
import java.nio.ByteBuffer

/**
 * GIF异步压缩任务
 */
class CompressJob(
    private val context: Context?,
    private val options: CompressOptions
) : Runnable {

    /** 根据缩放比计算得出的输出宽度 */
    private var outWidth: Int = 0
    /** 根据缩放比计算得出的输出高度 */
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
            val gifFrameSampler = GIFFrameSampler(gifMetadata.frameRate, options.targetFrameRate)
            val sampledGifFrames = gifFrameSampler.sample(gifMetadata.frameCount, gifFrames)
//            val sampledGifFrames = gifDecoder.violentlySample(gifMetadata.frameRate)

            // 4.将处理后的图像帧序列重新编码
            val gifEncoder = constructGifEncoder()
            gifEncoder.encode(sampledGifFrames)

        }catch (e: Exception) {
            options.listener?.onFailed(e)
        }
    }

    /**
     * 构造GIF解码器
     * @param gifHeader GIF头部
     * @param gifData GIF数据
     * @param gifMetadata GIF元数据
     */
    private fun constructGifDecoder(
        gifHeader: GifHeader,
        gifData: ByteBuffer,
        gifMetadata: GIFMetadata
    ): StandardGifDecoder {
        if(context == null) throw IllegalArgumentException("Context can not be null.")
        val sampleSize = calculateSampleSize(
            gifMetadata.width,
            gifMetadata.height,
            options.targetWidth,
            options.targetHeight
        )
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

    /**
     * 解码出完整的图像帧序列
     */
    private fun StandardGifDecoder.decode(): List<Bitmap> {
        return (0 until frameCount).mapNotNull {
            advance()
            nextFrame
        }
    }

    /**
     * 根据目标帧率进行抽帧
     * @param frameCount 帧数
     * @param gifFrames 图像帧序列
     */
    private fun GIFFrameSampler.sample(
        frameCount: Int,
        gifFrames: List<Bitmap>
    ): List<Bitmap> {
        return (0 until frameCount).mapNotNull {
            if (shouldRenderFrame()){
                gifFrames[it]
            } else {
                null
            }
        }
    }

    /**
     * 直接跳过中间帧的暴力抽帧
     * @param frameRate 输入帧率
     */
    private fun StandardGifDecoder.violentlySample(
        frameRate: Int,
    ): List<Bitmap> {
        val dropper = GIFFrameSampler(frameRate, options.targetFrameRate)
        return (0 until frameCount).mapNotNull {
            advance()
            if (dropper.shouldRenderFrame()){
                nextFrame
            } else {
                null
            }
        }
    }

    /**
     * 构造GIF编码器
     */
    private fun constructGifEncoder(): AnimatedGifEncoder{
        return AnimatedGifEncoder().apply {
            // 调整全局调色盘大小
            val palSize = (Math.log(options.targetGctSize.toDouble())/Math.log(2.0)).toInt() - 1
            setPalSize(palSize)
            // 调整分辨率
            setSize(outWidth, outHeight)
            // 调整帧率
            setFrameRate(options.targetFrameRate.toFloat())
        }
    }

    /**
     * 将处理后的图像帧序列重新编码
     * @param sampleFrames 抽帧后的图像帧序列
     */
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
---
theme: channing-cyan
---

“我报名参加金石计划1期挑战——瓜分10万奖池，这是我的第2篇文章，[点击查看活动详情](https://s.juejin.cn/ds/jooSN7t "https://s.juejin.cn/ds/jooSN7t")”

![android.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/47eee0521f294dd0a05410dfa1ca22bb~tplv-k3u1fbpfcp-zoom-1.image)

>移动端的图片压缩是一个老生常谈的话题，也曾涌现过不少诸如Luban之类的优秀的图片压缩工具库，但在GIF图像领域的压缩方案却几乎处于一片空白。
>
>许多开发者不知道的是，实际上，已经有一套现成的GIF图像压缩工具集，就内置在你集成的Glide图片加载框架之中。
---

大家好，我是潜伏于各大群中收集GIF表情包的**星际码仔**，今天我们要分享的是**移动端的GIF图像压缩方案**。

我们会从GIF图像的基础知识出发，介绍几种常见的GIF图像压缩策略，然后利用Glide框架内部自带的压缩工具集来实现。

过程中如有不合理的地方，欢迎随时"Objection！"。

照例，奉上思维导图一张：

![用Glide实现GIF压缩.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/48dd2668bc1a4e2a855dd2ff9e17ebcb~tplv-k3u1fbpfcp-watermark.image?)

## GIF图像基础知识

GIF的全称是**Graphics Interchange Format**，即**图像交换格式**，是CompuServe公司为支持彩色图像的下载，于1987年推出的位图图像格式。

GIF采用**Lempel-Ziv-Welch**（LZW）无损数据压缩技术进行压缩，可以**在不降低视觉质量的情况下减少文件大小**。

![LZW.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/06ac42a7d6474f6384e18c5a94429c96~tplv-k3u1fbpfcp-watermark.image?)

凭借其**体积小、成像相对清晰**的优点，GIF在带宽小、传输慢的互联网初期广受欢迎。发展至今，以其被大多数主流平台所支持的**高兼容性**，占据了动图格式的大半片江山。

### 256色
作为一种古老的位图图像格式，GIF的缺点也很明显，比如**仅支持8 bit的色深，每个像素最多只能显示2^8=256种颜色**。

相比之下，因LZW算法专利问题而被设计出来替代GIF的PNG格式，即使是不带透明度的24 bit格式，最多也可显示2^24=1600多万种颜色。

![战五渣.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7defab42da184560845fceb897f47a10~tplv-k3u1fbpfcp-zoom-1.image)

256色的限制大大局限了GIF的应用范围，使得GIF**只适用于包含少量颜色的图片**，比如Logo、卡通人物等，而在色彩丰富甚至带有渐变效果的图片上则表现不佳，常常会使图片伴有明显的噪点失真。

### 动效
GIF通过**将多张图像存储在同一个文件中，并利用人眼视觉残留的特性，控制连续播放的间隔，以实现简单的动画效果**，原理上有点类似于小时候玩过的手翻书。

![手翻书](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1d3f2ca31a244776bac394d537308a91~tplv-k3u1fbpfcp-zoom-1.image)

同样，因受256色的限制，**GIF动图大多只能用于小型的动画和低分辨率的视频**。

不过即便如此，相比于静态图片，GIF动图显然能传递更多的信息，并使沟通双方的情感交流更加直接、高效，因而得以在社交软件上被广泛使用和传播，近年来流行的表情包文化就是很好的佐证。

### 调色盘
GIF文件有一个很重要的概念就是**调色盘**，个人认为调色盘这个名称用得很恰当，可以说高度概括了其特征。


![调色盘.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5804429c431c4a4aab73f699106687f7~tplv-k3u1fbpfcp-watermark.image?)

那什么是调色盘呢？

前面我们讲了，GIF是一种**位图图像格式**，关于位图的特征，我们在[《Bitmap——Android内存刺客》](https://mp.weixin.qq.com/s/rPzIcyvdMjRiSA_oZ1nayQ)一文中已经有过介绍。简单讲，位图就是由若干个不同颜色的像素进行排列所构成的像素阵列。

![位图.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/59f57036f4054776be833059c5cfd4c2~tplv-k3u1fbpfcp-watermark.image?)

另外我们知道，GIF动图实际就是连续播放的多张图像，每张图像称为一帧，帧与帧之间的信息差异不大，其中的颜色是被大量重复使用的。

于是我们可以建立这样一张公共的索引表，**把每一帧的像素点所用到的颜色提取出来，组成一个调色盘，并为每个颜色值建立索引**。

这样，在存储真正的像素阵列时，**只需要存储每个颜色在调色盘里的对应索引值即可，从而减少存储的信息量**。

![调色盘索引.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c62b8df27fdf4da39ee7e208a2c7a2e8~tplv-k3u1fbpfcp-watermark.image?)

如果把调色盘放在文件头，作为所有帧公用的信息，就是**全局调色盘**；而如果放在每一帧的帧信息中，就是**局部调色盘**。

GIF允许两种调色盘同时存在，并且局部调色盘的优先级更高，当没有局部调色盘时，就使用公共调色盘渲染。

很明显，**颜色越丰富，调色盘也就越大，并最终影响到GIF文件的大小**。

### 文件大小
以我们最为熟悉的表情包为例，GIF动图类型的表情包的来源大致可分为**手绘卡通图像**以及**视频片段截取**两种。

手绘卡通图像的线条单调，颜色均匀，人物动作简单，因而调色盘大小与图像帧数往往都不大。

![手绘卡通图像.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b5d9a7a4d9604f7db2a28bdc2ad495b0~tplv-k3u1fbpfcp-zoom-1.image)

而视频片段截取之后转换的GIF，往往保留了原有视频的高帧数，且视频内容本身包含了大量的颜色细节，很容易就占满整个调色盘的大小。

![视频片段截取.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/cf85ceceed7e4c92ae9003d94e66e047~tplv-k3u1fbpfcp-zoom-1.image)

这也是视频片段截取的GIF文件大小往往比手绘卡通图像的大很多的原因所在。

## GIF图像压缩策略

 GIF文件过大，对于如何存储和传输都是一个难题，下面就来介绍一下几种常见的GIF图像压缩策略：

#### 缩放

作为一种位图图像格式，GIF文件的大小是跟分辨率呈正相关的，分辨率越高，所包含的像素个数就越多，图像也就越清晰，但相应的文件体积也就越大。

鉴于我们通常是在一个有限的展示区域内显示GIF图像的，因此更合理点的做法应该是**先对原始的GIF图像先进行一轮下采样，以提供一个较低分辨率版本的缩略图，减少内存占用，再贴合展示区域的尺寸进行一轮精确的缩放**。

#### 减色

减色也就是**减少调色盘的颜色**，同样可以达到压缩的效果。但是**GIF本身仅支持的最高256色已经是捉襟见肘了，再进一步减色，可能会使图像质量的损失更加明显**。而且这种方式的压缩率也比较低，减去一半颜色也可能只压缩10%左右。

以下是分别将调色盘的颜色减少至64色、16色和2色的效果：

![64color.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/45a942026e7e4a04a6ae29666db4a320~tplv-k3u1fbpfcp-zoom-1.image)

![16color.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/25dc2810f2e6488082b2cdb8848de81f~tplv-k3u1fbpfcp-zoom-1.image)

![2color.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5efcff6fa88643fe9846e803da633544~tplv-k3u1fbpfcp-zoom-1.image)

可以看到，随着调色盘颜色的减少，图片逐渐暗淡，颜色过渡也愈加粗糙，到最后甚至只剩下黑白两色。

#### 抽帧

前面讲过，GIF是通过逐帧播放单幅图像以达到连续动画的效果的。而抽帧，顾名思义，就是**从这些图像中每间隔一定的帧数抽取出单幅图像，通过降低帧率以达到降低GIF文件整体大小的效果**。

比如电影的常见帧率为24fps（帧每秒），截取其中的3秒并转换为GIF后，帧率依旧保持在24fps，那么总共要储存72幅图像；如果通过抽帧，将帧率降到12fps，就只要储存36幅图像就可以了。

不过，抽帧会影响到GIF动效的流畅度，因为**帧率降低之后，帧与帧之间的延迟时间变长，可能会达不到人眼视觉残留特性的阈值，从而在视觉感受上会有明显的卡顿**。

![重庆森林.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5daa426809de4fb2861ac92e9c51135a~tplv-k3u1fbpfcp-zoom-1.image)

#### 透明度存储

开始介绍这种方式之前，我们先来看一张GIF图：

![透明度存储示例.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/11ff2de7f89e47f1977fad1b39575ae4~tplv-k3u1fbpfcp-zoom-1.image)

根据直觉，我们猜想这张GIF图拆解后的每一帧应该是这样的：

![透明度存储示例拆解猜想.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/50a1db7214ab41ff97477c42ba11244d~tplv-k3u1fbpfcp-watermark.image?)

然而实际上，每一帧是这样的：

![透明度存储示例拆解实际情况.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/40ed02ec111e4a04b7c8e9d028f1a116~tplv-k3u1fbpfcp-watermark.image?)

也就是说，透明度存储这种方式是通过**只完整保留GIF的第一帧，排除后续帧没有变化的区域，只存储有变化的像素，而对于没变化的像素只存储一个透明值，从而避免存储重复的信息**来达到压缩的效果的，适合GIF图像本身具有较大的静态区域的情况。

今天利用Glide框架内部自带的压缩工具集来实现的，主要是前面的三种压缩策略。

## GIF图像压缩工具集

终于讲到正题了，让我们来看Glide框架内部都自带了哪些GIF压缩工具：

- **GifHeader**：GIF文件头。包含了GIF动图的帧数与每个独立帧的宽高等基本元数据，用于解码GIF。

- **StandardGifDecoder**：GIF解码器。从 GIF 图像源读取帧数据，并将其解码为独立的帧。

- **AnimatedGifEncoder**：GIF编码器。编码由一个或多个帧组成的 GIF 文件。

核心的类其实就以上几个。严格来讲，这一套GIF编解码实现类并非完全是Glide的原创，而是改编自其他开发者发布的示例开源代码，只不过为了支持GIF的编解码而内置到Glide库中而已。

## GIF图像压缩步骤

接下来，我们就利用这一套压缩工具集来实现GIF压缩。

#### 步骤1：解析GIF文件头，以获取其帧数及每个帧的源宽高

GIF格式的文件头与其他格式的文件头作用一致，都是**位于文件开头的一段数据，用于描述文件的一些重要属性，指示打开该文件的程序应该怎样处理这个文件**。

主要包含：

##### 格式声明

![格式声明.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0acade4e1d1949ce954d411e4b71c350~tplv-k3u1fbpfcp-watermark.image?)

- Signature 为文件类型的签名，此处为“GIF”3 个字符；
- Version 为GIF发布的版本号，可能是“87a”或“89a”。

##### 逻辑屏幕描述块

![逻辑屏幕描述块.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/c0c1e0b73082466b8b8becbdab37a096~tplv-k3u1fbpfcp-watermark.image?)

- 前两字节用以标识GIF图像的视觉宽高，单位是像素。
- Packet fields里包含的就是全局调色盘的信息了，比如全局调色盘的大小等，这里是简单介绍，就不一一展开了。

解析GIF文件头需要用到`GifHeaderParser`类，该类负责从表示GIF动图的数据中创建 `GifHeaders`类。

但实际`GifHeaderParser`类除了会解析GIF文件头外，还会读取GIF文件内容块，以获取帧数及局部调色盘等关键信息。

示例代码如下：
```
    // 1.解析GIF文件元数据
    val gifMetadataParser = GIFMetadataParser()
    val gifMetadata = gifMetadataParser.parse(options.source!!)
```
```
    fun parse(source: Uri): GIFMetadata {
        val file = File(source.path)

        gifData = ByteBufferUtil.fromFile(file)
        gifHeader = parseHeader(gifData)

        val duration = getDuration(gifHeader)

        return GIFMetadata(
            width = gifHeader.width,
            height = gifHeader.height,
            frameCount = gifHeader.numFrames,
            duration = getDuration(gifHeader),
            frameRate = getFrameRate(gifHeader.numFrames, duration),
            gctSize = getGctSize(gifHeader),
            fileSize = file.length()
        )
    }
```
```
    /**
     * 解析GIF文件头
     */
    private fun parseHeader(data: ByteBuffer): GifHeader {
        return GifHeaderParser().apply { setData(data) }.parseHeader()
    }
```

#### 步骤2：对比源宽高与目标宽高，计算出采样后只比目标宽高稍大些的样本大小

做过Bitmap内存优化工作的同学，看到**样本大小**(sampleSize)这个字眼是否有眼前一亮的感觉？是的，GIF解码器同样支持以2的次幂的样本大小对原始图像进行下采样，从而返回较小的图像以节省内存。

这一步样本大小的计算主要参考了Glide框架中对于Bitmap部分处理的源码思路，感兴趣的可阅读我之前写的[《Glide，你为何如此优秀？》](https://mp.weixin.qq.com/s/Tm3zauPlrBECKJPn_GUEXA)，这里就不重复讲了。

```
    // 2.解码出完整的图像帧序列，并进行下采样
    val gifDecoder = constructGifDecoder(gifMetadataParser.gifHeader, gifMetadataParser.gifData, gifMetadata)
    val gifFrames = gifDecoder.decode()
```
```
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
```
```
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
        return powerOfTwoSampleSize
    }

```

#### 步骤3：顺序解码每一帧，还原为完整的图像帧序列

之所以需要这一步，主要是因为部分GIF图像采用了前面所介绍的透明度存储方式来进行压缩，如果**暴力抽帧**，也即**跳过中间帧直接进行抽帧**，则最终会得到这样的图片：

![暴力抽帧.gif](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/be0291de386343478c96d96ad845117a~tplv-k3u1fbpfcp-zoom-1.image)

可以看到，暴力抽帧后的GIF图会有明显的残留噪点，这是因为**后续帧存储的仅仅是与第一帧对比有变化的像素**，所以我们要**先顺序解码每一帧，借助叠加方式、透明色索引等信息来还原出完整的图像帧**。

```
     /**
     * 解码出完整的图像帧序列
     */
    private fun StandardGifDecoder.decode(): List<Bitmap> {
        return (0 until frameCount).mapNotNull {
            advance()
            nextFrame
        }
    }
```

#### 步骤4：根据目标帧率进行抽帧，并重新计算帧间延迟

注意是根据**目标帧率**，而不是目标帧数。如果只是减少帧数，而帧间延迟保持不变，会造成GIF动效的总时长也相应变短，直观感受上就是动画明显加快了。

根据目标帧率进行抽帧，就是**保持GIF动效的总时长不变，只是减少1秒内播放的图像帧数，也即减少帧率**，为此需要我们**重新计算帧间延迟，对播放速度进行减缓处理**。

```
    // 3.根据目标帧率进行抽帧
    val gifFrameSampler = GIFFrameSampler(gifMetadata.frameRate, options.targetFrameRate)
    val sampledGifFrames = gifFrameSampler.sample(gifMetadata.frameCount, gifFrames)
```
```
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
```
```
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
```
#### 步骤5：重新编码为GIF文件，并依照配置参数进行精确缩放和减色

了解了GIF动效的原理之后，重新编码的流程就变得很清晰了，无非就是**将抽取之后的图像帧序列逐一添加回编码器，以写入必要的文件头数据以及图像的像素数据，并根据目标帧率调整帧与帧之间的延迟时间**，就可以重新编码生成新的GIF图像了。


```
    // 4.将处理后的图像帧序列重新编码
    val gifEncoder = constructGifEncoder()
    gifEncoder.encode(sampledGifFrames)
```
```
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
```
```
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
```

## 一个Demo

https://github.com/madchan/GlideGIFCompressor.git

为了方便演示以上所提及策略的实际压缩效果，我写了一个GIF图像压缩前后对比的Demo，可以通过调整宽高、帧率、色彩三个属性的数值来分别实现缩放、抽帧、减色三种压缩策略：


![Demo.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4206d10b11a648f6afaf1149660e4fc1~tplv-k3u1fbpfcp-watermark.image?)

如果这个Demo对你有帮助，希望不吝点个哈～

好了，以上就是今天要分享的内容。最后提一个问题，除了GIF，你还知道有哪些**动图格式**呢？欢迎在评论区或后台讨论哈～

> 少侠，请留步！若本文对你有所帮助或启发，还请：
> 
> 1. 点赞👍🏻，让更多的人能看到！
> 2. 收藏⭐️，好文值得反复品味！
> 3. 关注➕，不错过每一次更文！
> 
> ===> 公众号：「星际码仔」💪
> 
> 你的支持是我继续创作的动力，感谢！🙏

## 参考
- 浓缩的才是精华：浅析 GIF 格式图片的存储和压缩
https://cloud.tencent.com/developer/article/1004763
- 如何正确压缩GIF格式文件？来看京东设计师的总结！
https://www.uisdc.com/gif-compression

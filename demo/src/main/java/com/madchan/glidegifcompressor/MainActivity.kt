package com.madchan.glidegifcompressor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.UriUtils
import com.bumptech.glide.Glide
import com.madchan.glidegifcompressor.databinding.ActivityMainBinding
import com.madchan.glidegifcompressor.library.CompressListener
import com.madchan.glidegifcompressor.library.CompressOptions
import com.madchan.glidegifcompressor.library.GIFCompressor
import com.madchan.glidegifcompressor.library.GIFMetadataParser
import java.io.File

const val PICK_GIF_FILE = 2
const val REQUEST_EXTERNAL_STORAGE_PERMISSION = 2

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var sourceFile: File? = null
    private var sinkFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE_PERMISSION)
        }
    }

    fun chooseGif(view: View) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/gif" }
        startActivityForResult(intent, PICK_GIF_FILE)
    }

    fun startCompress(view: View) {
        sinkFile = File(externalCacheDir, "${System.currentTimeMillis()}.gif")
        FileUtils.createFileByDeleteOldFile(sinkFile)

        val options = CompressOptions().apply {
                source = Uri.parse(sourceFile?.absolutePath)
                sink = Uri.parse(sinkFile?.absolutePath)

                targetWidth = binding.width.text.toString().toInt()
                targetHeight = binding.height.text.toString().toInt()
                targetFrameRate = binding.fps.text.toString().toInt()
                targetGctSize = binding.color.text.toString().toInt()

                listener = object : CompressListener {
                    override fun onStart() {
                        runOnUiThread {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                    }

                    override fun onCompleted() {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "onCompleted", Toast.LENGTH_LONG).show()
                            binding.progressBar.visibility = View.GONE

                            sinkFile.let { file->
                                val gifInfo = GIFMetadataParser().parse(Uri.parse(file?.absolutePath))
                                Glide.with(this@MainActivity).load(file).into(binding.compressedPreview)
                                binding.compressedSize.text = ConvertUtils.byte2FitMemorySize(gifInfo.fileSize)
                                binding.compressedFrameCount.text = gifInfo.frameCount.toString()
                                binding.compressedDuration.text = ConvertUtils.millis2FitTimeSpan(gifInfo.duration, 5)
                                binding.compressedFps.text = gifInfo.frameRate.toString()
                                binding.compressedWidth.text = gifInfo.width.toString()
                                binding.compressedHeight.text = gifInfo.height.toString()
                                binding.compressedColor.text = gifInfo.gctSize.toString()
                            }

                        }
                    }

                    override fun onFailed(exception: Throwable) {
                        Toast.makeText(this@MainActivity, "onFailed：${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
        GIFCompressor.compress(this, options)
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == PICK_GIF_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                sourceFile = copyToExternalCacheDir(uri)
                sourceFile?.let { file->
                    val gifInfo = GIFMetadataParser().parse(Uri.parse(file.absolutePath))
                    gifInfo?.let { gifInfo->
                        Glide.with(this).load(file).into(binding.preview)
                        binding.size.text = ConvertUtils.byte2FitMemorySize(gifInfo.fileSize)
                        binding.frameCount.text = gifInfo.frameCount.toString()
                        binding.duration.text = ConvertUtils.millis2FitTimeSpan(gifInfo.duration, 5)
                        binding.width.setText("${gifInfo.width}")
                        binding.height.setText("${gifInfo.height}")
                        binding.fps.setText(gifInfo.frameRate.toString())
                        binding.color.setText(gifInfo.gctSize.toString())
                    }
                }
            }
        }
    }

    fun copyToExternalCacheDir(uri: Uri): File? {
        val file: File?
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                val rawFile = UriUtils.uri2File(uri)
                file = File.createTempFile("copied_", ".${FileUtils.getFileExtension(rawFile)}", externalCacheDir)
                file?.deleteOnExit()
                file?.outputStream()?.buffered()?.use {
                    if (inputStream != null) {
                        FileIOUtils.writeFileFromIS(file, inputStream)
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }
        return file
    }

    fun preview(view: View) {
        startActivity(Intent(this, PreviewActivity::class.java).apply {
            putExtra(PreviewActivity.EXTRA_NAME_FILE_PATH, sinkFile?.absolutePath)
        })
    }

}
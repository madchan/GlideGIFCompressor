package com.madchan.glidegifcompressor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.madchan.glidegifcompressor.databinding.ActivityMainBinding
import com.madchan.glidegifcompressor.databinding.ActivityPreviewBinding

class PreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME_FILE_PATH = "file_path"
    }

    private lateinit var binding: ActivityPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Glide.with(this).load(intent.getStringExtra(EXTRA_NAME_FILE_PATH)).into(binding.preview)
    }
}
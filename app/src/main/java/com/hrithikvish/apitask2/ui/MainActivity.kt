package com.hrithikvish.apitask2.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.hrithikvish.apitask2.adapter.StreamingUrlsRVAdapter
import com.hrithikvish.apitask2.databinding.ActivityMainBinding
import com.hrithikvish.apitask2.util.Validator.Companion.isValidYouTubeUrl
import io.github.shalva97.initNewPipe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        hideLoadingBar()
        binding.youtubeUrlEdtLayout.isEndIconVisible = false
        binding.youtubeUrlEdt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.youtubeUrlEdtLayout.isEndIconVisible = !s.isNullOrEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.youtubeUrlEdtLayout.setEndIconOnClickListener {
            binding.youtubeUrlEdt.text?.clear()
        }

        binding.youtubeVideoName.setOnClickListener {
            val maxLinesValue = binding.youtubeVideoName.maxLines

            binding.youtubeVideoName.maxLines = if(maxLinesValue > 2) 2 else Int.MAX_VALUE
        }

        binding.getAudioStreamsBtn.setOnClickListener {
            handleGetStreamsBtnClick(StreamType.AUDIO)
        }

        binding.getVideoStreamsBtn.setOnClickListener {
            handleGetStreamsBtnClick(StreamType.VIDEO)
        }
    }

    private fun handleGetStreamsBtnClick(streamType: StreamType) {
        val adapter = binding.streamsRV.adapter as? StreamingUrlsRVAdapter
        adapter?.upsertAudioStreamsList(emptyList(), StreamType.NULL)
        adapter?.notifyDataSetChanged()

        val youtubeUrlEdtText = binding.youtubeUrlEdt

        if (youtubeUrlEdtText.text != null && isValidYouTubeUrl(youtubeUrlEdtText.text.toString())) {
            showLoadingBar()

            CoroutineScope(Dispatchers.IO).launch {
                getVideoInfo(youtubeUrlEdtText.text.toString().trim(), streamType)
            }
        } else {
            youtubeUrlEdtText.error = "Enter valid url"
            hideLoadingBar()
        }
    }

    private fun getVideoInfo(youtubeUrl: String, streamType: StreamType) {
        initNewPipe()
        val service = ServiceList.YouTube

        val extractor = service.getStreamExtractor(youtubeUrl)
        extractor.fetchPage()

        val audioStreams = extractor.audioStreams
        val sortedAudioStreams = audioStreams.sortedWith(
            compareBy<AudioStream> {
                if (it.format.toString().lowercase() == "m4a") 0 else 1
            }.thenBy {
                it.bitrate
            }
        )

        val videoStreams = extractor.videoStreams
        videoStreams.sortBy { it.quality }

        runOnUiThread {
            binding.youtubeVideoName.text = extractor.name

            val adapter = StreamingUrlsRVAdapter(this@MainActivity)
            when (streamType) {
                StreamType.AUDIO -> adapter.upsertAudioStreamsList(sortedAudioStreams, StreamType.AUDIO)
                StreamType.VIDEO -> adapter.upsertAudioStreamsList(videoStreams, StreamType.VIDEO)
                else -> {}
            }

            binding.streamsRV.layoutManager =
                LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            binding.streamsRV.adapter = adapter
            binding.progressBar.isVisible = false
        }
    }

    private fun showLoadingBar() {
        binding.progressBar.isVisible = true
    }
    private fun hideLoadingBar() {
        binding.progressBar.isVisible = false
    }
}

enum class StreamType {
    AUDIO, VIDEO, NULL
}
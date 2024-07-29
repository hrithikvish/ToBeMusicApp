package com.hrithikvish.apitask2.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.hrithikvish.apitask2.Lyrics
import com.hrithikvish.apitask2.R
import com.hrithikvish.apitask2.XmlPullParserHandler
import com.hrithikvish.apitask2.databinding.UrlsRvItemBinding
import com.hrithikvish.apitask2.ui.StreamType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.InputStream
import java.net.URL

class StreamingUrlsRVAdapter(
    private val context: Context
) : RecyclerView.Adapter<StreamingUrlsRVAdapter.ViewHolder>() {

    private var player: ExoPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private val handler2 = Handler(Looper.getMainLooper())
    private var updateSeekBar: Runnable? = null
    private var updateLyrics: Runnable? = null

    private val listOfAudioOrVideoStreams = mutableListOf<Any>()
    private val listOfSubtitleStreams = mutableListOf<SubtitlesStream>()
    private lateinit var streamType: StreamType

    @SuppressLint("NotifyDataSetChanged")
    fun upsertAudioStreamsList(audioVideoStreamList: List<Any>, subtitleStreamList: List<SubtitlesStream>? = null, streamType: StreamType) {
        listOfAudioOrVideoStreams.clear()
        listOfSubtitleStreams.clear()

        listOfAudioOrVideoStreams.addAll(audioVideoStreamList)
        subtitleStreamList?.let {
            listOfSubtitleStreams.addAll(subtitleStreamList)
        }
        this.streamType = streamType

        notifyDataSetChanged()
    }

    class ViewHolder(val binding: UrlsRvItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = UrlsRvItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = listOfAudioOrVideoStreams.size

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.binding) {
            when (streamType) {
                StreamType.AUDIO -> {

                    val listOfAudioStreams = listOfAudioOrVideoStreams as List<AudioStream>

                    streamFormatTV.text = listOfAudioStreams[position].format.toString()
                    streamQualityTV.text = listOfAudioStreams[position].averageBitrate.toString()
                    streamUrlTV.text = listOfAudioStreams[position].url

                    copyStreamBtn.setOnClickListener {
                        copyStreamUrlToClipboard(listOfAudioStreams[position].url)
                    }
                    openStreamBtn.setOnClickListener {
                        openStreamInBrowser(listOfAudioStreams[position].url)
                    }


                    playPauseIV.tag = R.drawable.ic_play
                    playPauseIV.setOnClickListener {
                        if(playPauseIV.tag == R.drawable.ic_play) {
                            playPauseIV.tag = R.drawable.ic_pause

                            player?.release()
                            player = ExoPlayer.Builder(context).build()

                            streamLoadingBar.isVisible = true
                            playPauseIV.isVisible = false

                            val uri = Uri.parse(listOfAudioStreams[position].url)

                            val mediaItem = MediaItem.fromUri(uri)

                            // Set player listener
                            player?.addListener(object : Player.Listener {
                                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                                    if (playbackState == Player.STATE_READY) {
                                        streamLoadingBar.isVisible = false
                                        playPauseIV.isVisible = true
                                        playPauseIV.setImageResource(R.drawable.ic_pause)

                                        player?.play()
                                        seekBar.max = (player!!.duration / 1000).toInt()
                                        updateSeekBarProgress(seekBar)
                                    }
                                    if (playbackState == Player.STATE_ENDED) {
                                        seekBar.progress = 0
                                        handler.removeCallbacks(updateSeekBar!!)
                                        playPauseIV.setImageResource(R.drawable.ic_play)
                                    }
                                }
                            })
                            player?.setMediaItem(mediaItem)
                            player?.prepare()

                            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { }

                                override fun onStartTrackingTouch(seekBar: SeekBar?) { }

                                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                    seekBar?.let {
                                        if (it.isPressed) {
                                            val touchedPosition = (it.progress * 1000).toLong()
                                            player?.seekTo(touchedPosition)
                                        }
                                    }
                                }
                            })

                            if(listOfSubtitleStreams.isNotEmpty()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val inputStream: InputStream = URL(listOfSubtitleStreams[0].content).openStream()
                                    val parser = XmlPullParserHandler()
                                    val lyricsSegments = parser.parse(inputStream)

                                    (context as Activity).runOnUiThread {
                                        lyricsLayout.isVisible =  true
                                    }
                                    updateTimedLyrics(lyricsSegments, timedLyricsTV)
                                }
                            }
                        }
                        else {
                            playPauseIV.tag = R.drawable.ic_play
                            lyricsLayout.isVisible =  false

                            player?.release()
                            seekBar.progress = 0
                            handler.removeCallbacks(updateSeekBar!!)
                            playPauseIV.setImageResource(R.drawable.ic_play)
                        }
                    }
                }

                StreamType.VIDEO -> {
                    val listOfVideoStreams = listOfAudioOrVideoStreams as List<VideoStream>

                    streamFormatTV.text = listOfVideoStreams[position].format.toString()
                    streamQualityTV.text = listOfVideoStreams[position].getResolution()
                    streamUrlTV.text = listOfVideoStreams[position].url
                    kbpsTV.isVisible = false
                    seekBarLayout.isVisible = false

                    copyStreamBtn.setOnClickListener {
                        copyStreamUrlToClipboard(listOfVideoStreams[position].url)
                    }
                    openStreamBtn.setOnClickListener {
                        openStreamInBrowser(listOfVideoStreams[position].url)
                    }
                }

                StreamType.NULL -> {}
            }
        }
    }

    private fun openStreamInBrowser(url: String?) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url ?: "")
        )
        context.startActivity(browserIntent)
    }

    private fun copyStreamUrlToClipboard(url: String?) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("stream_url", url ?: "")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "stream url copied", Toast.LENGTH_SHORT).show()
    }

    private fun updateSeekBarProgress(seekBar: SeekBar) {
        updateSeekBar = Runnable {
            player?.let {
                seekBar.progress = (it.currentPosition / 1000).toInt()
                handler.postDelayed(updateSeekBar!!, 1000)
            }
        }
        handler.post(updateSeekBar!!)
    }

    private fun updateTimedLyrics(lyricsSegments: List<Lyrics>, timedLyricsTV: TextView) {
        updateLyrics = Runnable {
            player?.let { player ->

                val currentTime = player.currentPosition / 1000.0
                val matchingLyric = lyricsSegments
                    .filter { it.start <= currentTime }
                    .maxByOrNull { it.start }

                timedLyricsTV.text = matchingLyric?.text ?: ""
                handler2.postDelayed(updateLyrics!!, 100)
            }
        }
        handler2.post(updateLyrics!!)
    }
}
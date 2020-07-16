package net.zentring.live

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pedro.encoder.input.gl.render.filters.AndroidViewFilterRender
import com.pedro.encoder.input.gl.render.filters.NoFilterRender
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import kotlinx.android.synthetic.main.activity_live.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import kotlin.math.roundToInt
import kotlin.system.exitProcess


class LiveActivity : AppCompatActivity(), ConnectCheckerRtmp, SurfaceHolder.Callback {
    private var rtmpCamera1: RtmpCamera1? = null
    var streamingUrl = "rtmp://x.rtmp.youtube.com/live2/a343-e0rg-wu8v-dfcv-77a9"
    private var isPaused = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        rtmpCamera1 = RtmpCamera1(preview, this)
        preview.holder.addCallback(this)

        switching_camera.setOnClickListener {
            rtmpCamera1?.switchCamera()
        }
        /*
        Thread(Runnable {
            while (true) {
                runOnUiThread {
                    speed.text = rtmpCamera1!!.bitrate.toString()
                }
                Thread.sleep(1000)
            }
        }).start()*/
        left_button.setOnClickListener {
            if (!rtmpCamera1!!.isStreaming) {
                if (rtmpCamera1!!.isRecording || rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()) {
                    rtmpCamera1!!.startStream(streamingUrl)
                    //Starting stream

                    left_button.background =
                        ContextCompat.getDrawable(this, R.drawable.pause_streaming)
                    left_button.text = "暫停直播"
                    left_button.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
            } else {
                if (isPaused) {
                    isPaused = false
                    left_button.text = "開始直播"
                    left_button.setTextColor(ContextCompat.getColor(this, R.color.purple))
                    left_button.background =
                        ContextCompat.getDrawable(this, R.drawable.start_streaming)

                    continue_streaming_button.visibility = View.INVISIBLE
                    rtmpCamera1!!.stopStream()
                    rtmpCamera1!!.glInterface.setFilter(NoFilterRender())
                } else {

                    //left_button.background = ContextCompat.getDrawable(this, R.drawable.pause_streaming)

                    continue_streaming_button.visibility = View.VISIBLE
                    left_button.text = "停止直播"

                    val filter = AndroidViewFilterRender()
                    filter.view = pausedScene

                    rtmpCamera1!!.glInterface.setFilter(filter)
                    isPaused = true
                }
            }
        }
        continue_streaming_button.setOnClickListener {
            if (isPaused) {
                isPaused = false
                continue_streaming_button.visibility = View.INVISIBLE

                left_button.text = "暫停直播"
                rtmpCamera1!!.glInterface.setFilter(NoFilterRender())
            }
        }
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {

    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        if (!rtmpCamera1!!.isStreaming) {
            if (rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()) {
                rtmpCamera1!!.stopPreview()
                rtmpCamera1!!.startPreview()

            }
        }
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        if (rtmpCamera1!!.isRecording) {
            rtmpCamera1!!.stopRecord()
        }
        if (rtmpCamera1!!.isStreaming) {
            rtmpCamera1!!.stopStream()
        }
        rtmpCamera1!!.stopPreview()
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        var bitrateToShow = bitrate
        runOnUiThread {

            if (bitrate <= 1024) {
                speed.text = "$bitrate bps"
            } else {
                var bitrateKb: Double = bitrateToShow / 1024.0
                bitrateKb = (bitrateKb * 10).roundToInt() / 10.0
                if (bitrateKb <= 1024) {
                    speed.text = "$bitrateKb kbps"
                } else {
                    var bitrateMb: Double = bitrateKb / 1024.0
                    bitrateMb = (bitrateMb * 10).roundToInt() / 10.0
                    speed.text = "$bitrateMb Mbps"
                }
            }
        }
    }

    override fun onConnectionSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this, "串流中", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                "Connection failed. $reason",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
            speed.text = "0 kbps"
            Toast.makeText(this, "已中斷連線", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthErrorRtmp() {
        runOnUiThread {
            Toast.makeText(this, "Auth error", Toast.LENGTH_SHORT).show()
            rtmpCamera1?.stopStream()
        }
    }

    override fun onAuthSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exitProcess(0)
    }

}
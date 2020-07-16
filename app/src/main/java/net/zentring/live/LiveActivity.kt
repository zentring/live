package net.zentring.live

import android.content.res.Resources
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pedro.encoder.input.gl.render.filters.BlackFilterRender
import com.pedro.encoder.input.gl.render.filters.NoFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import kotlinx.android.synthetic.main.activity_live.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import kotlin.math.roundToInt
import kotlin.system.exitProcess


class LiveActivity : AppCompatActivity(), ConnectCheckerRtmp, SurfaceHolder.Callback {
    private var rtmpCamera1: RtmpCamera1? = null
    var streamingUrl = "rtmps://live-api-s.facebook.com:443/rtmp/1725492044269366?s_bl=1&s_sc=1725492074269363&s_sw=0&s_vt=api-s&a=AbzoQBAM6dIREz_E"
    private var isPaused = false
    var width = Resources.getSystem().displayMetrics.widthPixels // round
    var height = Resources.getSystem().displayMetrics.heightPixels
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        rtmpCamera1 = RtmpCamera1(preview, this)
        preview.holder.addCallback(this)

        switching_camera.setOnClickListener {
            rtmpCamera1?.switchCamera()
        }

        Thread(Runnable {
            while (true) {
                runOnUiThread {
                    //rtmpCamera1!!.glInterface.setFilter(filter)
                }
                Thread.sleep(1000)
            }
        }).start()

        left_button.setOnClickListener {
            if (!rtmpCamera1!!.isStreaming) {
                if (rtmpCamera1!!.isRecording || rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()
                ) {
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
                    rtmpCamera1!!.glInterface.setFilter(1, NoFilterRender())
                    rtmpCamera1!!.glInterface.setFilter(2, NoFilterRender())
                } else {

                    continue_streaming_button.visibility = View.VISIBLE
                    left_button.text = "停止直播"

                    rtmpCamera1!!.glInterface.setFilter(BlackFilterRender())
                    var tf = ImageObjectFilterRender()
                    tf.setImage(textAsBitmap("直播暂停，稍后回来！", 90f))
//                    tf.setPosition(
//                        0f,
//                        Resources.getSystem().displayMetrics.heightPixels.toFloat() / 2
//                    )
                    //tf.setScale()

                    rtmpCamera1!!.glInterface.setFilter(1, tf)

                    var logo = ImageObjectFilterRender()
                    logo.setImage(BitmapFactory.decodeResource(resources, R.drawable.logo))

                    //Max is 20f
                    logo.setPosition((20f - ((400f / width) * 20)) / 2, 2f)

                    //logo.setPosition(0f, 0f)
                    logo.setScale((400f / width) * 100, (400f / height) * 100)
                    rtmpCamera1!!.glInterface.setFilter(2, logo)
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
                rtmpCamera1!!.glInterface.setFilter(1, NoFilterRender())
                rtmpCamera1!!.glInterface.setFilter(2, NoFilterRender())
            }
        }
    }

    private fun textAsBitmap(
        text: String,
        textSize: Float,
        textColor: Int = Color.WHITE,
        typeface: Typeface? = null,
        isSizeMatchedScreen: Boolean = true
    ): Bitmap? {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.color = textColor
        paint.alpha = 255
        if (typeface != null) paint.typeface = typeface
        paint.textAlign = Paint.Align.LEFT
        val baseline = -paint.ascent() // ascent() is negative
        var textW = paint.measureText(text)
        var textH = baseline + paint.descent()

        var width = (textW + 0.5f).toInt() // round
        var height = (textH + 0.5f).toInt()

        if (isSizeMatchedScreen) {
            width = Resources.getSystem().displayMetrics.widthPixels // round
            height = Resources.getSystem().displayMetrics.heightPixels
        }
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (isSizeMatchedScreen) {
            canvas.drawText(
                text,
                ((width - textW) / 2),
                ((height - textH) / 2) + textH,
                paint
            )
        } else {
            canvas.drawText(text, 0f, baseline, paint)
        }
        return image
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
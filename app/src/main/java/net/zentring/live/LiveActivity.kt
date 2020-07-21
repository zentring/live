package net.zentring.live

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pedro.encoder.input.gl.render.filters.BlackFilterRender
import com.pedro.encoder.input.gl.render.filters.NoFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import kotlinx.android.synthetic.main.activity_live.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess


private const val PICK_LOGO_CODE = 1024

class LiveActivity : AppCompatActivity(), ConnectCheckerRtmp, SurfaceHolder.Callback {
    private var rtmpCamera1: RtmpCamera1? = null


    private var isPaused = false
    var width = Resources.getSystem().displayMetrics.widthPixels
    var height = Resources.getSystem().displayMetrics.heightPixels
    var logo: Bitmap? = null
    var logo2: Bitmap? = null
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_LOGO_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_LOGO_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return
            }
            val inputStream: InputStream =
                this.contentResolver.openInputStream(data.data!!)!!
            logo = BitmapFactory.decodeStream(inputStream)

            this.logo2 = this.logo!!.copy(this.logo!!.config, true)
            //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        videoList.visibility = View.INVISIBLE
        main_control.visibility = View.VISIBLE
        close_video_selector.setOnClickListener {
            videoList.visibility = View.INVISIBLE
            main_control.visibility = View.VISIBLE
        }

        val returnIntent = Intent()
        returnIntent.putExtra("exit", true)
        setResult(Activity.RESULT_OK, returnIntent)

        if (!File(data.TEMP_PATH, "situne/live").exists()) {
            File(data.TEMP_PATH, "situne/live").mkdirs()
        }


        select.setOnClickListener {
            videoList.visibility = View.VISIBLE
            main_control.visibility = View.INVISIBLE
        }

        rtmpCamera1 = RtmpCamera1(preview, this)
        rtmpCamera1!!.setReTries(100)

        floatingVolume.translationY = -50f
        preview.holder.addCallback(this)

        switching_camera.setOnClickListener {
            rtmpCamera1?.switchCamera()
        }
        speakerToggle.setOnClickListener {
            if (volume.visibility == View.VISIBLE) {
                volume.visibility = View.INVISIBLE
                floatingVolume.visibility = View.INVISIBLE
                speakerToggle.background =
                    ContextCompat.getDrawable(this, R.drawable.speaker_background_disabled)
            } else {
                volume.visibility = View.VISIBLE
                floatingVolume.visibility = View.VISIBLE

                speakerToggle.background =
                    ContextCompat.getDrawable(this, R.drawable.speaker_background)

            }
        }


        var audioEffect = AudioVolume()
        volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, value: Int, p2: Boolean) {
                audioEffect.volume = value
                rtmpCamera1!!.setCustomAudioEffect(audioEffect)
                var xPosition =
                    (((volume.right - volume.left) / volume.max) * volume.progress)// + volume.left
                floatingVolume.translationY = -(xPosition.toFloat() - (floatingVolume.width / 2))
                floatingVolume.text = value.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }
        })
        Thread(Runnable {
            while (true) {
                runOnUiThread {
                    if (!rtmpCamera1!!.isStreaming) {
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
                    }
                    rtmpCamera1!!.setCustomAudioEffect(audioEffect)
                    volumeRealTime.progress = audioEffect.volume
                    Log.d("VU", audioEffect.volume.toString())
                    if (isPaused) {
                        pausedText.visibility = View.VISIBLE
                    } else {
                        pausedText.visibility = View.INVISIBLE
                    }
                    //rtmpCamera1!!.glInterface.setFilter(filter)
                }
                Thread.sleep(100)
            }
        }).start()
        preview.setOnClickListener {
            if (isPaused) {
                pickImage()
            }
        }
        left_button.setOnClickListener {
            if (!rtmpCamera1!!.isStreaming) {
                if (rtmpCamera1!!.isRecording || rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()
                ) {
                    if (data.isDebug) {
                        rtmpCamera1!!.startStream("rtmp://x.rtmp.youtube.com/live2/mkes-2ytx-d8rc-bcmm-a0dc")
                    } else {
                        rtmpCamera1!!.startStream(data.pushurl)
                    }
                    rtmpCamera1!!.startRecord("${data.TEMP_PATH.absolutePath}/situne/live/" + System.currentTimeMillis() + ".mp4") {

                    }
                    //Starting stream
                    if (data.targetrate != "null") {
                        rtmpCamera1!!.setVideoBitrateOnFly(data.targetrate!!.toInt())
                    }
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
                    pausedText.visibility = View.INVISIBLE
                } else {

                    continue_streaming_button.visibility = View.VISIBLE
                    left_button.text = "停止直播"
                    pausedText.visibility = View.VISIBLE

                    rtmpCamera1!!.glInterface.setFilter(BlackFilterRender())
                    var tf = ImageObjectFilterRender()
                    tf.setImage(textAsBitmap(pausedText.text.toString(), 90f))

                    rtmpCamera1!!.glInterface.setFilter(1, tf)

                    var logo = ImageObjectFilterRender()
                    if (this.logo == null) {
                        logo.setImage(BitmapFactory.decodeResource(resources, R.drawable.pause))
                    } else {
                        if (this.logo!!.isRecycled) {
                            this.logo = this.logo2!!.copy(this.logo2!!.config, true)

                            //logo.setImage(BitmapFactory.decodeResource(resources, R.drawable.pause))
                        } else {
                        }
                        logo.setImage(this.logo)
                    }
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
                pausedText.visibility = View.INVISIBLE
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
                speed.setTextColor(Color.RED)
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
                when {
                    bitrateKb <= 100 -> {
                        speed.setTextColor(Color.RED)
                    }
                    bitrateKb <= 1200 -> {
                        speed.setTextColor(Color.argb(255, 255, 165, 0))
                    }
                    else -> {
                        speed.setTextColor(Color.GREEN)
                    }
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
            speed.text = "0 kbps"
            speed.setTextColor(Color.WHITE)
            if (rtmpCamera1!!.reTry(5000, reason)) {
                Toast.makeText(this, "連線丟失，重新連線", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    this, "連線失敗\n $reason", Toast.LENGTH_SHORT
                ).show()
                rtmpCamera1!!.stopStream()
            }

            rtmpCamera1!!.reTry(0, reason)
            //rtmpCamera1!!.reConnect(0)
        }
    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
            speed.text = "0 kbps"
            speed.setTextColor(Color.WHITE)
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
    }

    private val NONE = 0 // 原始

    private val DRAG = 1 // 拖动

    private val ZOOM = 2 // 放大

    private var mStartDistance = 0f

    private val mStartPoint = PointF()

    private var mStatus: Int = NONE

    private fun spacing(event: MotionEvent): Float {
        var x = event.getX(0) - event.getX(1)
        var y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        when (event!!.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mStartPoint.set(event.x, event.y)
                mStatus = DRAG;
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                var distance: Float = spacing(event)
                if (distance > 10f) {

                    mStatus = ZOOM
                    mStartDistance = distance
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mStatus == DRAG) {
                    //dragAction(event)
                } else {
                    if (event.pointerCount == 1) {
                        return true
                    }
                    rtmpCamera1!!.setZoom(event)
                    //zoomAction(event)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                mStatus = NONE
            }
        }

        return true
    }


}
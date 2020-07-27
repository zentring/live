package net.zentring.live

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Constraints
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.daasuu.mp4compose.composer.Mp4Composer
import com.pedro.encoder.input.decoder.AudioDecoderInterface
import com.pedro.encoder.input.decoder.VideoDecoderInterface
import com.pedro.encoder.input.gl.render.filters.BlackFilterRender
import com.pedro.encoder.input.gl.render.filters.NoFilterRender
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtplibrary.rtmp.RtmpCamera1
import com.pedro.rtplibrary.rtmp.RtmpFromFile
import idv.luchafang.videotrimmer.VideoTrimmerView
import kotlinx.android.synthetic.main.activity_live.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import net.zentring.live.data.Companion.instance
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt


private const val PICK_LOGO_CODE = 1024

class LiveActivity : AppCompatActivity(), ConnectCheckerRtmp, SurfaceHolder.Callback,
    VideoDecoderInterface, AudioDecoderInterface,
    VideoTrimmerView.OnSelectedRangeChangedListener, View.OnClickListener, Mp4Composer.Listener,
    View.OnTouchListener {
    private var rtmpCamera1: RtmpCamera1? = null
    private var rtmpFile: RtmpFromFile? = null

    private var isPaused = false
    var width = Resources.getSystem().displayMetrics.widthPixels
    var height = Resources.getSystem().displayMetrics.heightPixels
    var logo: Bitmap? = null
    var logo2: Bitmap? = null
    var sdPreviewPlayer: MediaPlayer? = null
    var bigPreviewLayoutParam: ConstraintLayout.LayoutParams? = null
    var smallPreviewLayoutParam: ConstraintLayout.LayoutParams? = null

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

    private fun cutVideoToTempPath(source: String, target: String, start: Long, end: Long) {
        Mp4Composer(
            source,
            target
        ).trim(start, end).listener(this).start()
    }

    override fun onProgress(progress: Double) {
        Log.d("", "onProgress = $progress")
    }

    override fun onCanceled() {

    }

    override fun onCompleted() {
        if (data.isCutFromBuffer) {
            data.isCutFromBuffer = false
            data.isInCutPage = true


            var tmpFile = File(data.getTempPath(), "tmp.mp4")
            var tmpFile2 = File(data.getTempPath(), "tmp2.mp4")
            tmpFile.copyTo(tmpFile2, true)

            Thread {

                sdPreviewPlayer = MediaPlayer()
                sdPreviewPlayer!!.setDataSource(tmpFile2.absolutePath)
                sdPreviewPlayer!!.prepare()
                sdPreviewPlayer!!.setDisplay(sdVideoPreview.holder)
                var d = sdPreviewPlayer!!.duration
                sdPreviewPlayer!!.start()
                sdPreviewPlayer!!.pause()

                runOnUiThread {
                    data.currentCutVideoStart = 0
                    data.currentCutVideoEnd = d.toLong()
                    videoTotalTime.text = (round(d / 100.0) / 10.0).toString() + "s"
                    cutedTime.text = (round(d / 100.0) / 10.0).toString() + "s"
                    root.removeView(rtmpCameraPreview)
                    root.addView(rtmpCameraPreview)
                    rtmpCameraPreview.requestLayout()
                    rtmpCameraPreview.invalidate()
                    rtmpCameraPreview.bringToFront()
                    PGM.bringToFront()
                }
            }.start()

            runOnUiThread {
                videoTrimmerView
                    .setVideo(tmpFile)
                    .setMaxDuration(120000)                 // millis
                    .setMinDuration(100)                    // millis
                    .setFrameCountInWindow(12)
                    .setExtraDragSpace(40f)                 // pixels
                    .setOnSelectedRangeChangedListener(this)
                    .show()
                sdVideoPreview.visibility = View.VISIBLE
            }
        } else {
            runOnUiThread {
                Toast.makeText(this, "剪輯完成", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onFailed(exception: Exception?) {
        Log.e("MP4", "onFailed()", exception)

        runOnUiThread {
            Toast.makeText(this, "裁切檔案失敗", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClick(view: View) {
        if (data.isSelectEnabled) {
            if (view.tag.toString() in data.selectedVideoList) {
                data.selectedVideoList.remove(view.tag.toString())
                view.setBackgroundColor(Color.TRANSPARENT)
            } else {
                data.selectedVideoList.add(view.tag.toString())
                view.setBackgroundColor(Color.LTGRAY)
            }
        } else {
            runOnUiThread {
                rtmpCamera1?.stopPreview()
                setVideoEditScreen()
                hideMainController()
                hideSideVideoSelector()
                var file = File(data.getSiTunePath(), "${view.tag}.mp4")
                var tmpmp4 = File(getExternalFilesDir(null), "/tmp/filetocut.mp4")
                file.copyTo(tmpmp4, true)

                Thread(Runnable {
                    sdPreviewPlayer = MediaPlayer()
                    sdPreviewPlayer!!.setDataSource(tmpmp4.absolutePath)
                    sdPreviewPlayer!!.prepare()
                    var dr = round(sdPreviewPlayer!!.duration / 100.0) / 10.0
                    runOnUiThread {
                        videoTotalTime.text = dr.toString() + "s"
                        cutedTime.text = videoTotalTime.text
                    }
                    sdPreviewPlayer!!.setDisplay(sdVideoPreview.holder)
                    sdPreviewPlayer!!.start()
                    sdPreviewPlayer!!.pause()
                }).start()
                data.currentCutVideoName = view.tag.toString()
                videoTrimmerView
                    .setVideo(file)
                    .setMaxDuration(600000_000)             // millis
                    .setMinDuration(100)                    // millis
                    .setFrameCountInWindow(12)
                    .setExtraDragSpace(40f)                 // pixels
                    .setOnSelectedRangeChangedListener(this)
                    .show()

            }
        }
    }

    override fun onSelectRangeStart() {
        // Start to drag range bar or start to scroll the video frame list
    }

    override fun onSelectRange(startMillis: Long, endMillis: Long) {
        // Range is changing
        data.currentCutVideoStart = startMillis
        data.currentCutVideoEnd = endMillis
        cutedTime.text = (round((endMillis - startMillis) / 100.0) / 10.0).toString() + "s"
    }

    override fun onSelectRangeEnd(startMillis: Long, endMillis: Long) {
        // Range selected, play the video here
    }

    private fun hideMainController() {
        main_control.visibility = View.INVISIBLE
    }

    private fun showMainController() {
        main_control.visibility = View.VISIBLE
    }

    private fun setVideoEditScreen() {
        //rtmpCamera1?.stopPreview()
        sdVideoPreview.visibility = View.VISIBLE
        editControllerFrame.visibility = View.VISIBLE
        goLiveButton.visibility = View.VISIBLE
        editControllerFrame.bringToFront()
        //goLiveButton.bringToFront()
    }

    private fun dpToPx(dp: Float): Int {

        val r = resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            r.displayMetrics
        ).toInt()

    }

    private fun initViewLayout() {
        bigPreviewLayoutParam = rtmpCameraPreview.layoutParams as ConstraintLayout.LayoutParams?
        smallPreviewLayoutParam = ConstraintLayout.LayoutParams(
            dpToPx(160f), dpToPx(90f)
        )

        (smallPreviewLayoutParam as ConstraintLayout.LayoutParams).endToEnd =
            Constraints.LayoutParams.PARENT_ID
        (smallPreviewLayoutParam as ConstraintLayout.LayoutParams).topToTop =
            Constraints.LayoutParams.PARENT_ID
        (smallPreviewLayoutParam as ConstraintLayout.LayoutParams).topMargin = dpToPx(16f)
        (smallPreviewLayoutParam as ConstraintLayout.LayoutParams).marginEnd = dpToPx(16f)

        videoList.visibility = View.INVISIBLE
        main_control.visibility = View.VISIBLE
        sdVideoPreview.visibility = View.INVISIBLE
        editControllerFrame.visibility = View.INVISIBLE
        saveFileDialog.visibility = View.INVISIBLE
        goLiveButton.visibility = View.INVISIBLE
        PGM.visibility = View.INVISIBLE
        returnToLive.visibility = View.INVISIBLE
        setting.visibility = View.INVISIBLE

        rtmpCameraPreview.setOnTouchListener(this)

    }

    private fun initClickListener() {
        close_video_selector.setOnClickListener {
            hideSideVideoSelector()
            showMainController()
        }

        upload.setOnClickListener {
            uploadOnClick()
        }

        multiple_select.setOnClickListener {
            multipleSelectOnClick()
        }

        select.setOnClickListener {
            openSideVideos()
        }

        switching_camera.setOnClickListener {
            rtmpCamera1?.switchCamera()
        }

        speakerToggle.setOnClickListener {
            toggleVolumeBar()
        }

        continue_streaming_button.setOnClickListener {
            continueStreamingOnClick()
        }

        playPause.setOnClickListener {
            playPauseOnClick()
        }

        giveup.setOnClickListener {
            //sdPreviewPlayer?.release()
            goHome()
        }

        left_button.setOnClickListener {
            leftButtonOnClick()
        }

        save.setOnClickListener {
            saveEditedVideo()
        }

        file_dialog_ok.setOnClickListener {
            processSaveCutedFile()
        }

        file_dialog_cancle.setOnClickListener {
            hideSaveFileDialog()
        }

        overwritten_yes.setOnClickListener {
            overwrittenYES()
        }

        overwritten_no.setOnClickListener {
            overwrittenNO()
        }

        cut.setOnClickListener {
            //Toast.makeText(this, "vgyhy", Toast.LENGTH_SHORT).show()
            switchToBufferEdit()
        }

        goLiveButton.setOnClickListener {
            fileToLive()
        }

        returnToLive.setOnClickListener {
            returnToLive()
        }

        settingBtn.setOnClickListener {
            setting.visibility = View.VISIBLE
        }
    }

    private fun returnToLive() {
        if (rtmpFile!!.isStreaming) {
            rtmpFile!!.stopStream()
        }
        rtmpCamera1!!.startPreview()
        rtmpFilePreview.visibility = View.INVISIBLE
        rtmpCameraPreview.visibility = View.VISIBLE
        returnToLive.visibility = View.INVISIBLE
    }

    private fun fileToLive() {
        var file = File(data.getSiTunePath(), data.currentCutVideoName + ".mp4")
        var fileCut = File(data.getTempPath(), "tmp.mp4")
        if (rtmpFile!!.isStreaming) {
            rtmpFile!!.stopStream()
        }
        if (data.isInCutPage) {
            if (rtmpFile!!.prepareAudio(fileCut.absolutePath) && rtmpFile!!.prepareVideo(fileCut.absolutePath)) {
                rtmpFile!!.startStream(data.pushurl)
                editControllerFrame.visibility = View.INVISIBLE
                main_control.visibility = View.VISIBLE
                returnToLive.visibility = View.VISIBLE
                goLiveButton.visibility = View.INVISIBLE
                sdVideoPreview.visibility = View.INVISIBLE

                Thread {
                    Thread.sleep(50)
                    while (rtmpFile!!.isStreaming) {
                        runOnUiThread {
                            returnToLive.text =
                                "切回直播\n" + (round(-(rtmpFile!!.videoTime - rtmpFile!!.videoDuration) * 10) / 10.0) + "s"
                        }
                        Thread.sleep(100)
                    }
                }.start()

            } else {
                Toast.makeText(this, "無法初始化播放器", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (rtmpFile!!.prepareAudio(file.absolutePath) && rtmpFile!!.prepareVideo(file.absolutePath)) {

                rtmpFile!!.startStream(data.pushurl)
                editControllerFrame.visibility = View.INVISIBLE
                main_control.visibility = View.VISIBLE
                returnToLive.visibility = View.VISIBLE
                goLiveButton.visibility = View.INVISIBLE
                sdVideoPreview.visibility = View.INVISIBLE

                Thread {
                    Thread.sleep(50)
                    while (rtmpFile!!.isStreaming) {
                        runOnUiThread {
                            returnToLive.text =
                                "切回直播\n" + (round(-(rtmpFile!!.videoTime - rtmpFile!!.videoDuration) * 10) / 10.0) + "s"
                        }
                        Thread.sleep(100)
                    }
                }.start()

            } else {
                Toast.makeText(this, "無法初始化播放器", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun switchToBufferEdit() {
        if (!rtmpCamera1!!.isStreaming) {
            Toast.makeText(this, "直播後才有畫面可剪輯", Toast.LENGTH_SHORT).show()
            return
        }
        rtmpCameraPreview.layoutParams = smallPreviewLayoutParam
        sdVideoPreview.visibility = View.VISIBLE
        editControllerFrame.visibility = View.VISIBLE
        editControllerFrame.bringToFront()

        PGM.visibility = View.VISIBLE
        //rtmpCameraPreview.bringToFront()
        PGM.bringToFront()

        var bufferFiles = data.getBufferVideoPath()

        if (bufferFiles.exists()) {

            rtmpCamera1!!.stopRecord()
            //rtmpCamera1!!.stopStream()

            //rtmpCamera1?.stopPreview()
            setVideoEditScreen()
            hideMainController()
            hideSideVideoSelector()
            var file = data.getBufferVideoPath()
            var tmpMP4 = File(data.getTempPath(), "/tmp.mp4")


            try {
                val mp = MediaPlayer()
                mp.setDataSource(file.absolutePath)
                mp.prepare()
                mp.setOnPreparedListener { mediaPlayer ->
                    val time: Int = mediaPlayer.duration
                    mediaPlayer.release()

                    data.isCutFromBuffer = true
                    if (time < 120000) {
                        Mp4Composer(
                            file.absolutePath,
                            tmpMP4.absolutePath
                        ).trim(0, time.toLong()).listener(this).start()
                    } else {
                        Mp4Composer(
                            file.absolutePath,
                            tmpMP4.absolutePath
                        ).trim((time - 120000).toLong(), time.toLong()).listener(this).start()
                    }
                }
            } catch (e: Exception) {

            }

        } else {
            Toast.makeText(this, "無資源可剪輯", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processSaveCutedFile() {
        var title = file_dialog_title.text.toString()
        var target = file_dialog_filename.text.toString()
        if (target == "") {
            Toast.makeText(this, "請填寫檔案名稱", Toast.LENGTH_SHORT).show()
        }
        hideSaveFileDialog()
        if (File(data.getSiTunePath(), "$target.mp4").exists()) {
            showOverwritten()
        } else {
            if (data.isInCutPage) {
                data.isInCutPage = false
                cutVideoToTempPath(
                    File(data.getTempPath(), "tmp.mp4").absolutePath,
                    File(data.getSiTunePath(), "$target.mp4").absolutePath,
                    data.currentCutVideoStart,
                    data.currentCutVideoEnd
                )
//                File(data.getTempPath(), "tmp.mp4").copyTo(
//                    File(
//                        data.getSiTunePath(),
//                        "$target.mp4"
//                    ), true
//                )
            } else {
                cutVideoToTempPath(
                    File(data.getSiTunePath(), "$title.mp4").absolutePath,
                    File(data.getSiTunePath(), "$target.mp4").absolutePath,
                    data.currentCutVideoStart,
                    data.currentCutVideoEnd
                )
            }
            Toast.makeText(this, "正在裁減檔案", Toast.LENGTH_SHORT).show()
            goHome()
        }
    }

    private fun overwrittenNO() {
        hideOverwritten()
        showSaveFileDialog(file_dialog_title.text.toString(), true)
    }

    private fun overwrittenYES() {
        var title = file_dialog_title.text.toString()
        var target = file_dialog_filename.text.toString()

        var fileTmp = File(data.getTempPath(), "/video_overwritten.mp4")
        File(data.getSiTunePath(), "$title.mp4").copyTo(fileTmp)
        cutVideoToTempPath(
            fileTmp.absolutePath,
            File(data.getSiTunePath(), "$target.mp4").absolutePath,
            data.currentCutVideoStart,
            data.currentCutVideoEnd
        )
        Toast.makeText(this, "正在裁減檔案", Toast.LENGTH_SHORT).show()
        hideOverwritten()
        goHome()
    }

    private fun showOverwritten() {
        dialog_overwritten.visibility = View.VISIBLE
    }

    private fun hideOverwritten() {
        dialog_overwritten.visibility = View.INVISIBLE
    }

    private fun saveEditedVideo() {
        if (data.isInCutPage) {
            showSaveFileDialog("", false)
            file_dialog_filename.setText("")
        } else {
            showSaveFileDialog(data.currentCutVideoName, false)
            file_dialog_filename.setText(data.currentCutVideoName)
        }
    }

    private fun showSaveFileDialog(filename: String, cancel: Boolean) {
        saveFileDialog.visibility = View.VISIBLE
        if (!cancel) {
            file_dialog_filename.setText("")
        }
        file_dialog_title.text = filename

        val sdf = SimpleDateFormat("hh:mm:ss")
        val currentDate = sdf.format(Date())
        file_dialog_time.text = "結束時間：$currentDate"
    }

    private fun hideSaveFileDialog() {
        saveFileDialog.visibility = View.INVISIBLE
    }

    private fun leftButtonOnClick() {
        if (!rtmpCamera1!!.isStreaming) {
            val rotation = CameraHelper.getCameraOrientation(this)

            if (rtmpCamera1!!.prepareAudio() &&
                rtmpCamera1!!.prepareVideo(
                    data.resolution[0],
                    data.resolution[1],
                    30,
                    12000 * 1024,
                    false,
                    rotation
                )
            ) {

                rtmpCamera1!!.stopPreview()
//                Toast.makeText(
//                    this,
//                    "" + data.resolution[0] + "x" + data.resolution[1],
//                    Toast.LENGTH_SHORT
//                ).show()

                rtmpCamera1!!.startPreview(data.resolution[0], data.resolution[1])
                if (data.isDebug) {
                    rtmpCamera1!!.startStream("rtmp://x.rtmp.youtube.com/live2/mkes-2ytx-d8rc-bcmm-a0dc")
                } else {
                    rtmpCamera1!!.startStream(data.pushurl)
                }

                rtmpCamera1!!.startRecord(data.getBufferVideoPath().absolutePath) {

                }
                //Starting stream
                if (data.targetrate != "null" && data.targetrate != null) {
                    if (data.targetrate?.toInt() == null) {
//                        Toast.makeText(this, data.targetrate.toString(), Toast.LENGTH_SHORT)
//                            .show()
                    } else {
                        rtmpCamera1!!.setVideoBitrateOnFly(data.targetrate?.toInt()!!)
                    }
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
                setPauseFilter()
                isPaused = true
            }
        }
    }

    private fun setPauseFilter() {

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
    }

    private fun goHome() {
        playPause.setImageResource(R.drawable.play)
        rtmpCamera1!!.startPreview()
        main_control.visibility = View.VISIBLE
        rtmpCameraPreview.layoutParams = bigPreviewLayoutParam
        sdVideoPreview.visibility = View.INVISIBLE
        editControllerFrame.visibility = View.INVISIBLE
        PGM.visibility = View.INVISIBLE
        goLiveButton.visibility = View.INVISIBLE
        returnToLive.visibility = View.INVISIBLE

        rtmpCameraPreview.bringToFront()
        PGM.bringToFront()
        main_control.bringToFront()

    }

    var pausedTime = 0
    private fun playPauseOnClick() {
        if (sdPreviewPlayer!!.isPlaying) {
            sdPreviewPlayer!!.pause()
            pausedTime = sdPreviewPlayer!!.currentPosition
            playPause.setImageResource(R.drawable.play)
        } else {
            sdPreviewPlayer!!.seekTo(pausedTime)
            sdPreviewPlayer!!.start()
            playPause.setImageResource(R.drawable.pause)
        }
    }

    private fun hideSideVideoSelector() {
        videoList.visibility = View.INVISIBLE
    }

    private fun showSideVideoSelector() {
        videoList.visibility = View.INVISIBLE
        showMainController()
    }

    private fun continueStreamingOnClick() {
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

    private fun uploadOnClick() {
        if (data.selectedVideoList.size == 0) {
            Toast.makeText(this, "請至少選擇一部影片上傳", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "上傳功能尚未實作", Toast.LENGTH_SHORT).show()
        }

    }

    private fun multipleSelectOnClick() {
        if (data.isSelectEnabled) {
            data.isSelectEnabled = false
            multiple_select.setTextColor(Color.WHITE)

            video_files.children.forEach {
                it.setBackgroundColor(Color.TRANSPARENT)
            }
            data.selectedVideoList = MutableList(0) { "" }
        } else {
            data.isSelectEnabled = true
            multiple_select.setTextColor(Color.DKGRAY)
        }
    }

    private fun openSideVideos() {
        goHome()
        videoList.visibility = View.VISIBLE
        videoList.bringToFront()
        main_control.visibility = View.INVISIBLE
        main_control.bringToFront()

        data.isSelectEnabled = false
        data.selectedVideoList = MutableList(0) { "" }

        video_files.removeAllViews()
        Thread(Runnable {
            var i = 0
            listSiTuneFiles().forEach {
                if (it.isFile) {
                    try {
                        val mp = MediaPlayer()
                        mp.setDataSource(it.absolutePath)
                        mp.prepare()
                        mp.setOnPreparedListener { mediaPlayer ->
                            i++
                            val time: Int = mediaPlayer.duration
                            runOnUiThread {
                                addVideoToList(it.nameWithoutExtension, (time / 1000), i)
                            }
                            mediaPlayer.release()

                        }
                        mp.setOnErrorListener { mediaPlayer, what, extra ->
                            i++
                            runOnUiThread {
                                addVideoToList(it.nameWithoutExtension, -1, i)
                            }
                            mediaPlayer.release()

                            false
                        }
                    } catch (e: Exception) {

                    }
                }
            }
            runOnUiThread {
                totalText.text = "(共 $i 個)"
            }

        }).start()
    }

    private fun toggleVolumeBar() {
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

    private fun initRTMPComponent() {
        rtmpCamera1 = RtmpCamera1(rtmpCameraPreview, this)
        rtmpCamera1!!.setReTries(100)

        rtmpFile = RtmpFromFile(rtmpFilePreview, this, this, this)
        rtmpCameraPreview.holder.addCallback(this)

        var resolutions = rtmpCamera1!!.resolutionsBack
        resolutions.forEach {
            data.resolutions.add(arrayOf(it.width, it.height))
            println("" + it.width + " " + it.height)
        }
        setting.setSettingResolutions()
        resolutions.sortBy { it.width }
        var resolution =
            arrayOf(
                resolutions[resolutions.size - 2].width,
                resolutions[resolutions.size - 2].height
            )
        println(resolution[0])
        println(resolution[1])

        rtmpCameraPreview.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rtmpCameraPreview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                var r = data.resolution[0].toDouble() / data.resolution[1].toDouble()
                setCameraScale(r)
            }
        })
        setDefaultPreviewSize()
    }

    fun setDefaultPreviewSize() {
        val r = 1920.0 / 1080
        data.resolution = arrayOf(1920, 1080)
        setCameraScale(r)
    }

    fun setCameraScale(r: Double) {
        rtmpCameraPreview.layoutParams.width = (rtmpCameraPreview.height * r).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data.instance = this
        setContentView(R.layout.activity_live)

        toggleVolumeBar()
        initViewLayout()
        initClickListener()

        val returnIntent = Intent()
        returnIntent.putExtra("exit", true)
        setResult(Activity.RESULT_OK, returnIntent)

        if (!File(data.getStoragePath(), "situne/live").exists()) {
            File(data.getStoragePath(), "situne/live").mkdirs()
        }
        if (!data.getTempPath().exists()) {
            data.getTempPath().mkdirs()
        }

        floatingVolume.translationY = -50f

        initRTMPComponent()

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

                        rtmpCamera1!!.glInterface.setFilter(NoFilterRender())
                        rtmpCamera1!!.glInterface.setFilter(1, NoFilterRender())
                        rtmpCamera1!!.glInterface.setFilter(2, NoFilterRender())


                    } else {
                        rtmpCamera1!!.setCustomAudioEffect(audioEffect)
                        volumeRealTime.progress = audioEffect.volume
                        volumeRealTime.max = volume.progress
                        //Log.d("VU", audioEffect.volume.toString())
                    }
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
        rtmpCameraPreview.setOnClickListener {
            if (isPaused) {
                pickImage()
            }
        }
    }

    private fun addVideoToList(name: String, time: Int, i: Int) {

        var frame = ConstraintLayout(this)

        frame.layoutParams = LinearLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        )
        frame.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT
        frame.layoutParams.height = 150
        frame.setOnClickListener(this)

        var timeText = TextView(this)
        timeText.setTextColor(Color.GRAY)
        if (time < 0) {
            timeText.text = "NaN"
        } else {
            timeText.text = time.toString() + "s"
        }
        timeText.layoutParams = Constraints.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        (timeText.layoutParams as Constraints.LayoutParams).topToTop =
            Constraints.LayoutParams.PARENT_ID
        (timeText.layoutParams as Constraints.LayoutParams).rightToRight =
            Constraints.LayoutParams.PARENT_ID
        (timeText.layoutParams as Constraints.LayoutParams).topMargin = 16
        (timeText.layoutParams as Constraints.LayoutParams).rightMargin = 16

        frame.addView(timeText)

        var count = TextView(this)
        count.setTextColor(ContextCompat.getColor(this, R.color.purple))
        count.text = i.toString().padStart(3, '0')
        count.textSize = 12f
        count.layoutParams = Constraints.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        (count.layoutParams as Constraints.LayoutParams).topToTop =
            Constraints.LayoutParams.PARENT_ID
        (count.layoutParams as Constraints.LayoutParams).leftToLeft =
            Constraints.LayoutParams.PARENT_ID
        (count.layoutParams as Constraints.LayoutParams).topMargin = 16
        (count.layoutParams as Constraints.LayoutParams).leftMargin = 16
        frame.addView(count)


        var filename = TextView(this)
        filename.setTextColor(Color.WHITE)
        filename.text = name
        filename.layoutParams = Constraints.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        (filename.layoutParams as Constraints.LayoutParams).topToTop =
            Constraints.LayoutParams.PARENT_ID
        (filename.layoutParams as Constraints.LayoutParams).leftToLeft =
            Constraints.LayoutParams.PARENT_ID
        (filename.layoutParams as Constraints.LayoutParams).topMargin = 72
        (filename.layoutParams as Constraints.LayoutParams).leftMargin = 16
        filename.textSize = 18f

        frame.addView(filename)
        frame.tag = name
        video_files.addView(frame)

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

    private fun listSiTuneFiles(): List<File> {
        return (data.getSiTunePath()).walkTopDown().toList()
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        if (!rtmpCamera1!!.isStreaming) {
            if (rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()) {
                rtmpCamera1!!.stopPreview()
                rtmpCamera1!!.startPreview(data.resolution[0], data.resolution[1])
                setCameraScale(data.resolution[0].toDouble() / data.resolution[1])
            }
        }

        if (data.isInCutPage) {
            if (rtmpCamera1!!.prepareAudio() && rtmpCamera1!!.prepareVideo()) {
                rtmpCamera1!!.startStream(data.pushurl)

                left_button.background =
                    ContextCompat.getDrawable(this, R.drawable.pause_streaming)
                left_button.text = "暫停直播"
                left_button.setTextColor(ContextCompat.getColor(this, R.color.white))
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
            //Toast.makeText(this, "串流中", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            speed.text = "0 kbps"
            speed.setTextColor(Color.WHITE)
            if (rtmpCamera1!!.reTry(5000, reason)) {
                //Toast.makeText(this, "連線丟失，重新連線", Toast.LENGTH_SHORT).show()
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
            volumeRealTime.progress = 0
            //Toast.makeText(this, "已中斷連線", Toast.LENGTH_SHORT).show()

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
        this.releaseInstance()
        finishAffinity()
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

    private fun _onTouchEvent(event: MotionEvent?): Boolean {
        //Log.d("Test", "obTouch")

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
                    //Log.d("Test", "setZoom")
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

    override fun onVideoDecoderFinished() {
        runOnUiThread {
            if (rtmpFile!!.isStreaming) {
//                Toast.makeText(
//                    this,
//                    "Video stream finished",
//                    Toast.LENGTH_SHORT
//                ).show()
                rtmpFile!!.stopStream()


                rtmpFilePreview.visibility = View.INVISIBLE
                returnToLive.visibility = View.INVISIBLE
                rtmpCameraPreview.visibility = View.VISIBLE

                rtmpCameraPreview.layoutParams = bigPreviewLayoutParam
                PGM.visibility = View.INVISIBLE
                rtmpCamera1!!.startPreview()
            }
        }
    }

    override fun onAudioDecoderFinished() {

    }

    override fun onTouch(v: View?, me: MotionEvent?): Boolean {
        return _onTouchEvent(me)

    }

    companion object {
        fun getInstance(): Context? {
            return instance
        }
    }

}
package net.zentring.live

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

class data {
    companion object {
        var currentCutVideoName = ""
        var currentCutVideoStart: Long = 0
        var currentCutVideoEnd: Long = 0

        var isCutFromBuffer = false
        var isInCutPage = false

        var instance: Context? = null
        var isStarted = false
        var isSelectEnabled = false
        var selectedVideoList = MutableList(0) { "" }
        fun getTempPath() =
            File((instance)?.getExternalFilesDir(null)!!.absolutePath)

        fun getStoragePath(): File = run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.getStorageDirectory()
            } else {
                Environment.getExternalStorageDirectory()
            }
        }

        fun getSiTunePath(): File {
            return File(getStoragePath(), "/situne/live")
        }

        fun getRecordPath(): File {
            return File(getTempPath(), "viturl_buffer")
        }

        fun getBufferVideoPath(): File {
            return File(getTempPath(), "/buffer.mp4")
        }

        var isDebug = false

        var resolutions = MutableList(0) { arrayOf(0, 0) }
        var resolution = arrayOf(640, 480)


        var pushurl //推流地址
                : String? = null
        var match //比赛ID
                : String? = null
        var round //比赛当前轮次
                : String? = null
        var graphic //是否开启字幕功能（0:否 1：是）
                : String? = null
        var player //当前登录账号的组别球员
                : List<Player>? = null
        var minrate //推流最小码率
                : String? = null
        var targetrate //推流最大码率
                : String? = null
        var initrate //推流初始化码率
                : String? = null
    }
}
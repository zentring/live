package net.zentring.live

import android.app.DownloadManager
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

        fun getBufferVideoPath(): File {
            return File(getTempPath(), "/buffer.mp4")
        }

        fun getBufferVideoPartPath(): File {
            return File(getTempPath(), "/buffer/")
        }

        fun getMergedVideoPath(): File {
            return File(getTempPath(), "/merged.mp4")
        }

        fun getMergeTempPath(): File {
            return File(getTempPath(), "/merged_tmp.mp4")
        }

        var isDebug = false

        var resolutions = MutableList(0) { arrayOf(0, 0) }
//        var resolution = arrayOf(1280, 720)
        var resolution = arrayOf(1280, 720)


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
        var gp_id //当前组ID
                : String? = null

        const val API_BASE_URL = "http://www.wifigolf.com/interface/graphic_test/"

        fun getClubTypeString(clubType: Int): String {
            return when (clubType) {
                1 -> "1号木"
                2 -> "3号铁"
                3 -> "7号铁"
                4 -> "切杆"
                5 -> "推杆"
                else -> ""
            }
        }

        fun getPlaceTypeString(placeType: Int): String {
            return when (placeType) {
                1 -> PlaceType.fairway.getPlaceString()
                2 -> PlaceType.rough.getPlaceString()
                3 -> PlaceType.fairway_bunker.getPlaceString()
                4 -> PlaceType.green_bunker.getPlaceString()
                5 -> PlaceType.green.getPlaceString()
                6 -> PlaceType.in_hole.getPlaceString()
                7 -> PlaceType.out_of_bounds.getPlaceString()
                8 -> PlaceType.lost.getPlaceString()
                9 -> PlaceType.immovable_obstacles.getPlaceString()
                10 -> PlaceType.water.getPlaceString()
                11 -> PlaceType.long_grass.getPlaceString()
                else -> ""
            }
        }

        var loginUser: String = ""
        var downloadManager: DownloadManager? = null
    }
}
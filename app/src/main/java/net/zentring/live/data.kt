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

        enum class PLACE_TYPE(iNumber: Int) {
            fairway(1),
            rough(2),
            fairway_bunker(3),
            green_bunker(4),
            green(5),
            in_hole(6),
            out_of_bounds(7),
            lost(8),
            immovable_obstacles(9),
            water(10),
            long_grass(11);

            var iNumber = 0
            fun toNumber(): Int {
                return iNumber
            }

            init {
                this.iNumber = iNumber
            }

            open fun getPlaceString(type: Int): String? {
                return if (fairway.toNumber() == type) {
                    "球道"
                } else if (rough.toNumber() == type) {
                    "长草"
                } else if (fairway_bunker.toNumber() == type) {
                    "球道沙坑"
                } else if (green_bunker.toNumber() == type) {
                    "果岭沙坑"
                } else if (green.toNumber() == type) {
                    "果岭"
                } else if (in_hole.toNumber() == type) {
                    "进洞"
                } else if (out_of_bounds.toNumber() == type) {
                    "界外"
                } else if (lost.toNumber() == type) {
                    "遗失球"
                } else if (immovable_obstacles.toNumber() == type) {
                    "不可移动障碍物"
                } else if (water.toNumber() == type) {
                    "水障碍"
                } else if (long_grass.toNumber() == type) {
                    "深长草"
                } else {
                    ""
                }
            }
        }
    }
}
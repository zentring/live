package net.zentring.live

import android.os.Environment
import android.text.TextUtils
import java.io.File

class Utils {
    companion object StaticParams {
        fun deleteUselessZero(str: String): String? {
            var str = str
            if (TextUtils.isEmpty(str)) {
                return "0"
            }
            if (str.indexOf(".") > 0) {
                str = str.replace("0+?$".toRegex(), "") //去掉多余的0
                str = str.replace("[.]$".toRegex(), "") //如最后一位是.则去掉
            }
            return str
        }

        fun deleteUselessZero(data: Double): String? {
            if (data == 0.0) {
                return "0"
            }
            var str = data.toString()
            if (str.indexOf(".") > 0) {
                str = str.replace("0+?$".toRegex(), "") //去掉多余的0
                str = str.replace("[.]$".toRegex(), "") //如最后一位是.则去掉
            }
            return str
        }

        val SD_DIR =
            Environment.getExternalStorageDirectory().path
        val RESOURCE_DIR = "/situne/live"

        @JvmStatic
        fun getHitGraphicPath(): String {
            return "$SD_DIR$RESOURCE_DIR/graphic/hit/"
        }

        @JvmStatic
        fun getRankGraphicPath(): String {
            return "$SD_DIR$RESOURCE_DIR/graphic/rank/"
        }
    }
}
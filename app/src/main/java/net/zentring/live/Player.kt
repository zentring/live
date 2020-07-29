package net.zentring.live

import com.volley.library.flowtag.OptionCheck
import java.io.Serializable

class Player : OptionCheck, Serializable {
    var name //球员名字
            : String? = null
    var id //球员ID
            : String? = null

    var isCheck = false

    override fun isChecked(): Boolean {
        return isCheck
    }

    override fun setChecked(checked: Boolean) {
        isCheck = checked
    }
}
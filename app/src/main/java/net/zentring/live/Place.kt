package net.zentring.live

import com.volley.library.flowtag.OptionCheck
import java.io.Serializable

class Place: OptionCheck, Serializable {
    var name
            : String? = null
    var id
            : Int? = null

    var isCheck = false

    override fun isChecked(): Boolean {
        return isCheck
    }

    override fun setChecked(checked: Boolean) {
        isCheck = checked
    }

}
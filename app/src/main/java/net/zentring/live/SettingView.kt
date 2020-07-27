package net.zentring.live

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_live.view.*
import kotlinx.android.synthetic.main.activity_setting.view.*

class SettingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.activity_setting, this)

        resolutions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                data.resolution = data.resolutions[position]
                (LiveActivity.getInstance() as LiveActivity).setCameraScale(data.resolution[0].toDouble() / data.resolution[1])
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // your code here
            }
        }

        back.setOnClickListener {
            visibility = View.INVISIBLE
        }
    }

    fun setSettingResolutions() {
        val arrayOfResolution = MutableList(0) { "" }
        data.resolutions.forEach {
            arrayOfResolution.add("" + it[0] + "x" + it[1])
        }
        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            context, R.layout.spinner_preview,
            arrayOfResolution
        ) //selected item will look like a spinner set from XML

        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_view)
        resolutions.adapter = spinnerArrayAdapter
        if (arrayOfResolution.indexOf("1920x1080") != -1) {
            resolutions.setSelection(arrayOfResolution.indexOf("1920x1080"))
        }
    }
}
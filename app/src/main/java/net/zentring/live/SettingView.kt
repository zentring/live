package net.zentring.live

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.android.synthetic.main.activity_setting.view.*
import java.io.File

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
            (LiveActivity.getInstance() as LiveActivity).settingBtn.visibility = View.VISIBLE
        }

        pause_image_preview.setOnClickListener {
            (LiveActivity.getInstance() as LiveActivity).pickImage()
        }
        val imgFile = File(data.getTempPath(), "pause.image")

        if (imgFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            pause_image_preview.setImageBitmap(myBitmap)
        } else {
            pause_image_preview.setImageResource(R.drawable.pause)
        }
    }

    fun previewPauseImage() {
        var f = File(data.getTempPath(), "pause.image")
        var bmp = BitmapFactory.decodeFile(f.absolutePath)
        pause_image_preview.setImageBitmap(bmp)
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
package net.zentring.live

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


const val CAMERA_REQUEST_CODE = 100
const val AUDIO_REQUEST_CODE = 200

class MainActivity : AppCompatActivity() {

    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (!hasPermissions(this, this.permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 0)
            } else {
                goLoginActivity()
            }
        }
    }

    private fun goLoginActivity() {
        val myIntent = Intent(this, LoginActivity::class.java)
        //myIntent.putExtra("key", value) //Optional parameters
        startActivityForResult(myIntent, 0)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 0) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 0)
            return
        } else {
            goLoginActivity()
        }
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        permissions.forEach {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true

    }
}
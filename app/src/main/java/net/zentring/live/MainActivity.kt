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

    private val permissionsCarema = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    private val permissionsFile = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (!hasPermissions(this, this.permissionsCarema)) {
                ActivityCompat.requestPermissions(this, permissions, 0)
            } else {
                if (!hasPermissions(this, this.permissionsFile)) {
                    recreate()
                    //ActivityCompat.requestPermissions(this, permissions, 1)
                } else {
                    goLoginActivity()
                }
            }

        } else if (requestCode == 1) {
            if (!hasPermissions(this, this.permissionsFile)) {
                ActivityCompat.requestPermissions(this, permissions, 1)
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

        if (!hasPermissions(this, permissionsCarema)) {
            ActivityCompat.requestPermissions(this, permissionsCarema, 0)
            return
        } else {
            if (!hasPermissions(this, permissionsFile)) {
                ActivityCompat.requestPermissions(this, permissionsFile, 1)
                return
            } else {
                goLoginActivity()
            }
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
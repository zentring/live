package net.zentring.live

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import kotlin.system.exitProcess


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (!data.isStarted) {
            data.isStarted = true
        } else {
            finish()
            exitProcess(0)
        }

        if (data.isDebug) {
            goLiveActivity()
            return
        }

        username.setText("15941131385")
        password.setText("123456")

        try {
            val pInfo: PackageInfo =
                packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            ver.text = "版本 : $version"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        var isShowPassword = false
        previewPassword.setOnClickListener {
            if (isShowPassword) {
                password.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                password.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            isShowPassword = !isShowPassword

        }

        login_btn.setOnClickListener {
            error.text = ""
            if (username.text.isEmpty()) {
                error.text = "請輸入帳號"
                return@setOnClickListener
            }
            if (password.text.isEmpty()) {
                error.text = "請輸入密碼"
                return@setOnClickListener
            }

            // Instantiate the RequestQueue.
            val queue = Volley.newRequestQueue(this)
            val url =
                "https://www.wifigolf.com/interface/graphic/login.php?user=${username.text}&password=${password.text}"

            // Request a string response from the provided URL.
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                Response.Listener<String> { response ->

                    // Display the first 500 characters of the response string.
                    //error.text = "Response is: ${response.substring(0, 500)}"
                    Log.d("Info", "Response is: $response")
                    if (response == "[]") {
                        error.text = "登入失敗，帳號或密碼錯誤"
                    } else {
                        var jObject = JSONObject(response)
                        println(jObject.toString())

                        data.pushurl = jObject.getString("pushurl")
                        data.match = jObject.getString("match")
                        data.round = jObject.getString("round")
                        data.graphic = jObject.getString("graphic")
                        data.gp_id = jObject.getString("gp_id")

                        var players = jObject.getJSONArray("player")
                        var player = MutableList(0) { Player() }
                        for (p in 0 until players.length()) {
                            var pInstance = Player()
                            pInstance.name = players.getJSONObject(p).getString("name")
                            pInstance.id = players.getJSONObject(p).getString("id")
                            player.add(pInstance)
                        }
                        data.player = player

                        data.minrate = jObject.getString("minrate")
                        data.targetrate = jObject.getString("targetrate")
                        data.initrate = jObject.getString("initrate")
                        data.loginUser = username.text.toString()
                        frame.visibility = View.INVISIBLE
                        goLiveActivity()
                    }

                },
                Response.ErrorListener {
                    error.text = "發生錯誤"
                    it.printStackTrace()
                })


            // Add the request to the RequestQueue.
            queue.add(stringRequest)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        finishAffinity()
        exitProcess(0)
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Thread.sleep(100)
                finish()
                exitProcess(0)
            }
        }
    }

    private fun goLiveActivity() {
        val myIntent = Intent(this, LiveActivity::class.java)
        startActivityForResult(myIntent, 1)
    }

}
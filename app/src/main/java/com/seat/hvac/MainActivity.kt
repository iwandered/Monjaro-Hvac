package com.seat.hvac

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager

class MainActivity : FragmentActivity() {

    private var preferences: SharedPreferences? = null
    private var climateThreadManager: ClimateThreadManager? = null

    companion object {
        var instance: MainActivity? = null
        var prefs: SharedPreferences? = null

        fun debugToast(context: Context, s: String?) {
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        }
    }

    fun checkStoragePermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    private val STORAGE_PERMISSION_CODE = 23
    private fun requestForStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.setData(uri)
                startActivity(intent)
            } catch (e: java.lang.Exception) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        val intent = Intent()
        val packageName = packageName
        val pm: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        if (!checkStoragePermissions()) {
            requestForStoragePermissions()
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        instance = this
        prefs = preferences

        val controlFragment = ControlFragment()

        climateThreadManager = ClimateThreadManager(applicationContext, controlFragment.uiHandler)

        val climateThread = climateThreadManager!!.getClimateThread()

        if (climateThread != null) {
            val seatManager = SeatManager(climateThread)
            val climateManager = ClimateManager(climateThread)

            // 重要：只设置Manager，不调用任何恢复或初始化方法
            controlFragment.setManagers(seatManager, climateManager)
            controlFragment.setClimateThreadManager(climateThreadManager!!)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, controlFragment)
                .commit()

            Log.d("MainActivity", "应用初始化完成，等待ClimateThread发送初始状态")

        } else {
            Toast.makeText(this, "气候线程初始化失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "应用恢复运行")
    }

    override fun onDestroy() {
        super.onDestroy()
        climateThreadManager?.destroy()
        Log.d("MainActivity", "应用销毁，清理资源")
    }
}
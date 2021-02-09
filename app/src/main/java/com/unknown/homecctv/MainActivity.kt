package com.unknown.homecctv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "myLog.MainA"
    }

    private val mActivity by lazy { this }
    private val pref by lazy { getSharedPreferences(C.PREF_DATA, Context.MODE_PRIVATE) }

    private var isCCTVMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bt_user__main.setOnClickListener {
            isCCTVMode = false
            checkPermissions()
        }

        bt_cctv__main.setOnClickListener {
            isCCTVMode = true
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        val isPermissionGranted =
            (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)

        if (!isPermissionGranted) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,//pref
                    Manifest.permission.RECORD_AUDIO,//webRTC
                    Manifest.permission.CAMERA//webRTC
                ),
                C.PERMISSION_REQUEST_CODE
            )

        } else {

            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)


            if (isCCTVMode) startCCTVActivity()
            else startUserActivity()
        }

    }

    private fun startUserActivity() {
        pref.edit().putBoolean(C.PREF_IS_CCTV, false).apply()
        startActivity(Intent(this, HomeActivity::class.java))

    }

    private fun startCCTVActivity() {
        pref.edit().putBoolean(C.PREF_IS_CCTV, true).apply()
        startActivity(Intent(this, CCTVActivity::class.java))

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            C.PERMISSION_REQUEST_CODE -> {
                val isFirstPermissionRequest = pref.getBoolean(C.PREF_IS_FIRST_PERMISSION, true)

                if (isFirstPermissionRequest) {
                    pref.edit().putBoolean(C.PREF_IS_FIRST_PERMISSION, false).apply()

                    checkPermissions()

                } else {
                    if ((ContextCompat.checkSelfPermission(
                            this, Manifest.permission.RECORD_AUDIO
                        )
                                == PackageManager.PERMISSION_DENIED)
                        && !ActivityCompat.shouldShowRequestPermissionRationale(
                            mActivity,
                            Manifest.permission.RECORD_AUDIO
                        )
                        || (ContextCompat.checkSelfPermission(
                            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                                == PackageManager.PERMISSION_DENIED)
                        && !ActivityCompat.shouldShowRequestPermissionRationale(
                            mActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        || (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        )
                                == PackageManager.PERMISSION_DENIED)
                        && !ActivityCompat.shouldShowRequestPermissionRationale(
                            mActivity,
                            Manifest.permission.CAMERA
                        )

                    ) {
                        showAppDetailSettings()

                    }
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            C.APP_SETTINGS_REQUEST_CODE -> {
                checkPermissions()
            }
        }
    }

    private fun showAppDetailSettings() {
        androidx.appcompat.app.AlertDialog.Builder(mActivity)
            .setMessage(getString(R.string.need_permission))
            .setPositiveButton(mActivity.getString(R.string.ok)) { _, _ ->
                val mIntent = Intent()
                mIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                mIntent.data = uri
                startActivityForResult(mIntent, C.APP_SETTINGS_REQUEST_CODE)
            }
            .setNegativeButton(mActivity.getString(R.string.cancel)) { _, _ ->
                checkPermissions()
            }
            .create()
            .show()

    }

}

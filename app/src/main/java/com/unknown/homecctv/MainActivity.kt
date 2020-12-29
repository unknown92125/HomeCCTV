package com.unknown.homecctv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
    private val mDatabaseCCTV by lazy { FirebaseDatabase.getInstance().getReference("cctv") }

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
            if (isCCTVMode) startCCTVActivity()
            else startUserActivity()
        }

    }

    private fun startUserActivity() {
        pref.edit().putBoolean(C.PREF_IS_CCTV, false).apply()
        startActivity(Intent(this, CCTVModeActivity::class.java))

    }

    private fun startCCTVActivity() {
        pref.edit().putBoolean(C.PREF_IS_CCTV, true).apply()
        val cctvId = pref.getString(C.PREF_CCTV_ID, "").toString()
        if (cctvId.isEmpty()){
            getRandomIdAndPw()

        } else {
            C.cctvId = cctvId
            C.cctvPw = pref.getString(C.PREF_CCTV_PW, "").toString()

            getFcmToken()

        }

    }

    private fun getRandomIdAndPw() {
        Log.e(TAG, "getRandomIdAndPw")
        val allowedNumbers = "0123456789"
        val sizeOfRandomId = 8
        val sizeOfRandomPw = 4

        val random = Random()

        val randomId = StringBuilder(sizeOfRandomId)
        for (i in 0 until sizeOfRandomId){
            randomId.append(allowedNumbers[random.nextInt(allowedNumbers.length)])
        }
        Log.e(TAG, "randomId:$randomId")
        val cctvId = randomId.toString()

        val randomPw = StringBuilder(sizeOfRandomPw)
        for (i in 0 until sizeOfRandomPw){
            randomPw.append(allowedNumbers[random.nextInt(allowedNumbers.length)])
        }
        Log.e(TAG, "randomPw:$randomPw")
        val cctvPw = randomPw.toString()

        var isDuplicate = false

        mDatabaseCCTV.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (s in snapshot.children) {
                    Log.e(TAG, "key:${s.key}..value:${s.value}")
                    val key = s.key.toString()
                    if (key == cctvId) {
                        isDuplicate = true
                        break
                    }
                }

                if (isDuplicate) {
                    isDuplicate = false
                    getRandomIdAndPw()

                } else {
                    pref.edit().putString(C.PREF_CCTV_ID, cctvId).apply()
                    pref.edit().putString(C.PREF_CCTV_PW, cctvPw).apply()
                    C.cctvId = cctvId
                    C.cctvPw = cctvPw

                    getFcmToken()
                }
            }
        })

    }

    private fun getFcmToken(){
        Log.e(TAG, "getFcmToken")
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result as String
            Log.e(TAG, token)

            uploadToken(token)

        })
    }

    private fun uploadToken(token: String) {
        val mDatabaseToken = mDatabaseCCTV.child(C.cctvId).child("token")
        mDatabaseToken.setValue(token)
        val mDatabaseTime = mDatabaseCCTV.child(C.cctvId).child("time")
        mDatabaseTime.setValue(Calendar.getInstance().timeInMillis)

        startActivity(Intent(this, CCTVModeActivity::class.java))

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

package com.unknown.homecctv.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.unknown.homecctv.C
import java.util.*

class CCTVViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val TAG = "myLog.CCTVVM"
        var cctvId = ""
        var cctvPw = ""
    }

    private val mContext: Context by lazy { getApplication<Application>().applicationContext }
    private val pref by lazy { mContext.getSharedPreferences(C.PREF_DATA, Context.MODE_PRIVATE) }
    private val mDatabaseCCTV by lazy { FirebaseDatabase.getInstance().getReference("cctv") }

    val cctvIdLive = MutableLiveData<String>()
    val cctvPwLive = MutableLiveData<String>()

    init {
        loadCCTVIdAndPw()
    }

    private fun loadCCTVIdAndPw() {
        val id = pref.getString(C.PREF_CCTV_ID, "") ?: ""
        val pw = pref.getString(C.PREF_CCTV_PW, "") ?: ""
        if (id.isEmpty()) {
            getRandomIdAndPw()
        } else {
            cctvId = id
            cctvPw = pw
            cctvIdLive.value = id
            cctvPwLive.value = pw
            getFcmToken()
        }
    }

    fun setCCTVPw(pw: String) {
        pref.edit().putString(C.PREF_CCTV_PW, pw).apply()
        cctvPwLive.value = pw
        cctvPw = pw
    }

    private fun getRandomIdAndPw() {
        Log.e(TAG, "getRandomIdAndPw")
        val allowedNumbers = "0123456789"
        val sizeOfRandomId = 8
        val sizeOfRandomPw = 4

        val random = Random()

        val randomId = StringBuilder(sizeOfRandomId)
        for (i in 0 until sizeOfRandomId) {
            randomId.append(allowedNumbers[random.nextInt(allowedNumbers.length)])
        }
        Log.e(TAG, "randomId:$randomId")
        val id = randomId.toString()

        val randomPw = StringBuilder(sizeOfRandomPw)
        for (i in 0 until sizeOfRandomPw) {
            randomPw.append(allowedNumbers[random.nextInt(allowedNumbers.length)])
        }
        Log.e(TAG, "randomPw:$randomPw")
        val pw = randomPw.toString()

        var isDuplicate = false

        mDatabaseCCTV.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (s in snapshot.children) {
                    Log.e(TAG, "key:${s.key}..value:${s.value}")
                    val key = s.key.toString()
                    if (key == id) {
                        isDuplicate = true
                        break
                    }
                }

                if (isDuplicate) {
                    isDuplicate = false
                    getRandomIdAndPw()

                } else {
                    pref.edit().putString(C.PREF_CCTV_ID, id).apply()
                    pref.edit().putString(C.PREF_CCTV_PW, pw).apply()
                    cctvIdLive.value = id
                    cctvPwLive.value = pw
                    cctvId = id
                    cctvPw = pw

                    getFcmToken()
                }
            }
        })

    }

    private fun getFcmToken() {
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
        cctvIdLive.value?.let {
            val mDatabaseToken = mDatabaseCCTV.child(it).child("token")
            mDatabaseToken.setValue(token)
            val mDatabaseTime = mDatabaseCCTV.child(it).child("time")
            mDatabaseTime.setValue(Calendar.getInstance().timeInMillis)
        }
    }
}
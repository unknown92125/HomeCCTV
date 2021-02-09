package com.unknown.homecctv

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FCMService : FirebaseMessagingService() {

    companion object {
        const val TAG = "myLog.FCMService"
    }

    private val mContext by lazy { applicationContext }
    private val pref by lazy { getSharedPreferences(C.PREF_DATA, Context.MODE_PRIVATE) }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e(TAG, "from:${remoteMessage.from} priority:${remoteMessage.priority}")
        if (remoteMessage.data.isNotEmpty()){
            Log.e(TAG, "message:${remoteMessage.data}")

            val message = remoteMessage.data["msg"] ?: ""
            val what: String
            var msg = ""

            if (message.contains(C.DIVIDER)) {
                val splitMsg = message.split(C.DIVIDER)
                what = splitMsg[0]
                msg = splitMsg[1]
            } else {
                what = message
            }

            if (pref.getBoolean(C.PREF_IS_CCTV, false)){
                when(what){
                    C.CONNECT_CCTV -> {
                        val isPWRight = msg == pref.getString(C.PREF_CCTV_PW, "")
                        if (isPWRight){
                            if (CCTVService.isRunning){
                                stopService(Intent(mContext, CCTVService::class.java))
                            }
                            startService(Intent(mContext, CCTVService::class.java))
                        }
                    }

                }
            }

        }
    }

    override fun onNewToken(newToken: String) {
        Log.e(TAG, "onNewToken:$newToken")
    }
}
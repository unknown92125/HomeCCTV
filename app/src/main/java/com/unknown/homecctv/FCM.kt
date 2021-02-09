package com.unknown.homecctv

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FCM {
    const val TAG = "FCM"

    fun sendToCCTV(message: String, cctvID: String) {
        getCCTVToken(message, cctvID)
    }

//    private fun getCCTVToken(message: String, cctvID: String) {
//        val mDatabase = FirebaseDatabase.getInstance().getReference("cctv")
//        mDatabase.child(cctvID).addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {
//                Log.e(TAG, "onCancelled:$error")
//            }
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//                Log.e(TAG, "value:${snapshot.value.toString()}")
//                val value = snapshot.value.toString()
//                val cctvToken = value.substring(7, value.length - 1)
//                Log.e(TAG, "CCTVToken:$cctvToken")
//
//                callAPI(message, cctvToken)
//
//            }
//
//        })
//    }

    private fun getCCTVToken(message: String, cctvID: String) {
        val mDatabase = FirebaseDatabase.getInstance().getReference("cctv")
        mDatabase.child(cctvID).child("token").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled:$error")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                Log.e(TAG, "value:${snapshot.value.toString()}")
                val cctvToken = snapshot.value.toString()
                Log.e(TAG, "CCTVToken:$cctvToken")

                callAPI(message, cctvToken)

            }

        })
    }

    private fun callAPI(message: String, cctvToken: String) {
        object : Thread() {
            override fun run() {
                val params = hashMapOf(
                    "message" to message,
                    "token" to cctvToken
                )

                val api = API()
                val result = api.callSync(C.SEND_FCM_URL, params)
                result?.let { response ->
                    Log.e(TAG, "response:$response")
                }
                return
            }
        }.start()

    }

}
package com.unknown.homecctv

import androidx.lifecycle.MutableLiveData

object C {
    const val DIVIDER = ";;::,,..;:,."

    const val CONNECTED = "CONNECTED"
    const val CONNECTING = "CONNECTING"
    const val DISCONNECTED = "DISCONNECTED"

    const val CONNECT_CCTV = "CONNECT_CCTV"
    const val DISCONNECT_CCTV = "DISCONNECT_CCTV"
    const val SWITCH_CAMERA = "SWITCH_CAMERA"

    const val SEND_FCM_URL = "fcm/sendMessage.php"
    const val BASE_URL = "http://unknown92125.dothome.co.kr/"

    const val PERMISSION_REQUEST_CODE = 9001
    const val APP_SETTINGS_REQUEST_CODE = 9002

    const val ACTION_FCM = "ACTION_FCM"

    const val PREF_DATA = "prefData"
    const val PREF_CCTV_ID = "prefCCTVId"
    const val PREF_CCTV_PW = "prefCCTVPw"
    const val PREF_IS_CCTV = "prefIsCCTV"
    const val PREF_IS_FIRST_PERMISSION = "prefIsFirstPermission"

    var cctvId = ""
    var userId = ""

    val liveCCTVPw = MutableLiveData<String>()

    var status = DISCONNECTED

}
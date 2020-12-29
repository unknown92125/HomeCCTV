package com.unknown.homecctv

object C {
    const val DIVIDER = ";;::,,..;:,."

    const val CONNECTED = "CONNECTED"
    const val CONNECTING = "CONNECTING"
    const val DISCONNECTED = "DISCONNECTED"

    const val START = "START"

    const val SEND_FCM_URL = "fcm/sendMessage.php"
    const val BASE_URL = "http://unknown92125.dothome.co.kr/"

    const val PERMISSION_REQUEST_CODE = 9001
    const val APP_SETTINGS_REQUEST_CODE = 9002

    const val PREF_DATA = "prefData"
    const val PREF_CCTV_ID = "prefCCTVId"
    const val PREF_CCTV_PW = "prefCCTVPw"
    const val PREF_IS_CCTV = "prefIsCCTV"
    const val PREF_IS_FIRST_PERMISSION = "prefIsFirstPermission"

    const val ROOM = "room"
    const val IS_CALLER = "isCaller"

    var cctvId = ""
    var cctvPw = ""
    var userID = ""

    var status = DISCONNECTED

}
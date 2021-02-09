package com.unknown.homecctv

object C {
    const val DIVIDER = ";;::,,..;:,."

    const val CONNECTED = "CONNECTED"
    const val CONNECTING = "CONNECTING"
    const val DISCONNECTED = "DISCONNECTED"

    const val CONNECT_CCTV = "CONNECT_CCTV"
    const val DISCONNECT_CCTV = "DISCONNECT_CCTV"
    const val SWITCH_CAMERA = "SWITCH_CAMERA"
    const val CHECK_PW = "CHECK_PW"
    const val RIGHT_PW = "RIGHT_PW"
    const val WRONG_PW = "WRONG_PW"

    const val TITLE_ADD = "TITLE_ADD"
    const val TITLE_EDIT = "TITLE_EDIT"
    const val TITLE_CONNECT = "TITLE_CONNECT"

    const val SEND_FCM_URL = "fcm/sendMessage.php"
    const val BASE_URL = "http://unknown92125.dothome.co.kr/"

    const val PERMISSION_REQUEST_CODE = 9001
    const val APP_SETTINGS_REQUEST_CODE = 9002

    const val ACTION_FCM = "ACTION_FCM"
    const val ACTION_CCTV = "ACTION_CCTV"
    const val ACTION_USER = "ACTION_USER"

    const val PREF_DATA = "prefData"
    const val PREF_CCTV_ID = "prefCCTVId"
    const val PREF_CCTV_PW = "prefCCTVPw"
    const val PREF_IS_CCTV = "prefIsCCTV"
    const val PREF_IS_FIRST_PERMISSION = "prefIsFirstPermission"
    const val PREF_CCTV_ITEMS = "prefCCTVItems"

    const val DB_NAME = "Data.db"

    var status = DISCONNECTED

}
package com.unknown.homecctv.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.unknown.homecctv.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val mContext: Context by lazy { getApplication<Application>().applicationContext }

    val arrCCTVLive: MutableLiveData<ArrayList<CCTVItem>> = MutableLiveData()


    init {
        loadCCTVItems()
    }

    private fun loadCCTVItems() {
        val arrCCTVItems = DB.getCCTVItems(mContext)
        arrCCTVLive.value = arrCCTVItems
    }

    fun getNewName() : String{
        var num = arrCCTVLive.value?.size ?: 0

        return "CCTV${++num}"
    }

    fun addCCTVItem(name: String, id: String) {
        val arrCCTVItems = DB.addCCTVItem(mContext, name, id)
        arrCCTVLive.value = arrCCTVItems
    }

    fun editCCTVItem(num: Int, name: String, id: String, pw: String, isSaveChecked: Int) {
        val arrCCTVItems = DB.editCCTVItem(mContext, num, name, id, pw, isSaveChecked)
        arrCCTVLive.value = arrCCTVItems
    }

    fun deleteCCTVItem(num: Int){
        val arrCCTVItems = DB.deleteCCTVItem(mContext, num)
        arrCCTVLive.value = arrCCTVItems
    }

    fun connectCCTV(id: String, pw: String){
        Log.e("TAG", "connectCCTV.. id:$id.. pw:$pw")

        val mIntent = Intent(mContext, VideoActivity::class.java)
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        mIntent.putExtra("id", id)
        mIntent.putExtra("pw", pw)
        mContext.startActivity(mIntent)
    }

}


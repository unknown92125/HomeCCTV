package com.unknown.homecctv.adapters

import android.util.Log
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.unknown.homecctv.CCTVItem

object DataBindingAdapters {

    @BindingAdapter("items")
    @JvmStatic
    fun setBindItem(view: RecyclerView, items: MutableLiveData<ArrayList<CCTVItem>>) {
        view.adapter?.let { adapter ->
            if (adapter is CCTVAdapter) {
                items.value?.let {
                    adapter.arrCCTV = it
                }
                adapter.notifyDataSetChanged()
                Log.e("myLog.BindingA", "setBindItem..notifyDataSetChanged")
            }
        }
    }
}
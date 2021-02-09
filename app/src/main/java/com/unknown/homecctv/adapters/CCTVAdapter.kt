package com.unknown.homecctv.adapters

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.unknown.homecctv.C
import com.unknown.homecctv.CCTVItem
import com.unknown.homecctv.FCM
import com.unknown.homecctv.R
import com.unknown.homecctv.viewmodels.HomeViewModel
import kotlinx.android.synthetic.main.dialog_cctv_info.view.*
import kotlinx.android.synthetic.main.rv_cctv.view.*

class CCTVAdapter(val viewModelHome: HomeViewModel) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TAG = "myLog.CCTVAdapter"
    }

    lateinit var mContext: Context

    var arrCCTV: ArrayList<CCTVItem> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mContext = parent.context
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.rv_cctv, parent, false)
        return VH(itemView)
    }

    override fun getItemCount(): Int {
        return arrCCTV.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewHolder = holder as VH
        Log.e(TAG, "onBindViewHolder")
        val item = arrCCTV[position]
        viewHolder.itemView.tv_name__rv_cctv.text = item.name
        viewHolder.itemView.tv_id__rv_cctv.text = item.id
        viewHolder.itemView.bt_connect__rv_cctv.setOnClickListener { showConnectDialog(item) }
        viewHolder.itemView.bt_edit__rv_cctv.setOnClickListener { showEditDialog(item) }
        viewHolder.itemView.bt_delete__rv_cctv.setOnClickListener { deleteCCTV(item.num) }
    }

    inner class VH constructor(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun deleteCCTV(num: Int) {
        AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.press_ok_to_delete))
            .setPositiveButton(
                mContext.getString(R.string.ok)
            ) { _, _ ->
                viewModelHome.deleteCCTVItem(num)
            }
            .setNegativeButton(
                mContext.getString(R.string.cancel)
            ) { _, _ -> }
            .show()
    }

    private fun showEditDialog(item: CCTVItem) {
        val view = LayoutInflater.from(mContext).inflate(R.layout.dialog_cctv_info, null)
        view.tv_title__dialog_cctv_info.text = "CCTV 수정"
        view.et_name__dialog_cctv_info.setText(item.name)
        view.et_id__dialog_cctv_info.setText(item.id)
        view.et_pw__dialog_cctv_info.setText(item.pw)
        view.cb_pw__dialog_cctv_info.isChecked = item.isSaveChecked == 1

        val dialog = AlertDialog.Builder(mContext)
            .setView(view)
            .show()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.bt_ok__dialog_cctv_info.setOnClickListener {
            val name = view.et_name__dialog_cctv_info.text.toString()
            val id = view.et_id__dialog_cctv_info.text.toString()

            val pw: String
            val isChecked: Int
            if (view.cb_pw__dialog_cctv_info.isChecked){
                isChecked = 1
                pw = view.et_pw__dialog_cctv_info.text.toString()

            } else {
                isChecked = 0
                pw = ""
            }

            when {
                name.isEmpty() -> Toast.makeText(mContext, "별명을 입력해 주세요", Toast.LENGTH_SHORT).show()
                id.isEmpty() -> Toast.makeText(mContext, "아이디를 입력해 주세요", Toast.LENGTH_SHORT).show()
                else -> {
                    viewModelHome.editCCTVItem(item.num, name, id, pw, isChecked)
                    dialog.dismiss()
                }
            }
        }

        view.bt_cancel__dialog_cctv_info.setOnClickListener { dialog.dismiss() }

    }

    private fun showConnectDialog(item: CCTVItem){
        val view = LayoutInflater.from(mContext).inflate(R.layout.dialog_cctv_info, null)
        view.tv_title__dialog_cctv_info.text = item.name
        view.ll_id__dialog_cctv_info.visibility = View.GONE

        view.et_pw__dialog_cctv_info.setText(item.pw)
        view.cb_pw__dialog_cctv_info.isChecked = item.isSaveChecked == 1

        val dialog = AlertDialog.Builder(mContext)
            .setView(view)
            .show()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.bt_ok__dialog_cctv_info.setOnClickListener {
            val pw = view.et_pw__dialog_cctv_info.text.toString()
            if (pw.isEmpty()){
                Toast.makeText(mContext, "비밀번호를 입력해 주세요", Toast.LENGTH_SHORT).show()

            } else {
                if (view.cb_pw__dialog_cctv_info.isChecked){
                    viewModelHome.editCCTVItem(item.num, item.name, item.id, pw, 1)
                } else {
                    viewModelHome.editCCTVItem(item.num, item.name, item.id, "", 0)
                }

                dialog.dismiss()
                viewModelHome.connectCCTV(item.id, pw)
            }
        }

        view.bt_cancel__dialog_cctv_info.setOnClickListener { dialog.dismiss() }
    }
}

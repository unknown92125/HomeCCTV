package com.unknown.homecctv

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.unknown.homecctv.adapters.CCTVAdapter
import com.unknown.homecctv.databinding.ActivityHomeBinding
import com.unknown.homecctv.viewmodels.HomeViewModel
import kotlinx.android.synthetic.main.dialog_cctv_info.*
import kotlinx.android.synthetic.main.dialog_cctv_info.view.*

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TAG = "myLog.HomeActivity"
    }

    private val mActivity by lazy { this }
    private val mContext by lazy { applicationContext }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModelHome: HomeViewModel

    var num = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_home)

        viewModelHome = HomeViewModel(application)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        binding.apply {
            lifecycleOwner = mActivity
            rvHome.adapter = CCTVAdapter(viewModelHome)
            hvm = viewModelHome


            btAddCctvHome.setOnClickListener {
                showAddDialog()
            }

        }
    }

    private fun showAddDialog(){
        val view = layoutInflater.inflate(R.layout.dialog_cctv_info, null)
        view.tv_title__dialog_cctv_info.text = "CCTV 추가"
        view.et_name__dialog_cctv_info.setText(viewModelHome.getNewName())
        view.ll_pw__dialog_cctv_info.visibility = View.GONE

        val dialog = AlertDialog.Builder(mActivity)
            .setView(view)
            .show()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.bt_ok__dialog_cctv_info.setOnClickListener {
            val name = view.et_name__dialog_cctv_info.text.toString()
            val id = view.et_id__dialog_cctv_info.text.toString()
            when {
                name.isEmpty() -> Toast.makeText(mActivity, "별명을 입력해 주세요", Toast.LENGTH_SHORT).show()
                id.isEmpty() -> Toast.makeText(mActivity, "아이디를 입력해 주세요", Toast.LENGTH_SHORT).show()
                else -> {
                    viewModelHome.addCCTVItem(name, id)
                    dialog.dismiss()
                }
            }
        }

        view.bt_cancel__dialog_cctv_info.setOnClickListener { dialog.dismiss() }

    }

}



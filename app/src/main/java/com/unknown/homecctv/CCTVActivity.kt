package com.unknown.homecctv

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.unknown.homecctv.databinding.ActivityCctvBinding
import com.unknown.homecctv.viewmodels.CCTVViewModel
import kotlinx.android.synthetic.main.activity_cctv.*

class CCTVActivity : AppCompatActivity() {

    companion object {
        const val TAG = "myLog.CCTVActivity"
    }

    private val mContext by lazy { applicationContext }
    private val mActivity by lazy { this }
    private val pref by lazy { getSharedPreferences(C.PREF_DATA, Context.MODE_PRIVATE) }
    private val cctvViewModel by lazy { CCTVViewModel(application) }

    private lateinit var binding: ActivityCctvBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_cctv)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_cctv)
        binding.apply {
            lifecycleOwner = mActivity
            cvm = cctvViewModel

            btOkCctv.setOnClickListener {
                changePassword()
            }
        }

        bt_finish__cctv.setOnClickListener { finish() }

    }

    private fun changePassword() {
        val newPw = et__cctv.text.toString()
        if (newPw.isEmpty()) {
            Toast.makeText(mActivity, getString(R.string.input_new_pw), Toast.LENGTH_SHORT).show()
        } else {
            pref.edit().putString(C.PREF_CCTV_PW, newPw).apply()
            cctvViewModel.setCCTVPw(newPw)
            et__cctv.setText("")

            Toast.makeText(mActivity, getString(R.string.pw_changed), Toast.LENGTH_SHORT).show()

        }
    }
}

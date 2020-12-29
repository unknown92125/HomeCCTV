package com.unknown.homecctv

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

class API {
    interface APIService {
        @FormUrlEncoded
        @Streaming
        @POST
        fun callAPI(
            @Url url: String,
            @FieldMap params: HashMap<String, String>
        ): Call<String>
    }

    fun call(
        url: String,
        callback: Callback<String>,
        params: HashMap<String, String> = hashMapOf()
    ) {
        Log.e("API", "url:$url params:$params")
        val retrofit = Retrofit.Builder()
            .baseUrl(C.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(APIService::class.java)
            .callAPI(url, params)
            .enqueue(callback)
    }

    fun callSync(
        url: String,
        params: HashMap<String, String> = hashMapOf()
    ): String? {
        Log.e("API", "url:$url params:$params")
        val retrofit = Retrofit.Builder()
            .baseUrl(C.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return try {
            retrofit.create(APIService::class.java).callAPI(url, params).execute().body()

        } catch (e: Exception) {
            e.printStackTrace()
            "exception"
        }

    }
}
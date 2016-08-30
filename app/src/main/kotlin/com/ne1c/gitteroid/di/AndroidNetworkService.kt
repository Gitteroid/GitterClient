package com.ne1c.gitteroid.di

import android.content.Context
import android.net.ConnectivityManager
import com.ne1c.gitteroid.di.base.NetworkService

class AndroidNetworkService(private val context: Context) : NetworkService {
    override fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null
    }
}
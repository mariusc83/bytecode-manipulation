package com.mc.codetransformer.utils

import android.util.Log

public class MyLogger {

    companion object {
        @JvmStatic
        fun log(tag: String, message: String) {
            Log.d(tag, message)
        }
    }
}
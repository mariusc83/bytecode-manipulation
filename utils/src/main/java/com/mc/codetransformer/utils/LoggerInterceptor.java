package com.mc.codetransformer.utils;

import android.util.Log;

public class LoggerInterceptor {

    public static void log(String tag, String message) {
        Log.d(tag, message);
    }
}

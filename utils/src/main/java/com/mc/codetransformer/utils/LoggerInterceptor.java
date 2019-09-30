package com.mc.codetransformer.utils;

import android.util.Log;

public class LoggerInterceptor {

    public static int log(String tag, String message) {
        Log.d(tag, message);
        return Integer.MAX_VALUE;
    }
}

package de.robv.android.xposed;

import android.util.Log;

/**
 * XposedBridge logging and hook registration methods.
 */
public final class XposedBridge {
    private static final String TAG = "XposedBridge_MTCE";

    private XposedBridge() {}

    public static void log(String text) {
        Log.i(TAG, text);
    }

    public static void log(Throwable t) {
        Log.e(TAG, "Xposed exception: ", t);
    }

    public static XC_MethodHook.Unhook hookMethod(java.lang.reflect.Member hookMethod, XC_MethodHook callback) {
        // Provided at runtime by the Xposed/LSPosed framework
        return null;
    }
}

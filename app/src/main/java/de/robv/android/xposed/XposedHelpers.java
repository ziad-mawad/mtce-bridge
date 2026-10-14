package de.robv.android.xposed;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * XposedHelpers utility methods for reflection and hooking.
 */
public final class XposedHelpers {
    private static final String TAG = "XposedHelpers_MTCE";

    private XposedHelpers() {}

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Class not found: " + className);
            return null;
        }
    }

    public static Object findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        if (clazz == null || methodName == null || parameterTypesAndCallback == null || parameterTypesAndCallback.length == 0) {
            return null;
        }
        int callbackIndex = parameterTypesAndCallback.length - 1;
        Object callbackObj = parameterTypesAndCallback[callbackIndex];
        if (!(callbackObj instanceof XC_MethodHook)) {
            Log.e(TAG, "Last argument must be an XC_MethodHook instance");
            return null;
        }
        XC_MethodHook callback = (XC_MethodHook) callbackObj;

        Class<?>[] paramTypes = new Class<?>[callbackIndex];
        for (int i = 0; i < callbackIndex; i++) {
            Object obj = parameterTypesAndCallback[i];
            if (obj instanceof Class<?>) {
                paramTypes[i] = (Class<?>) obj;
            } else if (obj instanceof String) {
                paramTypes[i] = findClass((String) obj, clazz.getClassLoader());
            }
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return XposedBridge.hookMethod(method, callback);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to hook " + clazz.getName() + "#" + methodName + ": " + t.getMessage());
            return null;
        }
    }

    public static Object findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        Class<?> clazz = findClass(className, classLoader);
        if (clazz != null) {
            return findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        }
        return null;
    }
}

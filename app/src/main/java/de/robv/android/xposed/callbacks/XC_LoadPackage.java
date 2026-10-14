package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;

/**
 * Xposed XC_LoadPackage parameter holder.
 */
public class XC_LoadPackage {
    public static class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstApplication;
    }
}

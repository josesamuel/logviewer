package com.josesamuel.logviewer.view;

import com.android.ddmlib.AndroidDebugBridge;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;

public class AdbBridgeFactory {

    public static ListenableFuture<AndroidDebugBridge> getAdb(File adb) {
        try {

            Class adbService = Class.forName("com.android.tools.idea.adb.AdbService");
            Object instance = adbService.getMethod("getInstance").invoke(null);
            return (ListenableFuture<AndroidDebugBridge>) instance.getClass().getMethod("getDebugBridge", File.class).invoke(instance, adb);
        } catch (Exception ex) {

            try {
                Class adbService = Class.forName("com.android.tools.idea.ddms.adb.AdbService");
                Object instance = adbService.getMethod("getInstance").invoke(null);
                return (ListenableFuture<AndroidDebugBridge>) instance.getClass().getMethod("getDebugBridge", File.class).invoke(instance, adb);
            } catch (Exception ex2) {
                throw new RuntimeException(ex);
            }
        }
    }

}

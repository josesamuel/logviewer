package com.josesamuel.logviewer.view;

import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

interface MessageFormatter {
    Pattern PROCESS_ID = Pattern.compile("\\d+");
    Pattern THREAD_ID = Pattern.compile("\\d+");
    Pattern PACKAGE = Pattern.compile("\\S+");
    Pattern PRIORITY = Pattern.compile("[VDIWEAF]");
    Pattern TAG = Pattern.compile("[^ ]+");
    Pattern MESSAGE = Pattern.compile(".*");

    @NotNull
    String format(@NotNull String var1, @NotNull LogCatHeader var2, @NotNull String var3);

    @Nullable
    LogCatMessage tryParse(@NotNull String var1);
}

package com.josesamuel.logviewer.util;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class UIUtil {

    public static void invokeLaterIfNeeded(@NotNull Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void invokeAndWaitIfNeeded(@NotNull Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception e) {
            }
        }

    }
}

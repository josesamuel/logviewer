package com.josesamuel.logviewer.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Executes given background tasks using {@link com.intellij.openapi.progress.ProgressManager}
 * by making sure only one is allowed at one time
 * Others will be ignored
 */
public class SingleTaskBackgroundExecutor {

    private static final Object REFRESH_LOCK = new Object();
    private static boolean refreshInProgress;

    /**
     * Task to be run in background
     */
    public interface BackgroundTask {
        /**
         * Gets called to run the task.
         * Update the progress using the indicator
         */
        void run(ProgressIndicator progressIndicator);

        /**
         * Returns the name of the task
         */
        default String getTaskName() {
            return "";
        }

        /**
         * Called if this task is ignored because some other task is running.
         */
        default void onTaskIgnored() {
        }

        /**
         * Called when task is completed.
         */
        default void onTaskComplete() {
        }
    }


    /**
     * Executes the given task if no other tasks are running. If it is running, it will be ignored
     */
    public static void executeIfPossible(Project project, BackgroundTask task) {
        synchronized (REFRESH_LOCK) {
            if (refreshInProgress) {
                task.onTaskIgnored();
                return;
            }
            refreshInProgress = true;
        }

        Task.Backgroundable backgroundable = new Task.Backgroundable(project, task.getTaskName()) {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                try {
                    UIUtil.invokeAndWaitIfNeeded((Runnable) () -> progressIndicator.setFraction(0));
                    task.run(progressIndicator);
                } catch (Throwable ignored) {
                } finally {
                    try {
                        UIUtil.invokeAndWaitIfNeeded((Runnable) () -> progressIndicator.setFraction(1.0));
                    } catch (Exception ignored) {
                    }
                    synchronized (REFRESH_LOCK) {
                        refreshInProgress = false;
                    }
                    task.onTaskComplete();
                }
            }
        };

        try {
            UIUtil.invokeLaterIfNeeded(() -> ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundable, new BackgroundableProcessIndicator(backgroundable)));
        } catch (Exception ignored) {
        }

    }
}

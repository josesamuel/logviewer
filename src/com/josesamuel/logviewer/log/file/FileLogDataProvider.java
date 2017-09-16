package com.josesamuel.logviewer.log.file;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import com.josesamuel.logviewer.log.LogDataListener;
import com.josesamuel.logviewer.log.LogDataProvider;
import com.josesamuel.logviewer.log.LogProcess;
import com.josesamuel.logviewer.log.file.reader.LogFileReader;
import com.josesamuel.logviewer.log.file.reader.LogFileReaderFactory;
import com.josesamuel.logviewer.util.SingleTaskBackgroundExecutor;

import java.io.File;
import java.util.Set;

/**
 * {@link LogDataProvider} for a {@link FileLogSource}
 * This reads all the contents from the file and returns it as the data
 */
public class FileLogDataProvider implements LogDataProvider {

    private LogDataListener logListener;
    private StringBuffer logData;
    private Set<LogProcess> processes;
    private File file;
    private Project project;

    /**
     * Initialize with given file
     */
    FileLogDataProvider(Project project, File file) {
        this.file = file;
        this.project = project;
    }

    @Override
    public void registerLogListener(LogDataListener logListener) {
        this.logListener = logListener;
        logListener.onCleared();
        if (logData == null) {
            loadLogs(file);
        } else {
            populateLogs();
        }
    }

    @Override
    public void unRegisterLogListener(LogDataListener logListener) {
        this.logListener = null;
    }

    @Override
    public void dispose() {
        logData = null;
        processes.clear();
        processes = null;
    }

    /**
     * Load logs from the given file
     */
    private void loadLogs(final File file) {
        SingleTaskBackgroundExecutor.executeIfPossible(project, new SingleTaskBackgroundExecutor.BackgroundTask() {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    UIUtil.invokeAndWaitIfNeeded((Runnable) () -> progressIndicator.setFraction(0.10));
                    // start your process
                    LogFileReader fileReader = LogFileReaderFactory.getFileReader(file);
                    UIUtil.invokeAndWaitIfNeeded((Runnable) () -> progressIndicator.setFraction(0.15));
                    logData = fileReader.getFileData(logListener);
                    UIUtil.invokeAndWaitIfNeeded((Runnable) () -> progressIndicator.setFraction(0.70));
                    processes = fileReader.getProcesses();
                    UIUtil.invokeAndWaitIfNeeded((Runnable) () -> progressIndicator.setFraction(0.90));
                } catch (Exception ignored) {
                }

            }

            @Override
            public String getTaskName() {
                return "Loading " + file.getName();
            }

            @Override
            public void onTaskComplete() {
                populateLogs();
            }
        });
    }

    private void populateLogs() {
        if (logListener != null && logData != null) {
            logListener.onProcessList(processes);
            logListener.onLogData(logData.toString());
        }
    }


}

package com.josesamuel.logviewer.log;


import java.util.Set;

/**
 * Used by {@link LogDataProvider} to notify about new log data.
 */
public interface LogDataListener {

    /**
     * Called when a new line of log is available
     */
    void onLogLine(String log, LogProcess process);

    /**
     * Called when a data of log consisting of multiple lines are available
     */
    void onLogData(String log);

    /**
     * Called when the data needs to be cleard
     */
    void onCleared();

    /**
     * Called to update the set of current {@link LogProcess}
     */
    void onProcessList(Set<LogProcess> processList);

    /**
     * Used for debugging
     */
    void debug(String log);
}

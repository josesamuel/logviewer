package com.josesamuel.logviewer.log;


import com.intellij.openapi.Disposable;

/**
 * Provider of log data including the log lines, and process lists
 */
public interface LogDataProvider extends Disposable {

    /**
     * Register a listener to the provider. This will initialize the provider to start listening or reading from its source
     */
    void registerLogListener(LogDataListener logListener);

    /**
     * Unregisters a listener.
     */
    void unRegisterLogListener(LogDataListener logListener);
}

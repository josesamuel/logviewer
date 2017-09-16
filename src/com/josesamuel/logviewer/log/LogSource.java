package com.josesamuel.logviewer.log;


import com.android.ddmlib.IDevice;

/**
 * Represents a source for log.
 * This could be a device, or a file
 */
public interface LogSource {

    /**
     * Returns the source device
     */
    IDevice getSource();

    /**
     * Returns the provider of the log for this source
     */
    LogDataProvider getLogProvider();
}

package com.josesamuel.logviewer.log.dnd;

import java.io.File;

/**
 * Listener to get notified about file drop
 */
public interface DnDListener {

    /**
     * Called when a file is dropped
     */
    void onFileDropped(File file);
}

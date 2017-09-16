package com.josesamuel.logviewer.log.file;

import com.android.ddmlib.IDevice;
import com.intellij.openapi.project.Project;
import com.josesamuel.logviewer.log.LogDataProvider;
import com.josesamuel.logviewer.log.LogSource;

import java.io.File;

/**
 * A {@link LogSource} that provides data from a file
 */
public class FileLogSource implements LogSource {

    private File file;
    private FileDevice device;
    private FileLogDataProvider fileLogDataProvider;

    /**
     * Initialize the {@link FileLogSource} with given file
     */
    public FileLogSource(Project project, File file) {
        this.file = file;
        this.device = new FileDevice(file);
        this.fileLogDataProvider = new FileLogDataProvider(project, file);
    }

    @Override
    public IDevice getSource() {
        return device;
    }

    @Override
    public LogDataProvider getLogProvider() {
        return fileLogDataProvider;
    }

    @Override
    public String toString() {
        return file.getName() + " " + super.toString();
    }
}

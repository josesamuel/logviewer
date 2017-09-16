package com.josesamuel.logviewer.log.file.reader;


import com.android.ddmlib.logcat.LogCatMessage;
import com.android.tools.idea.logcat.AndroidLogcatFormatter;
import com.josesamuel.logviewer.log.LogDataListener;
import com.josesamuel.logviewer.log.LogProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LogFileReader {

    private File file;
    private Set<LogProcess> processes;

    LogFileReader(File file) {
        this.file = file;
        this.processes = new HashSet<>();
    }

    public File getFile() {
        return file;
    }

    public StringBuffer getFileData(LogDataListener listener) throws Exception {
        BufferedReader reader = null;
        StringBuffer data = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file)));
            data = getFileData(reader, null);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
            }
        }
        return data;
    }

    public Set<LogProcess> getProcesses() {
        return processes;
    }

    StringBuffer getFileData(BufferedReader reader, String fileName) throws Exception {
        StringBuffer data = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            updateAndAddLine(data, line, fileName);
        }
        return data;
    }

    protected void updateAndAddLine(StringBuffer data, String line, String fileName) {
        LogProcess logProcess = new LogProcess();

        String log = LogLineUpdater.parseLogLine(line, fileName, logProcess);
        if (log != null && !log.isEmpty()) {
            data.append(log).append('\n');
            if (logProcess.getProcessName() != null) {
                processes.add(new LogProcess().setProcessID(logProcess.getProcessID())
                        .setProcessName(logProcess.getProcessName()));
            }
        }

    }

}

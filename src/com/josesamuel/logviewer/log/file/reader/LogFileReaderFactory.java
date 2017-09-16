package com.josesamuel.logviewer.log.file.reader;

import java.io.File;

public class LogFileReaderFactory {

    public static LogFileReader getFileReader(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".tgz")) {
            return new TgzLogFileReader(file);
        }
        if (fileName.endsWith(".tar")) {
            return new TarLogFileReader(file);
        }
        if (fileName.endsWith(".zip")) {
            return new ZipLogFileReader(file);
        }
        return new LogFileReader(file);
    }
}

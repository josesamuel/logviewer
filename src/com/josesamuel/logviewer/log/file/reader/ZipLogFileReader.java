package com.josesamuel.logviewer.log.file.reader;


import com.josesamuel.logviewer.log.LogDataListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ZipLogFileReader extends LogFileReader {


    ZipLogFileReader(File file) {
        super(file);
    }

    @Override
    public StringBuffer getFileData(LogDataListener listener) throws Exception {

        StringBuffer data = new StringBuffer();
        ZipFile zipFile = null;
        try {

            zipFile = new ZipFile(getFile());
            Enumeration entries = zipFile.entries();
            ZipEntry zipEntry;

            while (entries.hasMoreElements()) {
                zipEntry = (ZipEntry) entries.nextElement();
                if (!zipEntry.isDirectory()) {
                    data.append(getFileData(new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry))),
                            zipEntry.getName()));
                }
            }

        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Exception e) {
            }
        }
        return data;
    }


}

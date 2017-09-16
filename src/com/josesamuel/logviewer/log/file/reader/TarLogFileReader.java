package com.josesamuel.logviewer.log.file.reader;


import com.josesamuel.logviewer.log.LogDataListener;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

class TarLogFileReader extends LogFileReader {


    TarLogFileReader(File file) {
        super(file);
    }

    @Override
    public StringBuffer getFileData(LogDataListener listener) throws Exception {
        TarInputStream tis = null;
        StringBuffer data = null;
        try {
            tis = getTarInputStream();
            data = getFileData(tis);
        } finally {
            try {
                if (tis != null) {
                    tis.close();
                }
            } catch (Exception e) {
            }
        }
        return data;
    }

    StringBuffer getFileData(TarInputStream tis) throws Exception {
        StringBuffer data = new StringBuffer();

        TarEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
            String fName = entry.getName().substring(2);
            data.append(getFileData(new BufferedReader(
                    new InputStreamReader(tis)), fName));
        }

        return data;
    }

    TarInputStream getTarInputStream() throws Exception {
        return new TarInputStream(new FileInputStream(getFile()));
    }


}

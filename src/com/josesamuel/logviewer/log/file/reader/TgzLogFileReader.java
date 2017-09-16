package com.josesamuel.logviewer.log.file.reader;


import org.xeustechnologies.jtar.TarInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

class TgzLogFileReader extends TarLogFileReader {


    TgzLogFileReader(File file) {
        super(file);
    }

    @Override
    TarInputStream getTarInputStream() throws Exception {
        return new TarInputStream(new GZIPInputStream(
                new FileInputStream(getFile())));
    }

}

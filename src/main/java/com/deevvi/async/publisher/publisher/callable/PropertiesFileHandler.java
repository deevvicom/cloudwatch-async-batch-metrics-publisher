package com.deevvi.async.publisher.publisher.callable;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Handler for manipulating properties files associated to a file with logs.
 */
final class PropertiesFileHandler {

    private static final String BYTES_READ = "bytesRead";
    private static final String LAST_UPDATE = "lastUpdate";

    private final File fileHandler;
    private final Properties properties;

    /**
     * Constructor.
     *
     * @param fileName log file name
     * @throws IOException - if any IOException occurs
     */
    PropertiesFileHandler(final String fileName) throws IOException {

        Preconditions.checkNotNull(StringUtils.trimToNull(fileName), "File name cannot be null or empty.");
        this.fileHandler = new File(fileName);
        this.properties = new Properties();
        if (propertiesFileExists()) {

            loadProperties();
        }
    }

    private void loadProperties() throws IOException {

        try (InputStream input = new FileInputStream(fileHandler)) {

            // load a properties file
            properties.load(input);
        }
    }

    private void saveProperties() throws IOException {
        try (OutputStream outputStream = new FileOutputStream(fileHandler)) {

            // load a properties file
            properties.store(outputStream, "");
        }
    }

    void updateRecords(final int bytesRead) throws IOException {

        properties.setProperty(BYTES_READ, String.valueOf(getBytesRead() + bytesRead));
        properties.setProperty(LAST_UPDATE, String.valueOf(System.currentTimeMillis()));
        saveProperties();
    }

    boolean propertiesFileExists() {

        return fileHandler.exists() && fileHandler.isFile();
    }

    int getBytesRead() {

        if (!properties.containsKey(BYTES_READ)) {

            return 0;
        }

        return Integer.parseInt(properties.getProperty(BYTES_READ));
    }

    long getLastUpdateTimestamp() {

        if (!properties.containsKey(LAST_UPDATE)) {

            return 0;
        }

        return Long.parseLong(properties.getProperty(LAST_UPDATE));
    }

    void delete() {

        fileHandler.delete();
        properties.clear();
    }
}

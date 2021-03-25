package com.deevvi.async.publisher.utils;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Helper class for handling file-specific operations.
 */
public final class FileUtils {

    private static final String TIME_ROLLING_LOG_FILE_SUFFIX = "/metrics-logs-%s.log";
    private static final String PROPERTIES_FILE_EXTENSION = ".properties";
    private static final String EXTENSION_SEPARATOR = ".";

    private static final SimpleDateFormat TEMPLATE = new SimpleDateFormat("yyyy-MM-dd-HH");
    private static final Pattern TIME_ROLLING_LOG_FILE_PATTERN = Pattern.compile("metrics-logs-\\d{4}-\\d{1,2}-\\d{1,2}-\\d{1,2}.log\\z");

    /**
     * Private constructor, to avoid class init.
     */
    private FileUtils() {
    }

    /**
     * Build the name for the hourly rotate file with logs.
     *
     * @return file name
     */
    public static String generateLogFileTimeRollingSuffix() {

        return String.format(TIME_ROLLING_LOG_FILE_SUFFIX, TEMPLATE.format(new Date()));
    }

    /**
     * Build the associate properties file name for a given log file.
     *
     * @param logFile log file
     * @return name of properties file associated to log file
     */
    public static String generatePropertiesFileNameForLogFile(String logFile) {

        Preconditions.checkNotNull(StringUtils.trimToNull(logFile), "Log file name cannot be null or empty.");

        int extensionSeparator = logFile.lastIndexOf(EXTENSION_SEPARATOR);
        return logFile.substring(0, extensionSeparator) + PROPERTIES_FILE_EXTENSION;
    }

    /**
     * Validate if a file is valid log file.
     *
     * @param file input file
     * @return true if is a valid log, false otherwise
     */
    public static boolean isLogFile(File file) {

        return TIME_ROLLING_LOG_FILE_PATTERN.matcher(file.getName()).find();
    }

    /**
     * Validate path where log files are going to be stored.
     * 
     * - If folder doesn't exist, it is created.
     * - If path points to a file, an exception is thrown.
     *
     * @param filePath - path where log files are going to be stored
     * @return file path
     */
    public static String validatePath(String filePath) {

        File file = new File(filePath);
        if (!filePath.endsWith("/")) {
            filePath = filePath + "/";
        }

        if (!file.exists()) {
            file.mkdir();
        } else {
            if (!file.isDirectory()) {
                throw new IllegalArgumentException("Invalid path: it must point to a directory, not a file.");
            }
        }

        return filePath;
    }
}

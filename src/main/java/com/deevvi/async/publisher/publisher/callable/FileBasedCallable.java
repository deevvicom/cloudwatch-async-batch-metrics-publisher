package com.deevvi.async.publisher.publisher.callable;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.deevvi.async.publisher.utils.FileUtils;
import com.deevvi.async.publisher.utils.JSONUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Async CW publisher using a file for reading metrics.
 */
public final class FileBasedCallable implements Callable<Object> {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FileBasedCallable.class);

    /**
     * Maximum number of items to publish to CW per batch
     */
    private static final int MAX_ITEMS_PER_BATCH = 18;

    /**
     * Default retention period for a file.
     */
    private static final int DEFAULT_TIME_TO_KEEP_FILES = 1000 * 3600 * 3;

    /**
     * Location where log files are persisted.
     */
    private final String filePath;

    /**
     * AWS CW client.
     */
    private final AmazonCloudWatch client;

    /**
     * Sleep time between 2 runs.
     */
    private final int millisBetweenRun;

    /**
     * CW namespace.
     */
    private final String namespace;

    /**
     * Time since last update until files are delete. Default is 3h.
     */
    private long maxTimeToKeepFilesInMillis = DEFAULT_TIME_TO_KEEP_FILES;

    /**
     * Constructor.
     *
     * @param client           AWS CW client
     * @param filePath         location where logs are persisted
     * @param namespace        CW namespace
     * @param millisBetweenRun time to sleep between 2 runs
     */
    public FileBasedCallable(final AmazonCloudWatch client, final String filePath, final String namespace, final int millisBetweenRun) {

        Preconditions.checkNotNull(client, "CW client cannot be null or empty.");
        Preconditions.checkNotNull(StringUtils.trimToNull(filePath), "File path cannot be null or empty.");
        Preconditions.checkNotNull(StringUtils.trimToNull(namespace), "Namespace cannot be null or empty.");
        Preconditions.checkArgument(millisBetweenRun > 0, "Wait time cannot be negative.");

        this.filePath = filePath;
        this.client = client;
        this.millisBetweenRun = millisBetweenRun;
        this.namespace = namespace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object call() throws Exception {

        while (true) {

            run();
        }
    }

    @VisibleForTesting
    void run() throws IOException {
        getAllFiles().forEach(file -> {
            if (!file.isDirectory() && FileUtils.isLogFile(file)) {
                try {
                    String propertiesFileName = FileUtils.generatePropertiesFileNameForLogFile(file.getName());
                    PropertiesFileHandler handler = new PropertiesFileHandler(filePath + propertiesFileName);
                    if (fileIsProcessed(file, handler)) {
                        LOG.info("File {} is completely processed.", file);
                        if (checkNoRecentUpdate(handler.getLastUpdateTimestamp())) {

                            LOG.info("Delete old file {}", file);
                            deleteFile(file, handler);
                        }
                    } else {

                        LOG.info("Processing file {}", file);
                        processFile(file, handler);
                    }
                } catch (IOException e) {

                    LOG.warn("Exception on processing file {} {}", file, e);
                }
            } else {

                LOG.info("Not a log file {}. Skipping.", file);
            }
        });

        sleep(millisBetweenRun);
    }

    /**
     * Set the logs retention period.
     *
     * @param maxTimeToKeepFilesInMillis retention period in milliseconds§
     */
    public void setMaxTimeToKeepFilesInMillis(final long maxTimeToKeepFilesInMillis) {

        Preconditions.checkArgument(maxTimeToKeepFilesInMillis > 0, "Retention period cannot be negative");

        this.maxTimeToKeepFilesInMillis = maxTimeToKeepFilesInMillis;
    }

    private void sleep(final int maxMillisToWait) throws IOException {

        try {

            Thread.sleep(maxMillisToWait);
        } catch (InterruptedException e) {

            throw new IOException(e);
        }
    }

    private void processFile(File file, PropertiesFileHandler handler) throws IOException {

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            reader.skip(handler.getBytesRead());
            String line;
            int bytesRead = 0;
            List<MetricDatum> metricDatums = Lists.newArrayList();
            while ((line = reader.readLine()) != null) {

                bytesRead += line.getBytes().length + 1;
                JSONUtils.decodeJSON(line).ifPresent(metricDatums::add);

                if (metricDatums.size() >= MAX_ITEMS_PER_BATCH) {

                    boolean result = tryPushMetricToCW(metricDatums);
                    if (result) {

                        handler.updateRecords(bytesRead);
                        bytesRead = 0;
                        metricDatums.clear();
                    }
                }
            }

            boolean result = tryPushMetricToCW(metricDatums);
            if (result) {

                handler.updateRecords(bytesRead);
            }
        }
    }

    private boolean tryPushMetricToCW(List<MetricDatum> metricDatums) {

        PutMetricDataRequest request = new PutMetricDataRequest()
                .withNamespace(namespace)
                .withMetricData(metricDatums);

        try {

            if (!metricDatums.isEmpty()) {

                client.putMetricData(request);
                LOG.info("Published {} metrics in a batch into CW.", metricDatums.size());
            }
            return true;
        } catch (Exception e) {

            LOG.warn("Exception on publishing metrics into CW:", e);
            return false;
        }
    }

    private void deleteFile(File file, PropertiesFileHandler handler) {

        file.delete();
        handler.delete();
        LOG.info("Deleted file: {}", file.getName());
    }

    private boolean fileIsProcessed(File file, PropertiesFileHandler handler) {

        return handler.propertiesFileExists()
                && file.length() == handler.getBytesRead();
    }

    private boolean checkNoRecentUpdate(long lastUpdateTimestamp) {

        return System.currentTimeMillis() - lastUpdateTimestamp > maxTimeToKeepFilesInMillis;
    }

    private List<File> getAllFiles() {

        File[] files = new File(filePath).listFiles();
        if (files == null) {

            return Lists.newArrayList();
        }

        List<File> theFiles = Arrays.asList(files);
        Collections.sort(theFiles);
        return ImmutableList.copyOf(theFiles);
    }
}

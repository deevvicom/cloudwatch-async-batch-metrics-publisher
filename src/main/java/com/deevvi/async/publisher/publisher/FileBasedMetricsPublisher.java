package com.deevvi.async.publisher.publisher;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.deevvi.async.publisher.publisher.callable.FileBasedCallable;
import com.deevvi.async.publisher.utils.FileUtils;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.deevvi.async.publisher.utils.FileUtils.generateLogFileTimeRollingSuffix;
import static com.deevvi.async.publisher.utils.JSONUtils.encodeToJSON;

/**
 * Metrics publisher using a file as a buffer.
 * Metrics are accumulated to a file and from that another process reads and publish to CW.
 */
public final class FileBasedMetricsPublisher implements MetricsPublisher {

    /**
     * Folder where to store the files.
     */
    private final String filePath;
    /**
     * Executors that handles process for reading from that file.
     */
    private final ExecutorService logsPublisher;

    /**
     * Constructor.
     *
     * @param client           - AWS CloudWatch client
     * @param filePath         - path where to store files
     * @param namespace        - namespace used for CW publishing
     * @param millisBetweenRun - time interval in millis between 2 file reads
     */
    public FileBasedMetricsPublisher(final AmazonCloudWatch client,
                                     final String filePath,
                                     final String namespace,
                                     final int millisBetweenRun) {

        Preconditions.checkNotNull(client, "AWS client cannot be null.");
        Preconditions.checkNotNull(StringUtils.trimToNull(filePath), "File path cannot be null or empty.");
        Preconditions.checkNotNull(StringUtils.trimToNull(namespace), "Namespace cannot be null or empty.");
        Preconditions.checkArgument(millisBetweenRun > 0, "Wait time interval cannot be negative.");

        FileUtils.validatePath(filePath);
        this.filePath = filePath;
        this.logsPublisher = Executors.newSingleThreadExecutor();
        logsPublisher.submit(new FileBasedCallable(client, filePath, namespace, millisBetweenRun));
    }

    /**
     * Constructor.
     *
     * @param client                    - AWS CloudWatch client
     * @param filePath                  - path where to store files
     * @param namespace                 - namespace used for CW publishing
     * @param millisBetweenRun          - time interval in millis between 2 file reads
     * @param logsRetentionPeriodMillis - time interval in millis while logs are stored on disk
     */
    public FileBasedMetricsPublisher(final AmazonCloudWatch client,
                                     final String filePath,
                                     final String namespace,
                                     final int millisBetweenRun,
                                     final int logsRetentionPeriodMillis) {

        Preconditions.checkNotNull(client, "AWS client cannot be null.");
        Preconditions.checkNotNull(StringUtils.trimToNull(filePath), "File path cannot be null or empty.");
        Preconditions.checkNotNull(StringUtils.trimToNull(namespace), "Namespace cannot be null or empty.");
        Preconditions.checkArgument(millisBetweenRun > 0, "Wait time interval cannot be negative.");
        Preconditions.checkArgument(logsRetentionPeriodMillis > 0, "Retention period interval cannot be negative.");

        FileUtils.validatePath(filePath);
        this.filePath = filePath;
        this.logsPublisher = Executors.newSingleThreadExecutor();
        FileBasedCallable task = new FileBasedCallable(client, filePath, namespace, millisBetweenRun);
        task.setMaxTimeToKeepFilesInMillis(logsRetentionPeriodMillis);
        logsPublisher.submit(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(final Collection<MetricDatum> metrics) throws IOException {

        if (logsPublisher.isShutdown()) {

            throw new IOException("Publisher channel is closed.");
        }

        String fullFilePath = filePath + generateLogFileTimeRollingSuffix();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fullFilePath, true))) {

            for (MetricDatum datum : metrics) {

                bw.write(encodeToJSON(datum));
                bw.newLine();
            }
        } catch (IOException e) {

            throw new IOException("Exception on writing to file: " + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closePublisher() {

        logsPublisher.shutdown();
    }
}

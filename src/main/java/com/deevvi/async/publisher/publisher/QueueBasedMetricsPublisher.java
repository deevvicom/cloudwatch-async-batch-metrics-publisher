package com.deevvi.async.publisher.publisher;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.deevvi.async.publisher.publisher.callable.QueueBasedCallable;
import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Metrics publisher that uses a queue to submit metrics to CW.
 */
public final class QueueBasedMetricsPublisher implements MetricsPublisher {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(QueueBasedMetricsPublisher.class);

    /**
     * Queue used to make operations async.
     */
    private final BlockingQueue<MetricDatum> metricsQueue;

    /**
     * Internal executor that handles the thread that publishes metrics into CW.
     */
    private final ExecutorService logsPublisher;

    /**
     * Constructor.
     *
     * @param client          AWS client
     * @param namespace       metrics namespace
     * @param maxMillisToWait maximum interval to wait until to publish metrics in CW
     */
    public QueueBasedMetricsPublisher(final AmazonCloudWatch client,
                                      final String namespace,
                                      final int maxMillisToWait) {

        Preconditions.checkNotNull(client, "AWS client cannot be null.");
        Preconditions.checkNotNull(StringUtils.trimToNull(namespace), "Namespace cannot be null or empty.");
        Preconditions.checkArgument(maxMillisToWait > 0, "Wait time interval cannot be negative.");

        this.metricsQueue = Queues.newLinkedBlockingQueue();
        this.logsPublisher = Executors.newSingleThreadExecutor();
        this.logsPublisher.submit(new QueueBasedCallable(metricsQueue, client, namespace, maxMillisToWait));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(final Collection<MetricDatum> metrics) throws IOException {

        Preconditions.checkNotNull(metrics, "Metrics list cannot be null");

        if (logsPublisher.isShutdown()) {

            throw new IOException("Publisher channel is closed.");
        }

        for (MetricDatum measure : metrics) {

            try {

                metricsQueue.put(measure);
                LOG.info("Published {} metrics to the queue. ", metrics.size());
            } catch (Exception e) {

                throw new IOException("Exception on adding item to queue.");
            }
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

package com.deevvi.async.publisher.publisher.callable;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Async CW publisher using a queue for reading metrics.
 */
public final class QueueBasedCallable implements Callable<Object> {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(QueueBasedCallable.class);

    /**
     * Maximum number of items to publish to CW per batch
     */
    private static final int MAX_ITEMS_PER_BATCH = 18;

    /**
     * Queue used for reading metrics to publish.
     */
    private final BlockingQueue<MetricDatum> metricsQueue;

    /**
     * CloudWatch client.
     */
    private final AmazonCloudWatch client;

    /**
     * Maximum interval to wait to buffer metrics.
     */
    private final int maxMillisToWait;

    /**
     * CloudWatch metrics namespace.
     */
    private final String namespace;

    /**
     * Constructor.
     *
     * @param metricsQueue    queue used for reading metrics
     * @param client          AWS CW client
     * @param namespace       CW namespace
     * @param maxMillisToWait maximum time interval to wait to buffer metrics
     */
    public QueueBasedCallable(final BlockingQueue<MetricDatum> metricsQueue, final AmazonCloudWatch client, final String namespace, final int maxMillisToWait) {

        Preconditions.checkNotNull(metricsQueue, "Queue cannot be null.");
        Preconditions.checkNotNull(client, "AWS client cannot be null.");
        Preconditions.checkNotNull(StringUtils.trimToNull(namespace), "Namespace cannot be null or empty.");
        Preconditions.checkArgument(maxMillisToWait > 0, "Wait time cannot be negative.");

        this.metricsQueue = metricsQueue;
        this.client = client;
        this.maxMillisToWait = maxMillisToWait;
        this.namespace = namespace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object call() {

        while (true) {

            try {
                long now = System.currentTimeMillis();
                List<MetricDatum> list = Lists.newArrayList();
                while (list.size() < MAX_ITEMS_PER_BATCH && System.currentTimeMillis() - now < maxMillisToWait) {

                    MetricDatum poll = metricsQueue.poll(maxMillisToWait, TimeUnit.MILLISECONDS);
                    if (poll != null) {
                        list.add(poll);
                    }
                }

                if (!list.isEmpty()) {

                    PutMetricDataRequest request = new PutMetricDataRequest()
                            .withNamespace(namespace)
                            .withMetricData(list);

                    client.putMetricData(request);
                    LOG.info("Published {} metrics in a batch into CW.", list.size());
                }

            } catch (Exception e) {

                LOG.warn(e.getMessage());
            }
        }
    }
}
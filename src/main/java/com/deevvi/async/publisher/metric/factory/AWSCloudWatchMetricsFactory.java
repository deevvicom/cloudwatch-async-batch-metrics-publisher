package com.deevvi.async.publisher.metric.factory;

import com.deevvi.async.publisher.metric.AWSCloudWatchMetric;
import com.deevvi.async.publisher.metric.Metric;
import com.deevvi.async.publisher.publisher.MetricsPublisher;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

/**
 * Implementation of {@link MetricsFactory} using AWS CloudWatch.
 */
public final class AWSCloudWatchMetricsFactory implements MetricsFactory {

    /**
     * Metrics publisher
     */
    private final MetricsPublisher metricsPublisher;

    /**
     * Constructor.
     *
     * @param metricsPublisher metrics publisher
     */
    public AWSCloudWatchMetricsFactory(final MetricsPublisher metricsPublisher) {

        Preconditions.checkNotNull(metricsPublisher, "Metrics publisher cannot be null or empty.");

        this.metricsPublisher = metricsPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metric newMetric(final String name) {

        Preconditions.checkNotNull(StringUtils.trimToNull(name), "Metrics name cannot be null or empty.");

        return new AWSCloudWatchMetric(metricsPublisher, name);
    }
}

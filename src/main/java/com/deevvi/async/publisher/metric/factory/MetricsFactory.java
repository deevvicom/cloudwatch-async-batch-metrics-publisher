package com.deevvi.async.publisher.metric.factory;

import com.deevvi.async.publisher.metric.Metric;

/**
 * Model for publishing a metrics.
 */
public interface MetricsFactory {

    /**
     * Create a metric.
     *
     * @param name metric name
     * @return new metric
     */
    Metric newMetric(String name);
}

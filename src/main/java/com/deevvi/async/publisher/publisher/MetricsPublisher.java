package com.deevvi.async.publisher.publisher;

import com.amazonaws.services.cloudwatch.model.MetricDatum;

import java.io.IOException;
import java.util.Collection;

/**
 * Model for publishing a collection of CW metrics.
 */
public interface MetricsPublisher {

    /**
     * Publish a collection of metrics to CW.
     *
     * @param metrics metrics collection
     * @throws IOException          - if an IO exception occurs
     * @throws NullPointerException - if argument is null
     */
    void publish(Collection<MetricDatum> metrics) throws IOException;

    /**
     * Close the publishing channel.
     */
    void closePublisher();
}

package com.deevvi.async.publisher.metric;

/**
 * Model for publishing a utils.
 */
public interface Metric {

    /**
     * Open a metric.
     *
     */
    void open();

    /**
     * Close a metric.
     *
     */
    void close();

    /**
     * Add a measure to a metric.
     * @param name metric name
     * @param value metric value
     *
     */
    void addMeasure(String name, double value);

    /**
     * Remove all measures from a metric.
     *
     */
    void resetMeasures();
}
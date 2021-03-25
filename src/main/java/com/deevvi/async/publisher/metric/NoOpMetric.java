package com.deevvi.async.publisher.metric;

/**
 * Implementation with no behavior, used for testing.
 */
public final class NoOpMetric implements Metric {

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMeasure(final String name, final double value) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetMeasures() {

    }
}
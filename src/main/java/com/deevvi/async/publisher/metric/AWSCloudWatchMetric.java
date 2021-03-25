package com.deevvi.async.publisher.metric;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.deevvi.async.publisher.publisher.MetricsPublisher;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import static com.amazonaws.services.cloudwatch.model.StandardUnit.Count;
import static com.amazonaws.services.cloudwatch.model.StandardUnit.Milliseconds;

/**
 * Metric implementation using AWS CloudWatch.
 */
public final class AWSCloudWatchMetric implements Metric {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AWSCloudWatchMetric.class);

    /**
     * Metrics publisher.
     */
    private final MetricsPublisher publisher;

    /**
     * The metric name.
     */
    private final String metricName;

    /**
     * List of measures to keep
     */
    private List<MetricDatum> measureList;

    
    private long openTimestamp;

    /**
     * Constructor.
     *
     * @param publisher metric publisher
     * @param name      metric name
     */
    public AWSCloudWatchMetric(final MetricsPublisher publisher, final String name) {

        Preconditions.checkNotNull(publisher, "Metrics publisher cannot be null.");
        Preconditions.checkNotNull(StringUtils.trimToNull(name), "Metrics name cannot be null or empty.");

        this.publisher = publisher;
        this.metricName = name;
        this.measureList = Lists.newArrayList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void open() {

        openTimestamp = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {

        Preconditions.checkArgument(openTimestamp > 0, "Missing open() call.");

        try {
            MetricDatum totalTime = new MetricDatum()
                    .withMetricName(metricName + ".Duration")
                    .withValue((double) (System.currentTimeMillis() - openTimestamp))
                    .withUnit(Milliseconds)
                    .withTimestamp(new Date(System.currentTimeMillis()));

            measureList.add(totalTime);
            publisher.publish(measureList);
            resetMeasures();
        } catch (Exception e) {
            LOG.info("Exception on publishing utils into AWS CloudWatch: ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMeasure(final String name, final double value) {

        Preconditions.checkNotNull(StringUtils.trimToNull(name), "Measure name cannot be null or empty.");
        Preconditions.checkArgument(openTimestamp > 0, "Missing open() call.");

        measureList.add(new MetricDatum()
                .withMetricName(metricName + "." + name)
                .withValue(value)
                .withUnit(Count)
                .withTimestamp(new Date(System.currentTimeMillis())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetMeasures() {

        measureList.clear();
    }
}

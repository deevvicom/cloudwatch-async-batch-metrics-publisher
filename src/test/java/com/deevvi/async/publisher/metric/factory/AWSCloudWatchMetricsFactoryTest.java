package com.deevvi.async.publisher.metric.factory;

import com.deevvi.async.publisher.metric.AWSCloudWatchMetric;
import com.deevvi.async.publisher.metric.Metric;
import com.deevvi.async.publisher.publisher.MetricsPublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;


/**
 * Tests for {@link AWSCloudWatchMetricsFactory} class
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AWSCloudWatchMetricsFactoryTest {

    private AWSCloudWatchMetricsFactory factory;
    private MetricsPublisher publisher;

    @BeforeAll
    public void setup() {

        publisher = Mockito.mock(MetricsPublisher.class);
        factory = new AWSCloudWatchMetricsFactory(publisher);
    }

    @Test
    public void testWithNullPublisher() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> new AWSCloudWatchMetricsFactory(null));
    }

    @Test
    public void testNewMetricEmptyName() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> factory.newMetric("  "));
    }

    @Test
    public void testComplete() {

        //call
        Metric metric = factory.newMetric("test-metric");

        //verify
        assertThat(metric).isNotNull();
        assertThat(metric).isInstanceOf(AWSCloudWatchMetric.class);
    }
}

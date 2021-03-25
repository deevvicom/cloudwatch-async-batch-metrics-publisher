package com.deevvi.async.publisher.metric;


import com.deevvi.async.publisher.publisher.MetricsPublisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link AWSCloudWatchMetric} class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AWSCloudWatchMetricTest {

    private AWSCloudWatchMetric metric;
    private MetricsPublisher publisher;

    @BeforeAll
    public void init() {

        publisher = Mockito.mock(MetricsPublisher.class);
        metric = new AWSCloudWatchMetric(publisher, "test");
    }


    @Test
    public void testNullPublisher() {

        //call
        assertThrows(NullPointerException.class, () -> new AWSCloudWatchMetric(null, "test"));
    }

    @Test
    public void testEmptyMetricName() {

        //call
        assertThrows(NullPointerException.class, () -> new AWSCloudWatchMetric(publisher, "  "));
    }

    @Test
    public void testAddMeasureNull() {

        //call
        assertThrows(NullPointerException.class, () -> metric.addMeasure("   ", -1));
    }

    @Test
    public void testAddMeasureWithoutOpen() {

        //call
        assertThrows(IllegalArgumentException.class, () -> metric.addMeasure("anymetric", -1));
    }

    @Test
    public void testComplete() throws IOException {

        //call
        metric.open();
        metric.addMeasure("request", 1.0);
        metric.addMeasure("success", 1.0);
        metric.addMeasure("failure", 0);
        metric.close();

        //verify
        Mockito.verify(publisher, times(1)).publish(anyList());
        Mockito.verifyNoMoreInteractions(publisher);
    }
}

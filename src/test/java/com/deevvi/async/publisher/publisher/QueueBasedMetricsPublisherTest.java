package com.deevvi.async.publisher.publisher;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link QueueBasedMetricsPublisher} class.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueueBasedMetricsPublisherTest {

    private QueueBasedMetricsPublisher publisher;
    private AmazonCloudWatch cloudWatch;

    @BeforeAll
    public void setup() {

        cloudWatch = mock(AmazonCloudWatch.class);
        publisher = new QueueBasedMetricsPublisher(cloudWatch, "test", 100);
    }

    @Test
    public void testNullCloudWatchClient() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> new QueueBasedMetricsPublisher(null, "test", 100));
    }

    @Test
    public void testNegativeWaitInterval() {

        //call
        Assertions.assertThrows(IllegalArgumentException.class, () -> new QueueBasedMetricsPublisher(cloudWatch, "test", -100));
    }

    @Test
    public void testEmptyNamespace() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> new QueueBasedMetricsPublisher(cloudWatch, "   ", 100));
    }

    @Test
    public void testExceptionAfterShutDown() {

        //setup
        publisher.closePublisher();

        //call
        Assertions.assertThrows(IOException.class, () -> publisher.publish(ImmutableList.of(new MetricDatum())));
    }

    @Test
    public void testExceptionWhenPublishNull() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> publisher.publish(null));
    }

    @Test
    public void testMetricsArePublished() throws IOException {

        //call
        publisher.publish(ImmutableList.of(new MetricDatum().withMetricName("m1"),
                new MetricDatum().withMetricName("m2")));

        //verify
        verifyNoMoreInteractions(cloudWatch);
    }
}

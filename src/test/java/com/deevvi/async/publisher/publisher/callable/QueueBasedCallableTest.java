package com.deevvi.async.publisher.publisher.callable;


import com.amazonaws.AmazonClientException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.amazonaws.services.cloudwatch.model.StandardUnit.Count;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link QueueBasedCallable} class.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class QueueBasedCallableTest {

    private QueueBasedCallable callable;
    private BlockingQueue<MetricDatum> metricsQueue;
    private AmazonCloudWatch cloudWatch;
    private ExecutorService service;
    private final int maxMillisToWait = 500;

    @BeforeEach
    public void setup() {

        cloudWatch = Mockito.mock(AmazonCloudWatch.class);
        metricsQueue = Queues.newLinkedBlockingQueue();
        service = Executors.newSingleThreadExecutor();

        callable = new QueueBasedCallable(metricsQueue, cloudWatch, "test", maxMillisToWait);
    }

    @Test
    public void testNullQueue() {

        //call
        Assertions.assertThrows(NullPointerException.class, () ->   new QueueBasedCallable(null, cloudWatch, "test", 10));
    }

    @Test
    public void testNullCWClient() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> new QueueBasedCallable(metricsQueue, null, "test", 10));
    }

    @Test
    public void testNegativeWaitTime() {

        //call
        Assertions.assertThrows(IllegalArgumentException.class, () ->   new QueueBasedCallable(metricsQueue, cloudWatch, "test", -10));
    }

    @Test
    public void testNullNamespace() {

        //call
        Assertions.assertThrows(NullPointerException.class, () ->      new QueueBasedCallable(metricsQueue, cloudWatch, " ", 10));
    }


    @Test
    public void testNoMetrics() throws Exception {

        //call
        service.submit(callable);
        Thread.sleep(2 * maxMillisToWait);
        service.shutdown();

        //verify
        verifyNoMoreInteractions(cloudWatch);
    }

    @Test
    public void testCWThrowsException() throws Exception {

        //setup
        metricsQueue.put(new MetricDatum().withMetricName("m1").withValue(1.0).withUnit(Count));
        metricsQueue.put(new MetricDatum().withMetricName("m2").withValue(1.0).withUnit(Count));
        when(cloudWatch.putMetricData(any())).thenThrow(new AmazonClientException("exception"));

        //call
        service.submit(callable);
        Thread.sleep(2 * maxMillisToWait);
        service.shutdown();

        //verify
        verify(cloudWatch, times(1)).putMetricData(any());
        verifyNoMoreInteractions(cloudWatch);
    }

    @Test
    public void testCWReturnsSuccess() throws Exception {

        //setup
        metricsQueue.put(new MetricDatum().withMetricName("m1").withValue(1.0).withUnit(Count));
        metricsQueue.put(new MetricDatum().withMetricName("m2").withValue(1.0).withUnit(Count));

        //call
        service.submit(callable);
        Thread.sleep(2 * maxMillisToWait);
        service.shutdown();

        //verify
        verify(cloudWatch, times(1)).putMetricData(any());
        verifyNoMoreInteractions(cloudWatch);
    }

    @Test
    public void testWaitTime() throws Exception {

        //setup
        MetricDatum m1 = new MetricDatum().withMetricName("m1").withValue(1.0).withUnit(Count);
        List<MetricDatum> datumList = Lists.newArrayList();
        datumList.add(m1);

        PutMetricDataRequest request = new PutMetricDataRequest()
                .withMetricData(m1)
                .withNamespace("test");

        metricsQueue.put(m1);

        //call
        service.submit(callable);
        Thread.sleep(3 * maxMillisToWait);
        metricsQueue.put(new MetricDatum().withMetricName("m2").withValue(1.0).withUnit(Count));
        service.shutdown();

        //verify
        verify(cloudWatch, times(1)).putMetricData(request);
        verifyNoMoreInteractions(cloudWatch);
    }

    @Test
    public void testMaxBatchSize() throws Exception {

        //setup
        for (int index = 0; index < 20; index++) {
            metricsQueue.put(new MetricDatum().withMetricName("m" + index).withValue(1.0).withUnit(Count));
        }


        //call
        service.submit(callable);
        Thread.sleep(2 * maxMillisToWait);
        service.shutdown();

        //verify
        verify(cloudWatch, times(2)).putMetricData(any());
        verifyNoMoreInteractions(cloudWatch);
    }
}

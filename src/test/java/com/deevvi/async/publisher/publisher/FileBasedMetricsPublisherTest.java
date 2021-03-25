package com.deevvi.async.publisher.publisher;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.deevvi.async.publisher.utils.FileUtils.generateLogFileTimeRollingSuffix;
import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link FileBasedMetricsPublisher} class.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class FileBasedMetricsPublisherTest {

    private FileBasedMetricsPublisher publisher;
    private AmazonCloudWatch cloudWatch;
    private String tmpDirPath;

    @BeforeEach
    public void init(@TempDir Path tempDir)  {

        tmpDirPath = tempDir.toAbsolutePath().toString()+ "/";
        cloudWatch = Mockito.mock(AmazonCloudWatch.class);
        publisher = new FileBasedMetricsPublisher(cloudWatch, tmpDirPath, "test", 100);
    }

    @Test
    public void testNullCWClient() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> new FileBasedMetricsPublisher(null, tmpDirPath, "test", 10));
    }

    @Test
    public void testNullPath() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> new FileBasedMetricsPublisher(cloudWatch, "   ", "test", 10));
    }


    @Test
    public void testEmptyNamespace() {

        //call
        Assertions.assertThrows(NullPointerException.class, () -> new FileBasedMetricsPublisher(cloudWatch, tmpDirPath, "   ", 10));
    }

    @Test
    public void testNegativeWaitTimeInterval() {

        //call
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FileBasedMetricsPublisher(cloudWatch, tmpDirPath, "test", -10));
    }

    @Test
    public void testNegativeRetentionTimeInterval() {

        //call
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FileBasedMetricsPublisher(cloudWatch,tmpDirPath, "test", 10, -1));
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

        //setup
        String file = tmpDirPath + generateLogFileTimeRollingSuffix();

        //call
        publisher.publish(ImmutableList.of(new MetricDatum().withMetricName("m1"),
                new MetricDatum().withMetricName("m2")));

        //verify
        assertThat(new File(tmpDirPath).exists()).isTrue();
        assertThat(new File(file).length()).isGreaterThan(0);
        Mockito.verifyNoMoreInteractions(cloudWatch);
    }

}

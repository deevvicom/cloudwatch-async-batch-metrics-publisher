package com.deevvi.async.publisher.publisher.callable;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

import com.deevvi.async.publisher.utils.JSONUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FileBasedCallable} class.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class FileBasedCallableTest {

    private FileBasedCallable callable;
    private AmazonCloudWatch cloudWatch;
    private String tmpDirPath;

    @BeforeEach
    public void setup(@TempDir Path tempDir){

        cloudWatch = Mockito.mock(AmazonCloudWatch.class);
        tmpDirPath = tempDir.toAbsolutePath().toString()+ "/";
        callable = new FileBasedCallable(cloudWatch, tmpDirPath , "test", 500);
    }

    @Test
    public void testNullCWClient() {

        //call
        assertThrows(NullPointerException.class, () -> new FileBasedCallable(null, tmpDirPath, "test", 10));
    }

    @Test
    public void testNullPath() {

        //call
        assertThrows(NullPointerException.class, () ->    new FileBasedCallable(cloudWatch, null, "test", 10));
    }

    @Test
    public void testNullNamespace() {

        //call
        assertThrows(NullPointerException.class, () ->   new FileBasedCallable(cloudWatch, tmpDirPath, " ", 10));
    }


    @Test
    public void testNegativeWaitTime() {

        //call
        assertThrows(IllegalArgumentException.class, () ->  new FileBasedCallable(cloudWatch, tmpDirPath, "test", -10));
    }

    @Test
    public void testNoMetrics() throws Exception {

        //call
        callable.run();

        //verify
        verifyNoMoreInteractions(cloudWatch);
    }

    @Test
    public void testProcessOneLine() throws Exception {

        //setup
        String fileName = "metrics-logs-2019-07-30-07.log";
        File file = new File(tmpDirPath + fileName);
        MetricDatum m1 = new MetricDatum().withMetricName("m1").withValue(1.0).withUnit(StandardUnit.Count).withTimestamp(new Date());

        String json = JSONUtils.encodeToJSON(m1);
        addToFile(file, json);

        //call
        callable.run();

        //verify
        PropertiesFileHandler handler = new PropertiesFileHandler(tmpDirPath + "/metrics-logs-2019-07-30-07.properties");

        assertThat(new File(tmpDirPath + "/metrics-logs-2019-07-30-07.properties").exists()).isTrue();
        assertThat(handler.getBytesRead()).isEqualTo( json.length() + 1);
        verify(cloudWatch, times(1)).putMetricData(any());
    }

    @Test
    public void testProcessOneLineCWFailsThenNoPropertiesFile() throws Exception {

        //setup
        MetricDatum m1 = new MetricDatum().withMetricName("m1").withValue(1.0).withUnit(StandardUnit.Count).withTimestamp(new Date());
        String fileName = "metrics-logs-2019-07-30-07.log";
        File file = new File(tmpDirPath + fileName);
        String json = JSONUtils.encodeToJSON(m1);
        addToFile(file, json);
        when(cloudWatch.putMetricData(any())).thenThrow(new AmazonServiceException("Internal Failure"));

        //call
        callable.run();

        //verify
        PropertiesFileHandler handler = new PropertiesFileHandler(new File(tmpDirPath + "/metrics-logs-2019-07-30-07.properties").toString());

        assertThat(new File(tmpDirPath + "/metrics-logs-2019-07-30-07.properties").exists()).isFalse();
        assertThat(handler.propertiesFileExists()).isFalse();
        verify(cloudWatch, times(1)).putMetricData(any());
    }


    @Test
    public void testInvalidLineIsSkipped() throws Exception {

        //setup
        MetricDatum m1 = new MetricDatum().withMetricName("m1").withValue(1.0).withUnit(StandardUnit.Count).withTimestamp(new Date());
        String fileName = "metrics-logs-2019-07-30-07.log";
        File file = new File(tmpDirPath + fileName);
        String json = JSONUtils.encodeToJSON(m1);
        addToFile(file, json);
        String invalidJSON = "FooBar";
        addToFile(file, invalidJSON);

        //call
        callable.run();

        //verify
        PropertiesFileHandler handler = new PropertiesFileHandler(tmpDirPath + "/metrics-logs-2019-07-30-07.properties");

        assertThat(new File(tmpDirPath + "/metrics-logs-2019-07-30-07.properties").exists()).isTrue();
        assertThat(handler.getBytesRead()).isEqualTo(json.length() + invalidJSON.length() + 2);
        verify(cloudWatch, times(1)).putMetricData(any());
    }

    @Test
    public void testOldFilesAreDeleted() throws Exception {

        //setup
        MetricDatum m1 = new MetricDatum().withMetricName("m1").withValue(1.0).withUnit(StandardUnit.Count).withTimestamp(new Date());
        String fileName = "metrics-logs-2019-07-30-07.log";
        File file = new File(tmpDirPath + fileName);
        String json = JSONUtils.encodeToJSON(m1);
        addToFile(file, json);
        PropertiesFileHandler handler = new PropertiesFileHandler(tmpDirPath +"/metrics-logs-2019-07-30-07.properties");
        handler.updateRecords(json.length() + 1);
        //one second
        callable.setMaxTimeToKeepFilesInMillis(1000);
        Thread.sleep(1000);

        //call
        callable.run();

        //verify
        assertThat(new File(tmpDirPath + "/metrics-logs-2019-07-30-07.properties").exists()).isFalse();
        assertThat(handler.propertiesFileExists()).isFalse();
    }


    private void addToFile(File file, String s) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {

            writer.write(s + "\n");
            writer.flush();
        }
    }
}

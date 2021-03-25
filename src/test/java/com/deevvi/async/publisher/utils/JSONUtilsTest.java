package com.deevvi.async.publisher.utils;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link JSONUtils} class.
 */
public class JSONUtilsTest {

    @Test
    public void testEncodeNullObject() {

        Assertions.assertThrows(NullPointerException.class, () -> JSONUtils.encodeToJSON(null));
    }

    @Test
    public void testDecodeEmptyString() {

        Assertions.assertThrows(NullPointerException.class, () -> JSONUtils.decodeJSON(" "));
    }

    @Test
    public void testCompleteMetricDatum() {

        MetricDatum datum = new MetricDatum()
                .withMetricName("test-12")
                .withValue(32.13)
                .withUnit(StandardUnit.Count)
                .withTimestamp(new Date(System.currentTimeMillis()));

        String json = JSONUtils.encodeToJSON(datum);

        Assertions.assertNotNull(json);

        Optional<MetricDatum> object = JSONUtils.decodeJSON(json);
        assertThat(object.isPresent()).isTrue();
        assertThat(datum.getMetricName()).isEqualTo(object.get().getMetricName());
        assertThat(datum.getUnit()).isEqualTo(object.get().getUnit());
    }
}

package com.deevvi.async.publisher.utils;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Helper class for manipulating SerDe operations for metric datum instances.
 */
public final class JSONUtils {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JSONUtils.class);

    /**
     * GSON instance.
     */
    private static final Gson GSON = new Gson();

    /**
     * Private constructor, to avoid class init.
     */
    private JSONUtils() {
    }

    /**
     * Encode the metric datum to a JSON.
     *
     * @param metricDatum instance to encode
     * @return JSON representation of input parameter
     */
    public static String encodeToJSON(MetricDatum metricDatum) {

        Preconditions.checkNotNull(metricDatum, "Argument cannot be null.");

        return GSON.toJson(metricDatum);
    }

    /**
     * Extract the metric datum from a string JSON.
     *
     * @param json the JSON representation of metric datum
     * @return metric datum.
     */
    public static Optional<MetricDatum> decodeJSON(String json) {

        Preconditions.checkNotNull(StringUtils.trimToNull(json), "Value cannot be null or empty.");

        try {
            return Optional.of(GSON.fromJson(json, MetricDatum.class));

        } catch (Exception e) {

            LOG.info("Exception on converting input {} ", json);
            return Optional.empty();
        }
    }
}

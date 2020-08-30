/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.internal.metrics.NoCacheHeader;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class MetricHandler implements Route.Handler {

  @Nonnull
  @Override
  public Object apply(@Nonnull Context ctx) {
    MetricRegistry registry = ctx.require(MetricRegistry.class);
    Map<String, Metric> allMetrics = registry.getMetrics();

    if (allMetrics.isEmpty()) {
      ctx.setResponseCode(StatusCode.NOT_IMPLEMENTED);
      NoCacheHeader.add(ctx);
      return allMetrics;
    } else {
      // params & filters
      String type = ctx.query("type").value("*");
      MetricFilter filter = ctx.query("name").toOptional()
          .<MetricFilter>map(name -> (n, m) -> n.startsWith(name))
          .orElse(MetricFilter.ALL);

      TimeUnit unit = TimeUnit.valueOf(ctx.query("unit").value("seconds").toUpperCase());
      String rateUnitLabel = calculateRateUnit(unit, "ops");
      double rateFactor = unit.toSeconds(1);

      String durationUnitLabel = unit.toString().toLowerCase(Locale.US);
      double durationFactor = 1.0 / unit.toNanos(1);

      boolean showSamples = ctx.query("showSamples").booleanValue(false);

      // dump metrics
      Map<String, Object> metrics = new TreeMap<>();
      Map<String, Object> counters = counters(registry.getCounters(filter));
      if (counters.size() > 0) {
        metrics.put("counters", counters);
      }
      Map<String, Object> gauges = gauges(registry.getGauges(filter));
      if (gauges.size() > 0) {
        metrics.put("gauges", gauges);
      }
      Map<String, Object> histograms = histograms(registry.getHistograms(filter), showSamples);
      if (histograms.size() > 0) {
        metrics.put("histograms", histograms);
      }
      Map<String, Object> meters = meters(registry.getMeters(filter), rateUnitLabel, rateFactor,
          durationUnitLabel, durationFactor);
      if (meters.size() > 0) {
        metrics.put("meters", meters);
      }
      Map<String, Object> timers = timers(registry.getTimers(filter), rateUnitLabel, rateFactor,
          durationUnitLabel, durationFactor, showSamples);
      if (timers.size() > 0) {
        metrics.put("timers", timers);
      }

      // send
      ctx.setResponseCode(StatusCode.OK);
      NoCacheHeader.add(ctx);

      //noinspection CollectionAddedToSelf
      return metrics.getOrDefault(type, metrics);
    }
  }

  private static Map<String, Object> timers(final SortedMap<String, Timer> timers,
      final String rateUnit, final double rateFactor, final String durationUnit,
      final double durationFactor, final boolean showSamples) {
    Map<String, Object> result = new TreeMap<>();
    timers.forEach((name, timer) -> result.put(name,
        timer(timer, rateUnit, rateFactor, durationUnit, durationFactor, showSamples)));
    return result;
  }

  private static Map<String, Object> timer(final Timer timer, final String rateUnit,
     final double rateFactor, final String durationUnit, final double durationFactor,
     final boolean showSamples) {
    Map<String, Object> result = meter(timer, rateUnit, rateFactor, durationUnit, durationFactor);

    result.putAll(snapshot(timer, durationFactor, showSamples));

    return result;
  }

  private static Map<String, Object> snapshot(final Sampling sampling, final double durationFactor,
      final boolean showSamples) {
    Map<String, Object> result = new TreeMap<>();
    final Snapshot snapshot = sampling.getSnapshot();
    result.put("max", snapshot.getMax() * durationFactor);
    result.put("mean", snapshot.getMean() * durationFactor);
    result.put("min", snapshot.getMin() * durationFactor);

    result.put("p50", snapshot.getMedian() * durationFactor);
    result.put("p75", snapshot.get75thPercentile() * durationFactor);
    result.put("p95", snapshot.get95thPercentile() * durationFactor);
    result.put("p98", snapshot.get98thPercentile() * durationFactor);
    result.put("p99", snapshot.get99thPercentile() * durationFactor);
    result.put("p999", snapshot.get999thPercentile() * durationFactor);

    if (showSamples) {
      final long[] values = snapshot.getValues();
      List<Double> scaledValues = new ArrayList<>(values.length);
      for (long value : values) {
        scaledValues.add(value * durationFactor);
      }
      result.put("values", scaledValues);
    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  private static Map<String, Object> gauges(final SortedMap<String, Gauge> gauges) {
    Map<String, Object> result = new TreeMap<>();
    gauges.forEach((name, gauge) -> {
      try {
        result.put(name, gauge.getValue());
      } catch (Exception ex) {
        result.put(name, ex.toString());
      }
    });
    return result;
  }

  private static Map<String, Object> counters(final SortedMap<String, Counter> counters) {
    Map<String, Object> result = new TreeMap<>();
    counters.forEach((name, c) -> result.put(name, c.getCount()));
    return result;
  }

  private static Map<String, Object> histograms(final SortedMap<String, Histogram> histograms,
      final boolean showSamples) {
    Map<String, Object> result = new TreeMap<>();
    histograms.forEach((name, timer) -> result.put(name, snapshot(timer, 1, showSamples)));
    return result;
  }

  private static Map<String, Object> meters(final SortedMap<String, Meter> timers,
      final String rateUnit, final double rateFactor, final String durationUnit,
      final double durationFactor) {
    Map<String, Object> result = new TreeMap<>();
    timers.forEach((name, timer) -> result.put(name,
        meter(timer, rateUnit, rateFactor, durationUnit, durationFactor)));
    return result;
  }

  private static Map<String, Object> meter(final Metered meter, final String rateUnit,
      final double rateFactor, final String durationUnit, final double durationFactor) {
    Map<String, Object> result = new TreeMap<>();
    result.put("count", meter.getCount());
    result.put("m15_rate", meter.getFifteenMinuteRate() * rateFactor);
    result.put("m1_rate", meter.getOneMinuteRate() * rateFactor);
    result.put("m5_rate", meter.getFiveMinuteRate() * rateFactor);
    result.put("mean_rate", meter.getMeanRate() * rateFactor);
    result.put("duration_units", durationUnit);
    result.put("rate_units", rateUnit);

    return result;
  }

  private static String calculateRateUnit(final TimeUnit unit, final String name) {
    final String s = unit.toString().toLowerCase(Locale.US);
    return name + '/' + s.substring(0, s.length() - 1);
  }
}

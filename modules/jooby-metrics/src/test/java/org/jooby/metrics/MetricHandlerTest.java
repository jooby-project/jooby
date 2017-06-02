package org.jooby.metrics;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;

public class MetricHandlerTest {

  @Test
  public void notImplemented() throws Exception {
    SortedMap<String, Metric> metrics = ImmutableSortedMap.of();
    new MockUnit(Request.class, Response.class, MetricRegistry.class)
        .expect(registry(metrics))
        .expect(send(Status.NOT_IMPLEMENTED, metrics))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void counters() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    registry.register("c", new Counter());
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("counters", ImmutableMap.of("c", 0L));

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "*"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", false))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void metricByType() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    registry.register("c1", new Counter());
    registry.register("c2", new Counter());
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("c1", 0L);
    result.put("c2", 0L);

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "counters"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", false))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void metricByName() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    registry.register("c1", new Counter());
    registry.register("c2", new Counter());
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("c1", 0L);

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.of("c1")))
        .expect(param("type", "counters"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", false))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void meters() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    Meter meter = new Meter();
    registry.register("m", meter);
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("meters", ImmutableMap.of("m", ImmutableMap.builder()
        .put("count", 0L)
        .put("duration_units", "seconds")
        .put("m15_rate", 0D)
        .put("m1_rate", 0D)
        .put("m5_rate", 0D)
        .put("mean_rate", 0D)
        .put("rate_units", "ops/second")
        .build()));

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "*"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", false))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void metersByUnit() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    Meter meter = new Meter();
    registry.register("m", meter);
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("meters", ImmutableMap.of("m", ImmutableMap.builder()
        .put("count", 0L)
        .put("duration_units", "milliseconds")
        .put("m15_rate", 0D)
        .put("m1_rate", 0D)
        .put("m5_rate", 0D)
        .put("mean_rate", 0D)
        .put("rate_units", "ops/millisecond")
        .build()));

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "*"))
        .expect(param("unit", "seconds", "milliseconds"))
        .expect(bparam("showSamples", false))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void timers() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    Timer meter = new Timer();
    registry.register("t", meter);
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("timers", ImmutableMap.of("t", ImmutableMap.builder()
        .put("count", 0L)
        .put("duration_units", "seconds")
        .put("m15_rate", 0D)
        .put("m1_rate", 0D)
        .put("m5_rate", 0D)
        .put("mean_rate", 0D)
        .put("rate_units", "ops/second")
        .put("max", 0D)
        .put("mean", 0D)
        .put("min", 0D)
        .put("p50", 0D)
        .put("p75", 0D)
        .put("p95", 0D)
        .put("p98", 0D)
        .put("p99", 0D)
        .put("p999", 0D)
        .put("values", new ArrayList<>())
        .build()));

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "*"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", true))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void histograms() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    Histogram h = new Histogram(new UniformReservoir());
    registry.register("h", h);
    h.update(7);
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("histograms",
        ImmutableMap.of("h", ImmutableMap.builder()
            .put("max", 7D)
            .put("mean", 7D)
            .put("min", 7D)
            .put("p50", 7D)
            .put("p75", 7D)
            .put("p95", 7D)
            .put("p98", 7D)
            .put("p99", 7D)
            .put("p999", 7D)
            .put("values", Lists.newArrayList(7D))
            .build()));

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "*"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", true))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void gauges() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    registry.register("g", new Gauge<Object>() {

      @Override
      public Object getValue() {
        return "v";
      }

    });
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("gauges", ImmutableMap.of("g", "v"));

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "*"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", false))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void gaugesErr() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    IllegalStateException v = new IllegalStateException("intentional err");
    registry.register("g", new Gauge<Object>() {

      @Override
      public Object getValue() {
        throw v;
      }

    });
    // result
    Map<String, Object> result = new TreeMap<>();
    result.put("gauges", ImmutableMap.of("g", v.toString()));

    new MockUnit(Request.class, Response.class)
        .expect(registry(registry))
        .expect(name("name", Optional.empty()))
        .expect(param("type", "*"))
        .expect(param("unit", "seconds", "seconds"))
        .expect(bparam("showSamples", false))
        .expect(send(Status.OK, result))
        .run(unit -> {
          new MetricHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  private Block param(final String name, final String value) {
    return param(name, "*", value);
  }

  private Block param(final String name, final String defvalue, final String value) {
    return unit -> {
      Mutant mvalue = unit.mock(Mutant.class);
      expect(mvalue.value(defvalue)).andReturn(value);

      Request req = unit.get(Request.class);
      expect(req.param(name)).andReturn(mvalue);
    };
  }

  private Block name(final String name, final Optional<String> value) {
    return unit -> {
      Mutant mvalue = unit.mock(Mutant.class);
      expect(mvalue.toOptional()).andReturn(value);

      Request req = unit.get(Request.class);
      expect(req.param(name)).andReturn(mvalue);
    };
  }

  private Block bparam(final String name, final boolean value) {
    return unit -> {
      Mutant mvalue = unit.mock(Mutant.class);
      expect(mvalue.booleanValue(false)).andReturn(value);

      Request req = unit.get(Request.class);
      expect(req.param(name)).andReturn(mvalue);
    };
  }

  private Block registry(final MetricRegistry registry) {
    return unit -> {
      Request req = unit.get(Request.class);
      expect(req.require(MetricRegistry.class)).andReturn(registry);
    };
  }

  private Block send(final Status status, final Object metrics) {
    return unit -> {
      Response rsp = unit.get(Response.class);
      expect(rsp.status(eq(status))).andReturn(rsp);
      expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
      rsp.send(eq(metrics));
    };
  }

  private Block registry(final SortedMap<String, Metric> metrics) {
    return unit -> {
      MetricRegistry registry = unit.get(MetricRegistry.class);
      expect(registry.getMetrics()).andReturn(metrics);

      Request req = unit.get(Request.class);
      expect(req.require(MetricRegistry.class)).andReturn(registry);
    };
  }

}

/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.redis;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.metrics.CommandLatencyEvent;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * lettuce event metrics
 *
 * @author aftershadow
 */
public class RedisEventConsumer implements Consumer<Event> {
  private final String name;
  private final MetricRegistry registry;
  private final RedisCommandGauge min;
  private final RedisCommandGauge max;

  public RedisEventConsumer(@Nonnull Object registry, @Nonnull String name) {
    this.registry = (MetricRegistry)registry;
    this.name = "redis."+name+".";
    this.min = new RedisCommandGauge();
    this.max = new RedisCommandGauge();
  }

  @Override
  public void accept(Event event) {
    if (event instanceof CommandLatencyEvent){
      ((CommandLatencyEvent)event).getLatencies().forEach((commandLatencyId, commandMetrics) -> {
        String key = name + "cmd." + commandLatencyId.commandType().name();
        TimeUnit timeUnit = commandMetrics.getTimeUnit();
        registry.counter(key).inc(commandMetrics.getCount());
        max.setValue(timeUnit.toMicros(commandMetrics.getCompletion().getMax()));
        min.setValue(timeUnit.toMicros(commandMetrics.getCompletion().getMin()));
        registry.gauge(key+".max", () -> max);
        registry.gauge(key+".min", () -> min);
      });
    }
  }

  private static class RedisCommandGauge implements Gauge<Long> {
    private Long value = 0l;

    @Override
    public Long getValue() {
      return value;
    }

    public void setValue(long value) {
      this.value = value;
    }
  }
}




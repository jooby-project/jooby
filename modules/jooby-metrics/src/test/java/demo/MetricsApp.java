package demo;

import java.util.concurrent.TimeUnit;

import org.jooby.Jooby;
import org.jooby.json.Jackson;
import org.jooby.metrics.Metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

public class MetricsApp extends Jooby {

  {
    use(new Jackson());
    use(new Metrics()
        .request()
        .reporter(registry -> {
          ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
              .convertDurationsTo(TimeUnit.SECONDS)
              .convertRatesTo(TimeUnit.SECONDS)
              .build();
          reporter.start(1, TimeUnit.MINUTES);
          return reporter;
        })
        .metric("memory", new MemoryUsageGaugeSet())
        .metric("threads", new ThreadStatesGaugeSet())
        .metric("gc", new GarbageCollectorMetricSet())
        .metric("fd", new FileDescriptorRatioGauge())
        .healthCheck("deadlock", new ThreadDeadlockHealthCheck())
        .ping()
        .threadDump());

    get("/", () -> "Hello metrics!");
  }

  public static void main(final String[] args) throws Throwable {
    new MetricsApp().start(args);
  }
}

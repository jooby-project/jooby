package org.jooby.internal.micrometer;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import static org.easymock.EasyMock.expect;
import org.jooby.Route;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TimedSupport.class, Timer.class, LongTaskTimer.class})
public class TimedSupportTest {

  @Test
  public void noTimer() throws Exception {
    new MockUnit(Route.class)
        .expect(attributes(Collections.emptyMap()))
        .run(unit -> {
          TimedSupport timer = TimedSupport.create(unit.get(Route.class));
          assertNull(timer);
        });
  }

  @Test
  public void timer() throws Exception {
    new MockUnit(Route.class, MeterRegistry.class)
        .expect(attributes(
            attributes("timed", "people.all",
                "timed.longTask", false,
                "timed.description", "desc",
                "timed.extraTags", new String[]{"foo", "bar"},
                "timed.histogram", true,
                "timed.percentiles", new double[]{.5})))
        .expect(unit -> {
          Timer.Sample sample = unit.mock(Timer.Sample.class);

          unit.mockStatic(Timer.class);
          expect(Timer.start(unit.get(MeterRegistry.class))).andReturn(sample);

          Timer timer = unit.mock(Timer.class);
          expect(sample.stop(timer)).andReturn(124L);

          Timer.Builder builder = unit.mock(Timer.Builder.class);
          expect(builder.description("desc")).andReturn(builder);
          expect(builder.tags("foo", "bar")).andReturn(builder);
          expect(builder.publishPercentileHistogram(true)).andReturn(builder);
          expect(builder.publishPercentiles(.5)).andReturn(builder);
          expect(builder.register(unit.get(MeterRegistry.class))).andReturn(timer);

          unit.mockStatic(Timer.class);
          expect(Timer.builder("people.all")).andReturn(builder);
        })
        .run(unit -> {
          TimedSupport timer = TimedSupport.create(unit.get(Route.class));
          TimedSupport.Sample sample = timer.start(unit.get(MeterRegistry.class));
          assertEquals(124L, sample.stop());
        });
  }

  @Test
  public void longTimer() throws Exception {
    new MockUnit(Route.class, MeterRegistry.class)
        .expect(attributes(
            attributes("timed", "people.all",
                "timed.longTask", true,
                "timed.description", "desc",
                "timed.extraTags", new String[]{"foo", "bar"})))
        .expect(unit -> {
          LongTaskTimer.Sample sample = unit.mock(LongTaskTimer.Sample.class);

          LongTaskTimer timer = unit.mock(LongTaskTimer.class);
          expect(timer.start()).andReturn(sample);
          expect(sample.stop()).andReturn(124L);

          LongTaskTimer.Builder builder = unit.mock(LongTaskTimer.Builder.class);
          expect(builder.description("desc")).andReturn(builder);
          expect(builder.tags("foo", "bar")).andReturn(builder);
          expect(builder.register(unit.get(MeterRegistry.class))).andReturn(timer);

          unit.mockStatic(LongTaskTimer.class);
          expect(LongTaskTimer.builder("people.all")).andReturn(builder);
        })
        .run(unit -> {
          TimedSupport timer = TimedSupport.create(unit.get(Route.class));
          TimedSupport.Sample sample = timer.start(unit.get(MeterRegistry.class));
          assertEquals(124L, sample.stop());
        });
  }

  private MockUnit.Block attributes(Map<String, Object> attributes) {
    return unit -> {
      Route route = unit.get(Route.class);
      expect(route.attributes()).andReturn(attributes);
    };
  }

  private Map<String, Object> attributes(Object... values) {
    Map<String, Object> result = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      result.put(values[i].toString(), values[i + 1]);
    }
    return result;
  }
}
